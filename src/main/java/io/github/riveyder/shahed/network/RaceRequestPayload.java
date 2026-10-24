package io.github.riveyder.shahed.network;

import io.github.riveyder.shahed.ShahedMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Public actions only: catalogue, join, cancel and preview. No client editor or timing fields. */
public record RaceRequestPayload(int action,String course) implements CustomPacketPayload {
    public static final Type<RaceRequestPayload> TYPE=new Type<>(ShahedMod.id("race_request_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf,RaceRequestPayload> CODEC=StreamCodec.ofMember(
            (p,b)->{b.writeVarInt(p.action);b.writeUtf(p.course,32);},b->new RaceRequestPayload(b.readVarInt(),b.readUtf(32)));
    @Override public Type<RaceRequestPayload> type(){return TYPE;}
}
