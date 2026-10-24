package io.github.riveyder.shahed.control;

import io.github.riveyder.shahed.entity.DroneEntity;
import io.github.riveyder.shahed.network.ExplosionEffectPayload;
import io.github.riveyder.shahed.compat.ServerPlayNetworking;
import java.util.UUID;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Optional;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import io.github.riveyder.shahed.compat.PortExplosion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;

/** Vanilla gameplay explosion with a bounded, protected block set and independent network effects. */
public final class DroneExplosions {
    // Aggregate direct destruction budget across simultaneous crashes in each world/tick.
    private static final java.util.Map<ServerLevel,long[]> BUDGETS=new java.util.WeakHashMap<>();

    public static int explode(ServerLevel level,Entity source,ServerPlayer owner,Vec3 position,DroneConfig.Settings raw) {
        if(source instanceof DroneEntity drone && drone.isNonDestructive()) return 0;
        var policy=raw.sanitized();
        boolean permitted=DroneExplosionProtection.ALLOW.invoker().allow(level,owner,BlockPos.containing(position));
        boolean gameplay=permitted;
        var budget=BUDGETS.computeIfAbsent(level,k->new long[]{-1,0});
        if(budget[0]!=level.getGameTime()) {budget[0]=level.getGameTime();budget[1]=0;}
        var accepted=new HashSet<BlockPos>();var decisions=new HashMap<BlockPos,Boolean>();
        var calculator=new ExplosionDamageCalculator() {
            @Override public Optional<Float> getBlockExplosionResistance(Explosion e,BlockGetter world,BlockPos pos,
                    BlockState state,net.minecraft.world.level.material.FluidState fluid) {
                var vanilla=super.getBlockExplosionResistance(e,world,pos,state,fluid);
                // Fictional gameplay balance, not a physical yield: ordinary terrain absorbs less ray energy.
                // Preserve fluids, unbreakable/high-resistance blocks and all subsequent protection vetoes.
                if(gameplay&&policy.destroyBlocks()&&fluid.isEmpty()&&state.getDestroySpeed(world,pos)>=0)
                    return vanilla.map(r->r<1200?r*.20F:r);
                return vanilla;
            }
            @Override public boolean shouldBlockExplode(Explosion explosion,BlockGetter world,BlockPos pos,BlockState state,float power) {
                if(!gameplay||!policy.destroyBlocks()||state.isAir()||owner==null||!level.hasChunkAt(pos)) return false;
                return decisions.computeIfAbsent(pos.immutable(),key->{
                    if(budget[1]>=32768||accepted.size()>=policy.maxBlocks()||!level.mayInteract(owner,key)
                            ||!DroneExplosionProtection.ALLOW.invoker().allow(level,owner,key)
                            ||!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level,owner,key,state,level.getBlockEntity(key))) return false;
                    accepted.add(key);budget[1]++;return true;
                });
            }
        };
        var interaction=gameplay&&policy.destroyBlocks()&&policy.maxBlocks()>0
                ?level.getGameRules().getBoolean(GameRules.RULE_TNT_EXPLOSION_DROP_DECAY)?Explosion.BlockInteraction.DESTROY_WITH_DECAY:Explosion.BlockInteraction.DESTROY
                :Explosion.BlockInteraction.KEEP;
        var explosion=new PortExplosion(level,source,level.damageSources().explosion(source,owner),calculator,position.x,position.y,position.z,policy.explosionPower(),interaction);
        explosion.explode();
        int blocks=explosion.getToBlow().size();
        explosion.finalizeExplosion(false);
        // Match vanilla distance/exposure damage, with the same server protection and damage-scale policy.
        if(gameplay&&policy.entityDamage()&&policy.damageScale()>0) {
            double radius=policy.explosionPower()*2;
            for(var target:level.getEntities(source,new net.minecraft.world.phys.AABB(position,position).inflate(radius))) {
                if(target.ignoreExplosion() || target instanceof ServerPlayer && target!=owner && !level.getServer().isPvpAllowed()
                        || !DroneExplosionProtection.ALLOW.invoker().allow(level,owner,target.blockPosition()))continue;
                double distance=target.position().distanceTo(position)/radius;
                if(distance>1)continue;
                Vec3 direction=target.getEyePosition().subtract(position);
                if(direction.lengthSqr()<1.0E-8)continue;
                double force=(1-distance)*Explosion.getSeenPercent(position,target);
                float damage=(float)((force*force+force)*.5*7*radius+1)*policy.damageScale();
                target.hurt(level.damageSources().explosion(source,owner),damage);
                Vec3 impulse=direction.normalize().scale(force*policy.damageScale());
                target.setDeltaMovement(target.getDeltaMovement().add(impulse));
                target.hasImpulse=true;
                if(target instanceof ServerPlayer player)explosion.getHitPlayers().put(player,impulse);
            }
        }
        if(gameplay&&policy.destroyBlocks()&&policy.realFire()&&owner!=null) {
            int fires=0;
            // Never ignite outside the protected set already accepted by the explosion.
            for(var pos:accepted) {
                if(fires>=48)break;
                if(level.getRandom().nextInt(6)!=0||!level.getBlockState(pos).isAir()
                        ||!level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),net.minecraft.core.Direction.UP))continue;
                if(!level.mayInteract(owner,pos)||!DroneExplosionProtection.ALLOW.invoker().allow(level,owner,pos)
                        ||!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level,owner,pos,level.getBlockState(pos),null))continue;
                // Spreading is explicit opt-in; vanilla fire itself obeys the server fire-spread gamerule.
                var fire=policy.fireSpread()?net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState():CrashFireBlock.INSTANCE.defaultBlockState();
                if(level.setBlock(pos,fire,3))fires++;
            }
        }
        for(var player:level.players()) {
            if(player.connection==null) continue;
            if(explosion.getHitPlayers().containsKey(player)) player.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
        emitEffects(level,new ExplosionEffectPayload(UUID.randomUUID(),position,level.getRandom().nextLong(),policy.particles(),policy.sound(),policy.effectScale(),policy.audioRange()));
        return blocks;
    }
    public static void emitEffects(ServerLevel level,ExplosionEffectPayload event) {
        if(!event.valid() || !event.particles()&&!event.sound()) return;
        // Dimension-local broadcast also reaches remote FPV cameras. Clients cull by camera distance.
        // One small reliable packet replaces dozens of particle/sound packets; no server debris entities.
        for(var player:level.players()) if(player.connection!=null && ServerPlayNetworking.canSend(player,ExplosionEffectPayload.TYPE))
            ServerPlayNetworking.send(player,event);
    }
    private DroneExplosions() { }
}
