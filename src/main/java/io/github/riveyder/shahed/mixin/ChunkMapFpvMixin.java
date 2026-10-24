package io.github.riveyder.shahed.mixin;

import io.github.riveyder.shahed.control.DroneSessions;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Stream chunks about the FPV lens while leaving the body's player tickets and position alone. */
@Mixin(ChunkMap.class)
public abstract class ChunkMapFpvMixin {
    @Redirect(method = "updateChunkTracking", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;chunkPosition()Lnet/minecraft/world/level/ChunkPos;"))
    private ChunkPos shahed$remoteViewingCenter(ServerPlayer player) {
        return DroneSessions.trackingChunk(player);
    }
}
