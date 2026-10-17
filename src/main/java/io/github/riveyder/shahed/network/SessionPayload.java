package io.github.riveyder.shahed.network;

import io.github.riveyder.shahed.ShahedMod;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SessionPayload(boolean active, UUID sessionId, UUID droneUuid, int entityId, float range, String reason)
        implements CustomPacketPayload {
    public static final Type<SessionPayload> TYPE = new Type<>(ShahedMod.id("drone_session"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SessionPayload> CODEC =
            StreamCodec.ofMember(SessionPayload::write, buffer -> new SessionPayload(buffer.readBoolean(),
                    buffer.readUUID(), buffer.readUUID(), buffer.readVarInt(), buffer.readFloat(), buffer.readUtf(128)));

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(active);
        buffer.writeUUID(sessionId);
        buffer.writeUUID(droneUuid);
        buffer.writeVarInt(entityId);
        buffer.writeFloat(range);
        buffer.writeUtf(reason, 128);
    }

    @Override
    public Type<SessionPayload> type() {
        return TYPE;
    }
}
