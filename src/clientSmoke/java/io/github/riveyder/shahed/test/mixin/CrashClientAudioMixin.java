package io.github.riveyder.shahed.test.mixin;
import io.github.riveyder.shahed.test.CrashEffectsProbe;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.resources.sounds.SoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(SoundEngine.class)
abstract class CrashClientAudioMixin {
    @Inject(method="play",at=@At("HEAD"))
    private void observe(SoundInstance sound,CallbackInfoReturnable<SoundEngine.PlayResult> ci) {
        if(CrashEffectsProbe.captureClient)CrashEffectsProbe.CLIENT_SOUNDS.add(sound.getIdentifier().toString());
    }
}
