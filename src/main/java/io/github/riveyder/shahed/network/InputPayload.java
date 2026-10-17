package io.github.riveyder.shahed.network;

import io.github.riveyder.shahed.ShahedMod;
import io.github.riveyder.shahed.control.DroneInput;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record InputPayload(UUID sessionId, UUID droneUuid, int sequence, DroneInput input)
        implements CustomPacketPayload {
    public static final Type<InputPayload> TYPE = new Type<>(ShahedMod.id("drone_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InputPayload> CODEC =
            StreamCodec.ofMember(InputPayload::write, InputPayload::read);

    private static InputPayload read(RegistryFriendlyByteBuf buffer) {
        return new InputPayload(buffer.readUUID(), buffer.readUUID(), buffer.readVarInt(),
                new DroneInput(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                        buffer.readFloat(), buffer.readFloat(), buffer.readBoolean()));
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(sessionId);
        buffer.writeUUID(droneUuid);
        buffer.writeVarInt(sequence);
        buffer.writeFloat(input.forward());
        buffer.writeFloat(input.strafe());
        buffer.writeFloat(input.vertical());
        buffer.writeFloat(input.yaw());
        buffer.writeFloat(input.pitch());
        buffer.writeBoolean(input.brake());
    }

    @Override
    public Type<InputPayload> type() {
        return TYPE;
    }
}
