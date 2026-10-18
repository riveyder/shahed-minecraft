package io.github.riveyder.shahed.test.mixin;
import io.github.riveyder.shahed.control.DroneExplosions;
import io.github.riveyder.shahed.network.ExplosionEffectPayload;
import io.github.riveyder.shahed.test.CrashEffectsProbe;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(DroneExplosions.class)
abstract class CrashAvEventMixin {
    @Inject(method="emitEffects",at=@At("HEAD"))
    private static void observe(ServerLevel level,ExplosionEffectPayload event,CallbackInfo ci) {
        var probe=CrashEffectsProbe.active();if(probe!=null&&event.valid()&&(event.particles()||event.sound()))probe.effects.add(event);
    }
}
