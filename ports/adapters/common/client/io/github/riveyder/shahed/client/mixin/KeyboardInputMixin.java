package io.github.riveyder.shahed.client.mixin;

import io.github.riveyder.shahed.client.FpvController;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;

import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** FPV keys drive the drone, never the player's body or vanilla player input packets. */
@Mixin(KeyboardInput.class)
abstract class KeyboardInputMixin extends Input {
    @Inject(method = "tick", at = @At("TAIL"))
    private void shahed$clearBodyInput(CallbackInfo callback) {
        if (FpvController.isActive()) {
            leftImpulse=forwardImpulse=0;
            up=down=left=right=jumping=shiftKeyDown=false;
        }
    }
}
