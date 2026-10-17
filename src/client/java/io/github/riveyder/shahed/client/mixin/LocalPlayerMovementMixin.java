package io.github.riveyder.shahed.client.mixin;

import io.github.riveyder.shahed.client.FpvController;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep the real body anchored without changing persisted gravity/ability flags. */
@Mixin(LocalPlayer.class)
abstract class LocalPlayerMovementMixin {
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void shahed$freezeBody(MoverType type, Vec3 movement, CallbackInfo callback) {
        if (FpvController.isActive()) {
            LocalPlayer player = (LocalPlayer) (Object) this;
            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance = 0;
            callback.cancel();
        }
    }
}
