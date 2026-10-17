package io.github.riveyder.shahed.network;

import io.github.riveyder.shahed.ShahedMod;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ExitPayload(UUID sessionId) implements CustomPacketPayload {
    public static final Type<ExitPayload> TYPE = new Type<>(ShahedMod.id("drone_exit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExitPayload> CODEC =
            StreamCodec.ofMember((payload, buffer) -> buffer.writeUUID(payload.sessionId()),
                    buffer -> new ExitPayload(buffer.readUUID()));

    @Override
    public Type<ExitPayload> type() {
        return TYPE;
    }
}
