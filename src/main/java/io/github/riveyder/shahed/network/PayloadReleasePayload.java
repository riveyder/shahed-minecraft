package io.github.riveyder.shahed.network;
import io.github.riveyder.shahed.ShahedMod;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
/** No target coordinates, damage or authority supplied by a client. */
public record PayloadReleasePayload(UUID sessionId,UUID aircraftId) implements CustomPacketPayload {
    public static final Type<PayloadReleasePayload> TYPE=new Type<>(ShahedMod.id("payload_release_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PayloadReleasePayload> CODEC=StreamCodec.ofMember(
            (p,b)->{b.writeUUID(p.sessionId);b.writeUUID(p.aircraftId);},b->new PayloadReleasePayload(b.readUUID(),b.readUUID()));
    @Override public Type<PayloadReleasePayload> type(){return TYPE;}
}
