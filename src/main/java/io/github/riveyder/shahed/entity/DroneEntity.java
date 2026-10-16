package io.github.riveyder.shahed.entity;

import io.github.riveyder.shahed.registry.ModEntities;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A persistent static prototype. Flight, damage and controller linking belong to later stages.
 * Its regular entity UUID is the stable drone identity; Owner is metadata for future ownership rules.
 */
public final class DroneEntity extends Entity {
    private UUID ownerUuid;

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
        // Stage 1 has no custom live state. Position and rotation use vanilla entity tracking.
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
        setNoGravity(true);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (ownerUuid != null) {
            output.putString("Owner", ownerUuid.toString());
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }
}
