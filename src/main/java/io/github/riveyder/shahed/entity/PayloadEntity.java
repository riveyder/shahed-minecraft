package io.github.riveyder.shahed.entity;

import io.github.riveyder.shahed.control.*;
import io.github.riveyder.shahed.registry.ModEntities;
import java.util.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;

/** Single-use arcade drop: server motion/collision, bounded lifetime, no recoverable item. */
public final class PayloadEntity extends Entity {
    private static final Map<ServerLevel,Set<UUID>> ACTIVE=new WeakHashMap<>();
    private final InterpolationHandler interpolation=new InterpolationHandler(this,2);
    private UUID owner;
    private UUID aircraft;
    private int age;
    private boolean detonated,registered;
    public PayloadEntity(EntityType<? extends PayloadEntity> type,Level level){super(type,level);}
    private boolean register(ServerLevel level){
        var ids=ACTIVE.computeIfAbsent(level,k->new HashSet<>());
        if(ids.size()>=Math.min(128,DroneConfig.get().activeLimit()*2))return false;
        ids.add(getUUID());registered=true;return true;
    }
    public static boolean release(ServerLevel level,DroneEntity drone,ServerPlayer player){
        var p=new PayloadEntity(ModEntities.PAYLOAD,level);p.owner=player.getUUID();p.aircraft=drone.getUUID();
        p.setPos(drone.getX(),drone.getY()-.35,drone.getZ());p.setDeltaMovement(drone.getDeltaMovement().scale(.7).add(0,-.08,0));
        if(!DroneEntity.areChunksLoaded(level,p.getBoundingBox())||!level.noCollision(p,p.getBoundingBox())||!p.register(level))return false;
        if(!level.addFreshEntity(p)){p.discard();return false;}return true;
    }
    @Override public void tick(){
        super.tick();if(level().isClientSide()){interpolation.interpolate();return;}
        if(!(level() instanceof ServerLevel level)||detonated)return;
        if(!registered&&!register(level)){discard();return;}
        if(++age>300||getY()<level.dimensionType().minY()-32){discard();return;}
        Vec3 velocity=getDeltaMovement().scale(.995).add(0,-.04,0);
        if(velocity.lengthSqr()>16)velocity=velocity.normalize().scale(4);
        var bounds=getBoundingBox().expandTowards(velocity);
        if(!DroneEntity.areChunksLoaded(level,bounds))return;
        if(!level.getWorldBorder().isWithinBounds(bounds)){discard();return;}
        Vec3 before=position();setDeltaMovement(velocity);move(MoverType.SELF,velocity);
        if(position().subtract(before).subtract(velocity).lengthSqr()>1e-10){
            detonated=true; // Before callbacks: never repeat effects or damage.
            var pilot=owner==null?null:level.getServer().getPlayerList().getPlayer(owner);
            DroneExplosions.explode(level,this,pilot,position().add(0,.12,0),DroneConfig.get());discard();
        }
        needsSync=true;
    }
    @Override public boolean canCollideWith(Entity other){
        return other.isAlive()&&!other.isSpectator()&&!other.noPhysics
                &&!other.getUUID().equals(aircraft)&&(other instanceof LivingEntity||other instanceof DroneEntity||super.canCollideWith(other));
    }
    @Override public void remove(RemovalReason reason){
        if(level() instanceof ServerLevel level&&registered){var ids=ACTIVE.get(level);if(ids!=null){ids.remove(getUUID());if(ids.isEmpty())ACTIVE.remove(level);}registered=false;}
        super.remove(reason);
    }
    @Override public boolean hurtServer(ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float amount){return false;}
    @Override public InterpolationHandler getInterpolation(){return interpolation;}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){}
    @Override protected void readAdditionalSaveData(ValueInput in){
        age=Math.clamp(in.getIntOr("Age",0),0,300);owner=uuid(in,"Owner");aircraft=uuid(in,"Aircraft");
    }
    private static UUID uuid(ValueInput in,String key){try{return UUID.fromString(in.getString(key).orElse(""));}catch(IllegalArgumentException e){return null;}}
    @Override protected void addAdditionalSaveData(ValueOutput out){
        out.putInt("Age",age);if(owner!=null)out.putString("Owner",owner.toString());if(aircraft!=null)out.putString("Aircraft",aircraft.toString());
    }
}
