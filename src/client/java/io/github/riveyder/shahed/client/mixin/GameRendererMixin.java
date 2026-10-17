package io.github.riveyder.shahed.client.mixin;

import io.github.riveyder.shahed.client.FpvController;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    @Inject(method = {"renderItemInHand", "bobHurt", "bobView"}, at = @At("HEAD"), cancellable = true)
    private void shahed$hideBodyEffects(CallbackInfo callback) {
        if (FpvController.isActive()) callback.cancel();
    }
}
