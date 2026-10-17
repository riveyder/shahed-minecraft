package io.github.riveyder.shahed.client.mixin;

import io.github.riveyder.shahed.client.FpvController;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
abstract class MinecraftControlMixin {
    @Inject(method = "handleKeybinds", at = @At("HEAD"), cancellable = true)
    private void shahed$suppressBodyActions(CallbackInfo callback) {
        if (FpvController.isActive()) {
            Minecraft client = (Minecraft) (Object) this;
            client.gui.handleKeybinds();
            FpvController.drainVanillaClicks(client);
            callback.cancel();
        }
    }
}
