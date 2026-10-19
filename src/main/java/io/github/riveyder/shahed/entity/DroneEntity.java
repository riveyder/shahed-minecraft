package io.github.riveyder.shahed.entity;

import io.github.riveyder.shahed.control.DroneConfig;

import io.github.riveyder.shahed.registry.ModEntities;
import io.github.riveyder.shahed.control.DroneInput;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
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
    private static final double CRASH_SPEED = 0.35; // normal contact speed, 7 m/s at 20 TPS
    private static final double COLLISION_EPSILON = 0.00001;
    private static final EntityDataAccessor<Float> ROLL = SynchedEntityData.defineId(
            DroneEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> FLYING = SynchedEntityData.defineId(
            DroneEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> THROTTLE = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.FLOAT);
    private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);
    private UUID ownerUuid;
    private DroneInput control = DroneInput.IDLE;
    private int inputAge;
    private int flightTicks;
    private float yawVelocity;
    private float pitchVelocity;
    private float rollVelocity;
    private float throttle;
    private float previousRoll;
    // Client-only visual state. Tick integration + unwrapped interpolation is independent of FPS.
    private float propellerAngle,previousPropellerAngle,propellerSpeed;
    public float getPropellerAngle(float partialTick) {
        return Mth.lerp(Mth.clamp(partialTick,0,1),previousPropellerAngle,propellerAngle);
    }
    private void tickPropeller() {
        previousPropellerAngle=propellerAngle%(float)(Math.PI*2);
        float rpm=Mth.clamp(getThrottle(),0,1);
        float target=rpm>.005F?.3F+rpm*3.1F:0;
        float response=(float)(1-Math.exp(-.05/(target>propellerSpeed?.35:.65)));
        float oldSpeed=propellerSpeed;
        propellerSpeed+=(target-propellerSpeed)*response;
        if(target==0&&propellerSpeed<.001F)propellerSpeed=0;
        propellerAngle=previousPropellerAngle+(oldSpeed+propellerSpeed)*.5F;
    }
    private static final EntityDataAccessor<Boolean> PAYLOAD = SynchedEntityData.defineId(DroneEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RACE_SAFE = SynchedEntityData.defineId(DroneEntity.class,EntityDataSerializers.BOOLEAN);
    public boolean hasPayload(){return entityData.get(PAYLOAD);}
    public boolean isRaceSafe(){return entityData.get(RACE_SAFE);}
    public boolean isNonDestructive(){return isTraining()||isRaceSafe();}
    public boolean canRace(){return !hasPayload()&&(isTraining()||kind()!=AircraftKind.SHAHED);}
    public void makeRaceSafe(){if(!level().isClientSide())entityData.set(RACE_SAFE,true);}
    /** Safety remains latched after a race, even on disconnect, until a landed owner explicitly arms it. */
    public boolean rearm(net.minecraft.server.level.ServerPlayer player){
        if(!landedOwner(player))return false;entityData.set(RACE_SAFE,false);return true;
    }
    private boolean landedOwner(net.minecraft.server.level.ServerPlayer player){
        return !destroyed&&!isRemoved()&&player.level()==level()&&player.isAlive()&&!player.isSpectator()
                &&getOwnerUuid().filter(player.getUUID()::equals).isPresent()&&player.distanceToSqr(this)<36
                &&onGround()&&getDeltaMovement().lengthSqr()<.0064&&!isFlying()
                &&!io.github.riveyder.shahed.control.DroneSessions.isControlling(player);
    }
    public boolean attachPayload(net.minecraft.server.level.ServerPlayer player,net.minecraft.world.item.ItemStack stack){
        if(kind()!=AircraftKind.QUAD||hasPayload()||!landedOwner(player)||isRaceSafe()||!stack.is(io.github.riveyder.shahed.registry.ModItems.PAYLOAD))return false;
        entityData.set(PAYLOAD,true);stack.shrink(1);needsSync=true;return true;
    }
    public boolean releasePayload(net.minecraft.server.level.ServerPlayer player){
        if(kind()!=AircraftKind.QUAD||!hasPayload()||isNonDestructive()||!isFlying()||onGround()||destroyed||isRemoved()
                ||!getOwnerUuid().filter(player.getUUID()::equals).isPresent()||!(level() instanceof ServerLevel server))return false;
        // Clear before spawning; a failed insertion restores attachment, never an inventory item.
        entityData.set(PAYLOAD,false);
        if(!PayloadEntity.release(server,this,player)){entityData.set(PAYLOAD,true);return false;}
        needsSync=true;return true;
    }
    public void setReplayPayload(boolean loaded){if(level().isClientSide())entityData.set(PAYLOAD,loaded&&kind()==AircraftKind.QUAD);}
    private float health = 10.0F;
    private boolean destroyed;
    private io.github.riveyder.shahed.replay.ReplayImpact replayImpact;
    public io.github.riveyder.shahed.replay.ReplayImpact replayImpact() {
        return replayImpact==null?io.github.riveyder.shahed.replay.ReplayImpact.none(position()):replayImpact;
    }
    private boolean spectatorsAllowed;
    public boolean spectatorsAllowed() { return spectatorsAllowed; }
    public void allowSpectators(boolean allowed) { spectatorsAllowed=allowed; }
    private static final EntityDataAccessor<Boolean> TRAINING = SynchedEntityData.defineId(DroneEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> PAINT = SynchedEntityData.defineId(DroneEntity.class,EntityDataSerializers.INT);
    public boolean isTraining() { return entityData.get(TRAINING); }
    public void setTraining(boolean training) { if(!level().isClientSide()) entityData.set(TRAINING,training); }
    public void setPaint(int paint) { if(!level().isClientSide()) entityData.set(PAINT,Math.clamp(paint,0,3)); }
    public int getPaint() { return entityData.get(PAINT); }
    public void setReplayPaint(int paint) { if(level().isClientSide()) entityData.set(PAINT,Math.clamp(paint,0,3)); }
    public int getTint() { return switch(entityData.get(PAINT)) {case 1 -> 0xFFE7C58C; case 2 -> 0xFF9AB58D; case 3 -> 0xFFE89282; default -> -1;}; }
    private double maximumSpeed;
    private double maximumAltitude=-Double.MAX_VALUE;
    public double maximumSpeed() { return maximumSpeed; }
    public double maximumAltitude() { return maximumAltitude; }

    private final io.github.riveyder.shahed.replay.ReplayBuffer replay = new io.github.riveyder.shahed.replay.ReplayBuffer();
    public java.util.List<io.github.riveyder.shahed.replay.ReplayFrame> replayFrames() { return replay.snapshot(); }
    private void recordFrame() { maximumSpeed=Math.max(maximumSpeed,getDeltaMovement().length()*20);maximumAltitude=Math.max(maximumAltitude,getY());replay.add(new io.github.riveyder.shahed.replay.ReplayFrame(getX(),getY(),getZ(),getYRot(),getXRot(),getRoll())); }
    public void setReplayRoll(float roll) { if(level().isClientSide()) entityData.set(ROLL,roll); }

    private int impactCooldown;

    public DroneEntity(EntityType<? extends DroneEntity> type, Level level) {
        super(type, level);
        setNoGravity(false);
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
            maximumSpeed=0;maximumAltitude=getY();
            entityData.set(FLYING, true);
        }
    }

    /** Disarming removes commanded thrust; momentum, aerodynamic forces and gravity remain. */
    public void stopControl() {
        control = DroneInput.IDLE;
        throttle = 0;
        entityData.set(THROTTLE, 0.0F);
        entityData.set(FLYING, false);
        inputAge = 0;
        needsSync = true;
    }

    public boolean isFlying() {
        return entityData.get(FLYING);
    }

    public AircraftKind kind() { return AircraftKind.of(getType()); }
    public double maxSpeed() { return kind().maxSpeed; }

    public float getThrottle() { return entityData.get(THROTTLE); }

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
        if (isRemoved()) return;
        if (impactCooldown > 0) impactCooldown--;
        previousRoll = getRoll();
        super.tick();
        if (level().isClientSide()) {
            tickPropeller();
            interpolation.interpolate();
            return;
        }
        if (isFlying() || flightTicks>0) {
            recordFrame();
        }
        // A missing pilot/input removes thrust commands; it never suspends the simulation.
        if (isFlying()) {
            flightTicks++;
        }
        DroneInput input = isFlying() && ++inputAge <= 10 ? control : DroneInput.IDLE;
        Vec3 velocity=kind()==AircraftKind.SHAHED?shahedVelocity(input):fleetVelocity(input);
        AABB sweptBounds = getBoundingBox().expandTowards(velocity);
        if (!(level() instanceof ServerLevel serverLevel)) return;
        if (!areChunksLoaded(serverLevel, sweptBounds)) {
            // Remote view tickets load ahead. Preserve momentum during a loading delay;
            // querying collisions here would synchronously generate terrain on the tick thread.
            setDeltaMovement(velocity);
            return;
        }
        if (!level().getWorldBorder().isWithinBounds(sweptBounds)) {
            velocity = new Vec3(0, velocity.y, 0);
        }
        int maximumY = level().dimensionType().minY() + level().dimensionType().height();
        if (sweptBounds.maxY > maximumY && velocity.y > 0) {
            velocity = new Vec3(velocity.x, 0, velocity.z);
        }
        Vec3 before = position();
        setDeltaMovement(velocity);
        move(MoverType.SELF, velocity);
        Vec3 actual = position().subtract(before);
        // Movement clipping identifies contact axes, NOT impact speed. The untravelled
        // distance depends on where within this tick the surface was hit and can be tiny
        // even at full speed. Use the pre-contact velocity on those axes instead.
        Vec3 impactVelocity = new Vec3(
                Math.abs(actual.x - velocity.x) > COLLISION_EPSILON ? velocity.x : 0,
                Math.abs(actual.y - velocity.y) > COLLISION_EPSILON ? velocity.y : 0,
                Math.abs(actual.z - velocity.z) > COLLISION_EPSILON ? velocity.z : 0);
        setDeltaMovement(velocity.subtract(impactVelocity));
        double impact = impactVelocity.length();
        // A fatal collision must not depend on accumulated chip damage or its cooldown.
        if (!isNonDestructive() && impact >= (kind()==AircraftKind.QUAD?.75:CRASH_SPEED)) {
            destroy(collisionPosition(impactVelocity));
            return;
        }
        if (impact > 0.18 && impactCooldown == 0) {
            impactCooldown = 8;
            serverLevel.playSound(null,getX(),getY(),getZ(),
                    io.github.riveyder.shahed.registry.ModSounds.IMPACT,net.minecraft.sounds.SoundSource.NEUTRAL,1,1);
            if(!isNonDestructive() && !(kind().hover&&onGround()&&impact<.35)) hurtServer(serverLevel,damageSources().flyIntoWall(),(float)((impact-0.18)*26));
        }
        needsSync = true;
    }

    private Vec3 shahedVelocity(DroneInput input) {
        Vec3 oldVelocity = getDeltaMovement();
        double airspeed = oldVelocity.length();

        // Angular inertia: rates and bank settle over time, rather than snapping the velocity
        // vector to the new heading. Space/Ctrl provide an arcade pitch/climb assist.
        float pitchDemand = input.pitch() * 3.0F;
        if (!isFlying() && !onGround()) {
            // A disconnected aircraft settles nose-down through angular acceleration,
            // retaining its current attitude and angular momentum at disarm time.
            pitchDemand = Mth.clamp((28.0F - getXRot()) * 0.035F, -1.0F, 1.0F);
        }
        if (input.vertical() != 0) {
            pitchDemand += Mth.clamp((-input.vertical() * 22.0F - getXRot()) * 0.10F, -2.5F, 2.5F);
        }
        yawVelocity = Mth.lerp(0.12F, yawVelocity, input.yaw() * 4.5F);
        pitchVelocity = Mth.lerp(0.12F, pitchVelocity, Mth.clamp(pitchDemand, -3, 3));
        float bankTarget = -input.strafe() * 35.0F - input.yaw() * 12.0F;
        if(!DroneConfig.get().stabilization() && input.strafe()==0 && input.yaw()==0 && isFlying()) bankTarget=getRoll();
        rollVelocity = Mth.clamp((rollVelocity + (bankTarget - getRoll()) * 0.025F) * 0.78F, -3, 3);
        float bank = Mth.clamp(getRoll() + rollVelocity, -50, 50);
        entityData.set(ROLL, bank);
        float bankTurn = (float) (-bank * 0.015 * Math.min(1, airspeed / MAX_SPEED));
        setYRot(Mth.wrapDegrees(getYRot() + yawVelocity + bankTurn));
        setXRot(Mth.clamp(getXRot() + pitchVelocity, -65.0F, 65.0F));

        double yaw = Math.toRadians(getYRot());
        double pitch = Math.toRadians(getXRot());
        double roll = Math.toRadians(bank);
        Vec3 forward = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch),
                Math.cos(yaw) * Math.cos(pitch));
        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vec3 wingUp = right.cross(forward).scale(Math.cos(roll)).subtract(right.scale(Math.sin(roll)));

        // Engine spool is independent of linear inertia. Wings provide lift only with airflow;
        // idle/slow aircraft cannot hover, even while a control session remains connected.
        float requestedThrottle = Math.max(Math.max(0, input.forward()), Math.max(0, input.vertical()) * 0.85F);
        if (input.brake() || input.forward() < 0) requestedThrottle = 0;
        throttle = Mth.lerp(requestedThrottle > throttle ? 0.09F : 0.045F, throttle, requestedThrottle);
        entityData.set(THROTTLE, Math.round(throttle * 100) / 100.0F);
        double forwardAirspeed = Math.max(0, oldVelocity.dot(forward));
        double lift = Math.min(0.040, forwardAirspeed * forwardAirspeed * 0.041);
        double airBrake = input.brake() ? 0.11 : Math.max(0, -input.forward()) * 0.065;
        double drag = (0.008 + 0.024 * airspeed)*DroneConfig.get().dragMultiplier() + airBrake + (onGround() ? 0.025 : 0);
        Vec3 velocity = oldVelocity.scale(Math.exp(-drag))
                .add(forward.scale(throttle * 0.045))
                .add(wingUp.scale(lift))
                .add(0, -0.025, 0);
        // A fin gradually damps sideslip; banked lift, not a lateral teleport, turns the aircraft.
        velocity = velocity.subtract(right.scale(oldVelocity.dot(right) * 0.025));
        if (velocity.lengthSqr() > MAX_SPEED * MAX_SPEED) {
            velocity = velocity.normalize().scale(MAX_SPEED);
        }
        return velocity;
    }

    private Vec3 fleetVelocity(DroneInput input){
        Vec3 old=getDeltaMovement();double speed=old.length();boolean powered=isFlying();
        if(kind().hover){
            yawVelocity=Mth.lerp(.28F,yawVelocity,input.yaw()*7);
            pitchVelocity=Mth.lerp(.22F,pitchVelocity,input.pitch()*4);
            setYRot(Mth.wrapDegrees(getYRot()+yawVelocity));
            setXRot(Mth.clamp(getXRot()+pitchVelocity,-55,55));
            entityData.set(ROLL,Mth.lerp(.22F,getRoll(),-input.strafe()*28-input.yaw()*8));
            double yaw=Math.toRadians(getYRot());
            Vec3 forward=new Vec3(-Math.sin(yaw),0,Math.cos(yaw)),right=new Vec3(-Math.cos(yaw),0,-Math.sin(yaw));
            double gain=kind()==AircraftKind.CONTACT?.15:.12;
            Vec3 motion=old.scale(Math.exp(-(input.brake()?.36:powered?.075:.025)*DroneConfig.get().dragMultiplier()));
            if(powered){
                motion=motion.add(forward.scale(input.forward()*gain)).add(right.scale(input.strafe()*gain))
                        .add(0,input.vertical()*.10-old.y*.14,0);
            }else motion=motion.add(0,-.035,0);
            float desired=powered?Math.min(1,.42F+.35F*Math.max(Math.abs(input.forward()),Math.abs(input.strafe()))+.23F*Math.abs(input.vertical())):0;
            throttle=Mth.lerp(.2F,throttle,desired);entityData.set(THROTTLE,Math.round(throttle*100)/100F);
            return motion.lengthSqr()>maxSpeed()*maxSpeed()?motion.normalize().scale(maxSpeed()):motion;
        }
        // Jet: speed-dependent authority, banked turns, thrust and inertial sideslip; never hover.
        float authority=(float)Math.clamp(speed/1.2,.12,1);
        yawVelocity=Mth.lerp(.1F,yawVelocity,input.yaw()*2.8F*authority);
        float demand=input.pitch()*2.4F-input.vertical()*1.8F;
        if(!powered&&!onGround())demand=Mth.clamp((32-getXRot())*.025F,-1,1);
        pitchVelocity=Mth.lerp(.09F,pitchVelocity,demand*authority);
        entityData.set(ROLL,Mth.lerp(.08F,getRoll(),-input.strafe()*60-input.yaw()*18));
        setYRot(Mth.wrapDegrees(getYRot()+yawVelocity-getRoll()*.022F*authority));
        setXRot(Mth.clamp(getXRot()+pitchVelocity,-70,70));
        double yaw=Math.toRadians(getYRot()),pitch=Math.toRadians(getXRot());
        Vec3 forward=new Vec3(-Math.sin(yaw)*Math.cos(pitch),-Math.sin(pitch),Math.cos(yaw)*Math.cos(pitch));
        float desired=powered&&!input.brake()?Math.max(0,input.forward()):0;
        throttle=Mth.lerp(desired>throttle?.14F:.06F,throttle,desired);entityData.set(THROTTLE,Math.round(throttle*100)/100F);
        Vec3 motion=old.scale(Math.exp(-(.012+(input.brake()?.12:0)+Math.max(0,-input.forward())*.07)))
                .add(forward.scale(throttle*.15)).add(0,-.032+Math.min(.035,Math.max(0,old.dot(forward))*.024),0);
        return motion.lengthSqr()>maxSpeed()*maxSpeed()?motion.normalize().scale(maxSpeed()):motion;
    }

    /** Vanilla sweeps these entity boxes together with block voxel shapes in move(). */
    @Override
    public boolean canCollideWith(Entity other) {
        return other.isAlive() && !other.isSpectator() && !other.noPhysics
                && !isPassengerOfSameVehicle(other)
                && (other instanceof LivingEntity || other instanceof DroneEntity || super.canCollideWith(other));
    }

    /** Place effects on the contacted surface, not several blocks behind the wide nose/wing. */
    private Vec3 collisionPosition(Vec3 impactVelocity) {
        AABB bounds = getBoundingBox();
        Vec3 center = bounds.getCenter();
        Direction.Axis axis = Math.abs(impactVelocity.x) > Math.abs(impactVelocity.y)
                ? Direction.Axis.X : Direction.Axis.Y;
        if (Math.abs(impactVelocity.z) > Math.abs(impactVelocity.get(axis))) axis = Direction.Axis.Z;
        double sign = Math.signum(impactVelocity.get(axis));
        double face = sign > 0 ? bounds.max(axis) : bounds.min(axis);
        Vec3 contact = center;
        double nearest = Double.MAX_VALUE;
        for (var shape : Entity.collectAllColliders(this, level(), bounds.inflate(COLLISION_EPSILON))) {
            for (AABB obstacle : shape.toAabbs()) {
                double surface = sign > 0 ? obstacle.min(axis) : obstacle.max(axis);
                if (Math.abs(surface - face) > COLLISION_EPSILON * 2) continue;
                Vec3 point = new Vec3(Mth.clamp(center.x, obstacle.minX, obstacle.maxX),
                        Mth.clamp(center.y, obstacle.minY, obstacle.maxY),
                        Mth.clamp(center.z, obstacle.minZ, obstacle.maxZ));
                double distance = point.distanceToSqr(center);
                if (distance < nearest) { nearest = distance; contact = point; }
            }
        }
        // Keep the particle/sound origin just outside the solid surface.
        double coordinate = face - sign * 0.01;
        return switch (axis) {
            case X -> new Vec3(coordinate, contact.y, contact.z);
            case Y -> new Vec3(contact.x, coordinate, contact.z);
            case Z -> new Vec3(contact.x, contact.y, coordinate);
        };
    }

    public static boolean areChunksLoaded(ServerLevel level, AABB bounds) {
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
    public static boolean spawn(ServerLevel level, Player player, Vec3 position) { return spawn(level,player,position,false); }

    public static boolean spawn(ServerLevel level, Player player, Vec3 position,boolean training) {
        return spawn(level,player,position,training,AircraftKind.SHAHED);
    }
    public static boolean spawn(ServerLevel level, Player player, Vec3 position,boolean training,AircraftKind kind) {
        if(player.level()!=level||io.github.riveyder.shahed.control.DroneDeployment.denial(player)!=null) return false;

        DroneEntity drone = new DroneEntity(kind.type(), level);
        drone.setPos(position.x, position.y, position.z);
        drone.setYRot(player.getYRot());
        drone.setXRot(0.0F);
        drone.setOwnerUuid(player.getUUID());
        drone.setTraining(training);
        if(training) drone.setPaint(2);

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
        builder.define(PAYLOAD,false);
        builder.define(RACE_SAFE,false);
        builder.define(ROLL, 0.0F);
        builder.define(TRAINING,false);
        builder.define(PAINT,0);
        builder.define(THROTTLE, 0.0F);
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
        entityData.set(PAYLOAD,kind()==AircraftKind.QUAD&&input.getBooleanOr("Payload",false));
        entityData.set(RACE_SAFE,input.getBooleanOr("RaceSafe",false));
        setTraining(input.getBooleanOr("Training",false));
        setPaint(input.getIntOr("Paint",0));
        float savedHealth = input.getFloatOr("Health", 10.0F);
        health = Float.isFinite(savedHealth) ? Mth.clamp(savedHealth, 0.1F, 10.0F) : 10.0F;
        // Never restore a live session or queued commands. Vanilla Motion retains momentum.
        stopControl();
        throttle = 0;
        yawVelocity = finiteSaved(input, "YawVelocity", 4.5F);
        pitchVelocity = finiteSaved(input, "PitchVelocity", 3.0F);
        rollVelocity = finiteSaved(input, "RollVelocity", 3.0F);
        entityData.set(ROLL, finiteSaved(input, "Bank", 50.0F));
        setNoGravity(false);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (ownerUuid != null) {
            output.putString("Owner", ownerUuid.toString());
        }
        output.putBoolean("Payload",hasPayload());
        output.putBoolean("RaceSafe",isRaceSafe());
        output.putBoolean("Training",isTraining());
        output.putInt("Paint",entityData.get(PAINT));
        output.putFloat("Health", health);
        output.putFloat("YawVelocity", yawVelocity);
        output.putFloat("PitchVelocity", pitchVelocity);
        output.putFloat("RollVelocity", rollVelocity);
        output.putFloat("Bank", getRoll());
    }

    private static float finiteSaved(ValueInput input, String key, float limit) {
        float value = input.getFloatOr(key, 0);
        return Float.isFinite(value) ? Mth.clamp(value, -limit, limit) : 0;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (destroyed || isRemoved() || isInvulnerableToBase(source) || !Float.isFinite(amount) || amount <= 0) {
            return false;
        }
        health -= amount;
        if (health <= 0 || (source.getEntity() instanceof Player player && player.getAbilities().instabuild)) {
            destroy();
        }
        return true;
    }

    public boolean isDestroyed() { return destroyed; }

    /** Mark before effects: chained explosion damage cannot re-enter this transaction. */
    public boolean destroy() {
        return destroy(position().add(0, 0.4, 0));
    }

    private boolean destroy(Vec3 explosionPosition) {
        if (!(level() instanceof ServerLevel server) || destroyed || isRemoved()) return false;
        destroyed=true;
        var policy=io.github.riveyder.shahed.control.DroneConfig.get();
        // Keep the existing replay visual protocol (0..12); gameplay uses the full policy below.
        replayImpact=new io.github.riveyder.shahed.replay.ReplayImpact(explosionPosition,!isNonDestructive()&&kind()!=AircraftKind.QUAD,policy.particles(),policy.sound(),Math.min(12,policy.explosionPower()));
        recordFrame();
        io.github.riveyder.shahed.control.DroneSessions.destroyed(this);
        stopControl();
        if(isNonDestructive()||kind()==AircraftKind.QUAD) { discard();return true; }
        var owner=ownerUuid==null?null:server.getServer().getPlayerList().getPlayer(ownerUuid);
        io.github.riveyder.shahed.control.DroneExplosions.explode(server,this,owner,explosionPosition,policy);
        discard();
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }
}
