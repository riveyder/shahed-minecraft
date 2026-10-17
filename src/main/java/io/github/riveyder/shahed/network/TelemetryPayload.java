package io.github.riveyder.shahed.network;

import io.github.riveyder.shahed.ShahedMod;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record TelemetryPayload(UUID sessionId, UUID droneUuid, double speed, double altitude, float signal, int flightTicks)
        implements CustomPacketPayload {
    public static final Type<TelemetryPayload> TYPE = new Type<>(ShahedMod.id("drone_telemetry"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TelemetryPayload> CODEC =
            StreamCodec.ofMember(TelemetryPayload::write, buffer -> new TelemetryPayload(buffer.readUUID(),
                    buffer.readUUID(), buffer.readDouble(), buffer.readDouble(), buffer.readFloat(), buffer.readVarInt()));

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(sessionId);
        buffer.writeUUID(droneUuid);
        buffer.writeDouble(speed);
        buffer.writeDouble(altitude);
        buffer.writeFloat(signal);
        buffer.writeVarInt(flightTicks);
    }

    @Override
    public Type<TelemetryPayload> type() {
        return TYPE;
    }
}
