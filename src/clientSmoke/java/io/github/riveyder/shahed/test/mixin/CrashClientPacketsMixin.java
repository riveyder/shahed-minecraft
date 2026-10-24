package io.github.riveyder.shahed.test.mixin;

import io.github.riveyder.shahed.test.CrashEffectsProbe;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class CrashClientPacketsMixin {
    // RETURN observes the one client-thread execution after vanilla has handled each packet.
    @Inject(method = "handleExplosion", at = @At("RETURN"))
    private void explosion(ClientboundExplodePacket packet, CallbackInfo ci) { CrashEffectsProbe.clientPacket(packet); }
    @Inject(method = "handleParticleEvent", at = @At("RETURN"))
    private void particles(ClientboundLevelParticlesPacket packet, CallbackInfo ci) { CrashEffectsProbe.clientPacket(packet); }
    @Inject(method = "handleSoundEvent", at = @At("RETURN"))
    private void sound(ClientboundSoundPacket packet, CallbackInfo ci) { CrashEffectsProbe.clientPacket(packet); }
}
