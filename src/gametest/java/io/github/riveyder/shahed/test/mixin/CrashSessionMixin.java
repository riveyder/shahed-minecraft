package io.github.riveyder.shahed.test.mixin;

import io.github.riveyder.shahed.control.DroneSessions;
import io.github.riveyder.shahed.network.SessionPayload;
import io.github.riveyder.shahed.test.CrashEffectsProbe;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DroneSessions.class, remap = false)
public abstract class CrashSessionMixin {
    @Inject(method = "send", at = @At("HEAD"))
    private static void session(ServerPlayer player, CustomPacketPayload payload, CallbackInfo ci) {
        var probe = CrashEffectsProbe.active();
        if (probe != null && payload instanceof SessionPayload session) probe.sessions.add(session);
    }
}
