package io.github.riveyder.shahed.mixin;

import io.github.riveyder.shahed.control.DroneSessions;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The camera is client-only; never use ServerPlayer.setCamera, which moves the physical pilot. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPlayerControlMixin {
    @Shadow
    public ServerPlayer player;

    @Shadow
    public abstract void ackBlockChangesUpTo(int sequence);

    @Inject(method = {"handleMovePlayer", "handlePlayerInput", "handleAttack", "handleInteract", "handleSetCarriedItem"},
            at = @At("HEAD"), cancellable = true)
    private void shahed$holdPilot(CallbackInfo callback) {
        // Let vanilla enqueue a packet that first arrives on the networking thread.
        if (player.level().getServer().isSameThread() && DroneSessions.isControlling(player)) {
            callback.cancel();
        }
    }

    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
    private void shahed$blockBodyActions(ServerboundPlayerActionPacket packet, CallbackInfo callback) {
        if (player.level().getServer().isSameThread() && DroneSessions.isControlling(player)) {
            ackBlockChangesUpTo(packet.getSequence());
            callback.cancel();
        }
    }
}
