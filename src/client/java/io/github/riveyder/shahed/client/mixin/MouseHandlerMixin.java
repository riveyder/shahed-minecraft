package io.github.riveyder.shahed.client.mixin;

import io.github.riveyder.shahed.client.FpvController;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {
    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void shahed$routeMouseToDrone(LocalPlayer player, double yaw, double pitch) {
        if (FpvController.isActive()) {
            FpvController.mouseTurn(yaw, pitch);
        } else {
            player.turn(yaw, pitch);
        }
    }
}
