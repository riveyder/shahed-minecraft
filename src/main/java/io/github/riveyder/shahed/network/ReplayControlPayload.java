package io.github.riveyder.shahed.network;

import io.github.riveyder.shahed.ShahedMod;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 0=stop, 1=normal playback, 2=quarter-speed. Server validates ownership and expiry. */
public record ReplayControlPayload(UUID sessionId,int mode,int camera) implements CustomPacketPayload {
    public ReplayControlPayload(UUID sessionId,int mode) {this(sessionId,mode,0);}
    public static final Type<ReplayControlPayload> TYPE=new Type<>(ShahedMod.id("replay_control_v2"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ReplayControlPayload> CODEC=StreamCodec.ofMember(
            (p,b)->{b.writeUUID(p.sessionId);b.writeVarInt(p.mode);b.writeVarInt(p.camera);},
            b->new ReplayControlPayload(b.readUUID(),b.readVarInt(),b.readVarInt()));
    @Override public Type<ReplayControlPayload> type() { return TYPE; }
}
