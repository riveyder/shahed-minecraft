package io.github.riveyder.shahed.mixin;
import io.github.riveyder.shahed.control.DroneSessions;
import net.minecraft.server.level.*;
import net.minecraft.core.*;
import net.minecraft.world.level.entity.EntityAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(ChunkMap.class)
public abstract class ChunkMapFpvMixin {
    @Redirect(method={"move","updatePlayerPos"},at=@At(value="INVOKE",target="Lnet/minecraft/core/SectionPos;of(Lnet/minecraft/world/level/entity/EntityAccess;)Lnet/minecraft/core/SectionPos;"))
    private SectionPos cameraSection(EntityAccess entity){return entity instanceof ServerPlayer p?SectionPos.of(BlockPos.containing(DroneSessions.trackingPosition(p))):SectionPos.of(entity);}
    @Redirect(method={"move","updatePlayerPos"},at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerPlayer;getBlockX()I"))
    private int cameraX(ServerPlayer p){return BlockPos.containing(DroneSessions.trackingPosition(p)).getX();}
    @Redirect(method={"move","updatePlayerPos"},at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerPlayer;getBlockZ()I"))
    private int cameraZ(ServerPlayer p){return BlockPos.containing(DroneSessions.trackingPosition(p)).getZ();}
}
