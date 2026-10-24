package io.github.riveyder.shahed.test.mixin;
import io.github.riveyder.shahed.client.ExplosionEffects;
import io.github.riveyder.shahed.network.ExplosionEffectPayload;
import io.github.riveyder.shahed.test.CrashEffectsProbe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ExplosionEffects.class)
abstract class CrashClientAvMixin {
    @Inject(method="receive",at=@At("HEAD"))
    private static void observe(ExplosionEffectPayload event,CallbackInfo ci) {
        if(CrashEffectsProbe.captureClient)CrashEffectsProbe.CLIENT_EFFECTS.add(event);
    }
}
