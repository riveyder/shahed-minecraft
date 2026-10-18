package io.github.riveyder.shahed.control;

import io.github.riveyder.shahed.entity.DroneEntity;
import io.github.riveyder.shahed.registry.ModSounds;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Optional;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

/** Vanilla gameplay explosion with a bounded, protected block set and independent network effects. */
public final class DroneExplosions {
    public static int explode(ServerLevel level,Entity source,ServerPlayer owner,Vec3 position,DroneConfig.Settings raw) {
        if(source instanceof DroneEntity drone && drone.isTraining()) return 0;
        var policy=raw.sanitized();
        boolean permitted=DroneExplosionProtection.ALLOW.invoker().allow(level,owner,BlockPos.containing(position));
        boolean gameplay=permitted && level.getGameRules().get(GameRules.TNT_EXPLODES);
        var accepted=new HashSet<BlockPos>();var decisions=new HashMap<BlockPos,Boolean>();
        var calculator=new ExplosionDamageCalculator() {
            @Override public boolean shouldBlockExplode(Explosion explosion,BlockGetter world,BlockPos pos,BlockState state,float power) {
                if(!gameplay||!policy.destroyBlocks()||state.isAir()||owner==null||!level.hasChunkAt(pos)) return false;
                return decisions.computeIfAbsent(pos.immutable(),key->{
                    if(accepted.size()>=policy.maxBlocks()||!level.mayInteract(owner,key)
                            ||!DroneExplosionProtection.ALLOW.invoker().allow(level,owner,key)
                            ||!PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level,owner,key,state,level.getBlockEntity(key))) return false;
                    accepted.add(key);return true;
                });
            }
            @Override public boolean shouldDamageEntity(Explosion explosion,Entity target) {
                return gameplay&&policy.entityDamage()&&policy.damageScale()>0
                        &&DroneExplosionProtection.ALLOW.invoker().allow(level,owner,target.blockPosition());
            }
            @Override public float getKnockbackMultiplier(Entity target) {
                return gameplay&&policy.entityDamage()&&DroneExplosionProtection.ALLOW.invoker().allow(level,owner,target.blockPosition())?policy.damageScale():0;
            }
            @Override public float getEntityDamageAmount(Explosion explosion,Entity target,float exposure) {
                return super.getEntityDamageAmount(explosion,target,exposure)*policy.damageScale();
            }
        };
        var interaction=gameplay&&policy.destroyBlocks()&&policy.maxBlocks()>0
                ?level.getGameRules().get(GameRules.TNT_EXPLOSION_DROP_DECAY)?Explosion.BlockInteraction.DESTROY_WITH_DECAY:Explosion.BlockInteraction.DESTROY
                :Explosion.BlockInteraction.KEEP;
        var explosion=new ServerExplosion(level,source,null,calculator,position,policy.explosionPower(),false,interaction);
        int blocks=explosion.explode();
        for(var player:level.players()) {
            if(player.connection==null) continue;
            if(explosion.getHitPlayers().containsKey(player)) player.connection.send(new ClientboundSetEntityMotionPacket(player));
            if(policy.particles() && player.distanceToSqr(position)<4096)
                player.connection.send(new ClientboundExplodePacket(position,policy.explosionPower(),blocks,Optional.empty(),
                        ParticleTypes.EXPLOSION,Holder.direct(SoundEvents.EMPTY),WeightedList.of()));
        }
        if(policy.particles()) {
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,position.x,position.y,position.z,1,0,0,0,0);
            level.sendParticles(ParticleTypes.LARGE_SMOKE,position.x,position.y,position.z,20,.7,.4,.7,.06);
        }
        if(policy.sound()) level.playSound(null,position.x,position.y,position.z,ModSounds.BLAST,SoundSource.NEUTRAL,1.5F,1);
        return blocks;
    }
    private DroneExplosions() { }
}
