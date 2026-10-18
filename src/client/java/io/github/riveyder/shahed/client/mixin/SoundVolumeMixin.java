package io.github.riveyder.shahed.client.mixin;

import io.github.riveyder.shahed.ShahedMod;
import io.github.riveyder.shahed.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.resources.sounds.SoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
abstract class SoundVolumeMixin {
    @Inject(method="calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F",at=@At("RETURN"),cancellable=true)
    private void shahed$volume(SoundInstance sound,CallbackInfoReturnable<Float> callback) {
        double gain=1;
        if(sound.getIdentifier().equals(ShahedMod.id("blast"))||sound.getIdentifier().equals(ShahedMod.id("blast_echo"))) gain=FpvController.settings().explosionVolume;
        else if(sound.getIdentifier().toString().equals("minecraft:ui.button.click") &&
                (Minecraft.getInstance().gui.screen() instanceof FpvSettingsScreen || Minecraft.getInstance().gui.screen() instanceof SignalLostScreen))
            gain=FpvController.settings().interfaceVolume;
        callback.setReturnValue((float)(callback.getReturnValue()*gain));
    }
}
