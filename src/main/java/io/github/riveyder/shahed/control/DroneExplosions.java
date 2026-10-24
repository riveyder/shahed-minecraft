package io.github.riveyder.shahed.control;

import io.github.riveyder.shahed.entity.DroneEntity;
import io.github.riveyder.shahed.network.ExplosionEffectPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

/** Vanilla gameplay explosion with a bounded, protected block set and independent network effects. */
public final class DroneExplosions {
    // Aggregate direct destruction budget across simultaneous crashes in each world/tick.
    private static final java.util.Map<ServerLevel,long[]> BUDGETS=new java.util.WeakHashMap<>();

    public static int explode(ServerLevel level,Entity source,ServerPlayer owner,Vec3 position,DroneConfig.Settings raw) {
        if(source instanceof DroneEntity drone && drone.isNonDestructive()) return 0;
        var policy=raw.sanitized();
        boolean permitted=DroneExplosionProtection.ALLOW.invoker().allow(level,owner,BlockPos.containing(position));
        boolean gameplay=permitted && level.getGameRules().get(GameRules.TNT_EXPLODES);
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
            private boolean allowTarget(Entity target) {
                return gameplay&&policy.entityDamage()&&policy.damageScale()>0
                        && (!(target instanceof ServerPlayer) || target==owner || level.getGameRules().get(GameRules.PVP))
                        &&DroneExplosionProtection.ALLOW.invoker().allow(level,owner,target.blockPosition());
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
            @Override public boolean shouldDamageEntity(Explosion explosion,Entity target) {
                return allowTarget(target);
            }
            @Override public float getKnockbackMultiplier(Entity target) {
                return allowTarget(target)?policy.damageScale():0;
            }
            @Override public float getEntityDamageAmount(Explosion explosion,Entity target,float exposure) {
                return super.getEntityDamageAmount(explosion,target,exposure)*policy.damageScale();
            }
        };
        var interaction=gameplay&&policy.destroyBlocks()&&policy.maxBlocks()>0
                ?level.getGameRules().get(GameRules.TNT_EXPLOSION_DROP_DECAY)?Explosion.BlockInteraction.DESTROY_WITH_DECAY:Explosion.BlockInteraction.DESTROY
                :Explosion.BlockInteraction.KEEP;
        var explosion=new ServerExplosion(level,source,level.damageSources().explosion(source,owner),calculator,position,policy.explosionPower(),false,interaction);
        int blocks=explosion.explode();
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
