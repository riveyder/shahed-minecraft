package io.github.riveyder.shahed.mixin;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.TicketStorage;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkMap.class)
public interface ChunkMapFpvAccess {
    @Accessor("ticketStorage")
    TicketStorage shahed$ticketStorage();

    @Invoker("updateChunkTracking")
    void shahed$updateChunkTracking(ServerPlayer player);
}
