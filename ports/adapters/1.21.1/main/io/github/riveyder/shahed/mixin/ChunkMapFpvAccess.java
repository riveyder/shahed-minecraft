package io.github.riveyder.shahed.mixin;
import net.minecraft.server.level.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(ChunkMap.class)
public interface ChunkMapFpvAccess {@Invoker("updateChunkTracking") void shahed$updateChunkTracking(ServerPlayer p);}
