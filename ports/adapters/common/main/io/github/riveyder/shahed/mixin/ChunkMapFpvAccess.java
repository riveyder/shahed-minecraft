package io.github.riveyder.shahed.mixin;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(ChunkMap.class)
public interface ChunkMapFpvAccess {}
