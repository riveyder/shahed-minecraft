package io.github.riveyder.shahed.mixin;

import io.github.riveyder.shahed.control.DroneSessions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Suspend physical gravity without changing the persistent/synchronized NoGravity entity flag. */
@Mixin(Entity.class)
public abstract class ServerPilotGravityMixin {
    @Inject(method = "getGravity", at = @At("HEAD"), cancellable = true)
    private void shahed$transientPilotGravity(CallbackInfoReturnable<Double> callback) {
        if ((Object) this instanceof ServerPlayer player && DroneSessions.isControlling(player)) {
            callback.setReturnValue(0.0);
        }
    }
}
