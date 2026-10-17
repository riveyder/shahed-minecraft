package io.github.riveyder.shahed.mixin;

import io.github.riveyder.shahed.control.DroneSessions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Send the nearest camera chunks first, including after returning to the physical player. */
@Mixin(PlayerChunkSender.class)
public abstract class PlayerChunkSenderFpvMixin {
    @Redirect(method = "sendNextChunks", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;chunkPosition()Lnet/minecraft/world/level/ChunkPos;"))
    private ChunkPos shahed$prioritizeCameraChunks(ServerPlayer player) {
        return DroneSessions.trackingChunk(player);
    }
}
