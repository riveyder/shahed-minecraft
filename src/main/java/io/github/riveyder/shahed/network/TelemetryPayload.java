package io.github.riveyder.shahed.network;

import io.github.riveyder.shahed.ShahedMod;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record TelemetryPayload(UUID sessionId, UUID droneUuid, double speed, double altitude, int flightTicks,
                               double verticalSpeed, float heading, float pitch, float roll, float throttle)
        implements CustomPacketPayload {
    public TelemetryPayload(UUID sessionId, UUID droneUuid, double speed, double altitude, int flightTicks) {
        this(sessionId,droneUuid,speed,altitude,flightTicks,0,0,0,0,0);
    }
    public TelemetryPayload {
        if(!Double.isFinite(speed)||!Double.isFinite(altitude)||!Double.isFinite(verticalSpeed)
                ||!Float.isFinite(heading)||!Float.isFinite(pitch)||!Float.isFinite(roll)||!Float.isFinite(throttle)
                ||speed<0||flightTicks<0||throttle<0||throttle>1) throw new IllegalArgumentException("Telemetry values");
    }
    public static final Type<TelemetryPayload> TYPE = new Type<>(ShahedMod.id("drone_telemetry_v3"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TelemetryPayload> CODEC =
            StreamCodec.ofMember(TelemetryPayload::write, buffer -> new TelemetryPayload(buffer.readUUID(),
                    buffer.readUUID(), buffer.readDouble(), buffer.readDouble(), buffer.readVarInt(),
                    buffer.readDouble(),buffer.readFloat(),buffer.readFloat(),buffer.readFloat(),buffer.readFloat()));

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(sessionId);
        buffer.writeUUID(droneUuid);
        buffer.writeDouble(speed);
        buffer.writeDouble(altitude);
        buffer.writeVarInt(flightTicks);
        buffer.writeDouble(verticalSpeed);buffer.writeFloat(heading);buffer.writeFloat(pitch);
        buffer.writeFloat(roll);buffer.writeFloat(throttle);
    }

    @Override
    public Type<TelemetryPayload> type() {
        return TYPE;
    }
}
