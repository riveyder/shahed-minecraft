package io.github.riveyder.shahed.network;

import io.github.riveyder.shahed.control.DroneSessions;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Common registration has no references to client classes, including on dedicated servers. */
public final class DroneNetworking {
    private DroneNetworking() {
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(SettingsRequestPayload.TYPE,SettingsRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SettingsStatePayload.TYPE,SettingsStatePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SettingsRequestPayload.TYPE,
                (payload,context)->io.github.riveyder.shahed.control.DroneSettingsAuthority.receive(context.player(),payload));
        io.github.riveyder.shahed.control.DroneSettingsAuthority.register();
        PayloadTypeRegistry.serverboundPlay().register(ReplayControlPayload.TYPE,ReplayControlPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ReplayControlPayload.TYPE,ReplayControlPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ReplayPayload.TYPE,ReplayPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ReplayControlPayload.TYPE,
                (payload,context)->io.github.riveyder.shahed.replay.DroneReplays.request(context.player(),payload));
        PayloadTypeRegistry.serverboundPlay().register(InputPayload.TYPE, InputPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ExitPayload.TYPE, ExitPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SessionPayload.TYPE, SessionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TelemetryPayload.TYPE, TelemetryPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(InputPayload.TYPE,
                (payload, context) -> DroneSessions.accept(context.player(), payload));
        ServerPlayNetworking.registerGlobalReceiver(ExitPayload.TYPE,
                (payload, context) -> DroneSessions.exit(context.player(), payload.sessionId()));
    }
}
