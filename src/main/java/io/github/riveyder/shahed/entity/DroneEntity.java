package io.github.riveyder.shahed.entity;

import io.github.riveyder.shahed.registry.ModEntities;
import io.github.riveyder.shahed.control.DroneInput;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative arcade aircraft. Its regular entity UUID is its persistent identity. */
public final class DroneEntity extends Entity {
    public static final double MAX_SPEED = 0.8; // blocks/tick = 16 metres/second at 20 TPS
    private static final EntityDataAccessor<Float> ROLL = SynchedEntityData.defineId(
            DroneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> FLYING = SynchedEntityData.defineId(
            DroneEntity.class, EntityDataSerializers.BOOLEAN);
    private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);
    private UUID ownerUuid;
    private DroneInput control = DroneInput.IDLE;
    private int inputAge;
    private int flightTicks;
    private float yawVelocity;
    private float pitchVelocity;
    private float previousRoll;
    private float health = 10.0F;

    public DroneEntity(EntityType<? extends DroneEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public Optional<UUID> getOwnerUuid() {
        return Optional.ofNullable(ownerUuid);
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = Objects.requireNonNull(ownerUuid, "ownerUuid");
    }

    /** Called only by the validated server session (or server integration tests). */
    public void setControlInput(DroneInput input) {
        if (level().isClientSide() || isRemoved()) {
            return;
        }
        control = Objects.requireNonNull(input, "input").sanitized();
        inputAge = 0;
        if (!isFlying()) {
            flightTicks = 0;
            entityData.set(FLYING, true);
        }
    }

    /** Fail safe: disarming leaves the aircraft hovering, never continuing stale throttle. */
    public void stopControl() {
        control = DroneInput.IDLE;
        entityData.set(FLYING, false);
        setDeltaMovement(Vec3.ZERO);
        yawVelocity = 0;
        pitchVelocity = 0;
        needsSync = true;
    }

    public boolean isFlying() {
        return entityData.get(FLYING);
    }

    public int getFlightTicks() {
        return flightTicks;
    }

    public float getRoll() {
        return entityData.get(ROLL);
    }

    public float getRoll(float partialTick) {
        return Mth.lerp(partialTick, previousRoll, getRoll());
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolation;
    }

    @Override
    public void tick() {
        previousRoll = getRoll();
        super.tick();
        if (level().isClientSide()) {
            interpolation.interpolate();
            return;
        }
        if (!isFlying()) {
            setDeltaMovement(Vec3.ZERO);
            entityData.set(ROLL, Mth.lerp(0.2F, getRoll(), 0.0F));
            return;
        }
        // A second, entity-level watchdog also protects against an abandoned session manager.
        if (++inputAge > 40) {
            stopControl();
            return;
        }
        flightTicks++;
        DroneInput input = inputAge > 10 ? new DroneInput(0, 0, 0, 0, 0, true) : control;
        yawVelocity = Mth.lerp(0.3F, yawVelocity, input.yaw() * 4.5F);
        pitchVelocity = Mth.lerp(0.3F, pitchVelocity, input.pitch() * 3.0F);
        setYRot(Mth.wrapDegrees(getYRot() + yawVelocity));
        setXRot(Mth.clamp(getXRot() + pitchVelocity, -65.0F, 65.0F));
        float bank = -input.strafe() * 22.0F - input.yaw() * 12.0F;
        entityData.set(ROLL, Mth.lerp(0.18F, getRoll(), bank));

        double yaw = Math.toRadians(getYRot());
        double pitch = Math.toRadians(getXRot());
        Vec3 forward = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch),
                Math.cos(yaw) * Math.cos(pitch));
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 acceleration = forward.scale(input.forward()).add(right.scale(input.strafe()))
                .add(0, input.vertical(), 0);
        if (acceleration.lengthSqr() > 1) {
            acceleration = acceleration.normalize();
        }
        Vec3 velocity = input.brake() ? getDeltaMovement().scale(0.62)
                : getDeltaMovement().add(acceleration.scale(0.055)).scale(0.94);
        if (velocity.lengthSqr() > MAX_SPEED * MAX_SPEED) {
            velocity = velocity.normalize().scale(MAX_SPEED);
        }
        if (velocity.lengthSqr() < 0.000001) {
            velocity = Vec3.ZERO;
        }
        AABB sweptBounds = getBoundingBox().expandTowards(velocity);
        int minimumY = level().dimensionType().minY();
        if (!(level() instanceof ServerLevel serverLevel)
                || sweptBounds.minY < minimumY
                || sweptBounds.maxY > minimumY + level().dimensionType().height()
                || !level().getWorldBorder().isWithinBounds(sweptBounds)
                || !areChunksLoaded(serverLevel, sweptBounds)) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        Vec3 before = position();
        setDeltaMovement(velocity);
        move(MoverType.SELF, velocity);
        Vec3 actual = position().subtract(before);
        setDeltaMovement(new Vec3(
                Math.abs(actual.x - velocity.x) > 0.00001 ? 0 : velocity.x,
                Math.abs(actual.y - velocity.y) > 0.00001 ? 0 : velocity.y,
                Math.abs(actual.z - velocity.z) > 0.00001 ? 0 : velocity.z));
        needsSync = true;
    }

    private static boolean areChunksLoaded(ServerLevel level, AABB bounds) {
        for (int x = Mth.floor(bounds.minX) >> 4; x <= Mth.floor(Math.nextDown(bounds.maxX)) >> 4; x++) {
            for (int z = Mth.floor(bounds.minZ) >> 4; z <= Mth.floor(Math.nextDown(bounds.maxZ)) >> 4; z++) {
                if (!level.getChunkSource().hasChunk(x, z)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** All callers run on the server thread: validate the complete hitbox before adding the entity. */
    public static boolean spawn(ServerLevel level, Player player, Vec3 position) {
        if (!player.getAbilities().instabuild) {
            return false;
        }

        DroneEntity drone = new DroneEntity(ModEntities.DRONE, level);
        drone.setPos(position.x, position.y, position.z);
        drone.setYRot(player.getYRot());
        drone.setXRot(0.0F);
        drone.setOwnerUuid(player.getUUID());

        AABB bounds = drone.getBoundingBox();
        int minimumY = level.dimensionType().minY();
        if (bounds.minY < minimumY || bounds.maxY > minimumY + level.dimensionType().height()
                || !level.getWorldBorder().isWithinBounds(bounds)
                || !level.mayInteract(player, BlockPos.containing(position))) {
            return false;
        }

        // Check every touched chunk before collision queries, which must not generate new terrain.
        int minimumChunkX = Mth.floor(bounds.minX) >> 4;
        int maximumChunkX = Mth.floor(Math.nextDown(bounds.maxX)) >> 4;
        int minimumChunkZ = Mth.floor(bounds.minZ) >> 4;
        int maximumChunkZ = Mth.floor(Math.nextDown(bounds.maxZ)) >> 4;
        for (int chunkX = minimumChunkX; chunkX <= maximumChunkX; chunkX++) {
            for (int chunkZ = minimumChunkZ; chunkZ <= maximumChunkZ; chunkZ++) {
                if (!level.getChunkSource().hasChunk(chunkX, chunkZ)) {
                    return false;
                }
            }
        }

        if (!level.noCollision(drone, bounds)
                || !level.getEntities(drone, bounds, entity -> !entity.isSpectator()).isEmpty()) {
            return false;
        }

        return level.addFreshEntity(drone);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ROLL, 0.0F);
        builder.define(FLYING, false);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        ownerUuid = input.getString("Owner").flatMap(value -> {
            try {
                return Optional.of(UUID.fromString(value));
            } catch (IllegalArgumentException exception) {
                return Optional.empty();
            }
        }).orElse(null);
        float savedHealth = input.getFloatOr("Health", 10.0F);
        health = Float.isFinite(savedHealth) ? Mth.clamp(savedHealth, 0.1F, 10.0F) : 10.0F;
        // Never restore a pilot, throttle or momentum after a world/server reload.
        stopControl();
        entityData.set(ROLL, 0.0F);
        setNoGravity(true);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (ownerUuid != null) {
            output.putString("Owner", ownerUuid.toString());
        }
        output.putFloat("Health", health);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isRemoved() || isInvulnerableToBase(source) || !Float.isFinite(amount) || amount <= 0) {
            return false;
        }
        health -= amount;
        if (health <= 0 || (source.getEntity() instanceof Player player && player.getAbilities().instabuild)) {
            stopControl();
            discard();
        }
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }
}
