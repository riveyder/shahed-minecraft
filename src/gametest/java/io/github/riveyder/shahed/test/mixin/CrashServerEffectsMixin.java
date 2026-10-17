package io.github.riveyder.shahed.test.mixin;

import io.github.riveyder.shahed.registry.ModSounds;
import io.github.riveyder.shahed.test.CrashEffectsProbe;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class CrashServerEffectsMixin {
    @Inject(method = "explode", at = @At("RETURN"))
    private void explosion(CallbackInfo ci) {
        var probe = CrashEffectsProbe.active();
        if (probe != null) probe.explosions++;
    }

    @Inject(method = "sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I", at = @At("RETURN"))
    private void particles(ParticleOptions type, double x, double y, double z, int count,
                           double dx, double dy, double dz, double speed, CallbackInfoReturnable<Integer> cir) {
        var probe = CrashEffectsProbe.active();
        if (probe == null) return;
        if (type == ParticleTypes.EXPLOSION_EMITTER) probe.explosionParticles += count;
        if (type == ParticleTypes.LARGE_SMOKE) probe.smokeParticles += count;
    }

    @Inject(method = "playSeededSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V", at = @At("RETURN"))
    private void sound(Entity except, double x, double y, double z, Holder<SoundEvent> sound,
                       SoundSource category, float volume, float pitch, long seed, CallbackInfo ci) {
        var probe = CrashEffectsProbe.active();
        if (probe != null && sound.value() == ModSounds.BLAST && volume > 0) {
            probe.blastSounds++;
            probe.blastPosition = new Vec3(x, y, z);
        }
    }
}
