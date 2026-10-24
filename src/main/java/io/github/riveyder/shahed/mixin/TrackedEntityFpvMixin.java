package io.github.riveyder.shahed.mixin;

import io.github.riveyder.shahed.control.DroneSessions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Apply vanilla entity tracking ranges around the camera; no global range expansion for other players. */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class TrackedEntityFpvMixin {
    @Redirect(method = "updatePlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;position()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 shahed$remoteEntityViewingOrigin(ServerPlayer player) {
        return DroneSessions.trackingPosition(player);
    }
}
