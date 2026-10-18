package io.github.riveyder.shahed.network;

import io.github.riveyder.shahed.ShahedMod;
import io.github.riveyder.shahed.control.DroneConfig.Settings;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 0=read, 1=apply, 2=designate current location, 3=prepare test, 4=confirm nonce. */
public record SettingsRequestPayload(int action,long revision,Settings settings,UUID nonce) implements CustomPacketPayload {
    public static final UUID NONE=new UUID(0,0);
    public static final Type<SettingsRequestPayload> TYPE=new Type<>(ShahedMod.id("settings_request_v3"));
    public static final StreamCodec<RegistryFriendlyByteBuf,SettingsRequestPayload> CODEC=StreamCodec.ofMember(
            (p,b)->{b.writeVarInt(p.action);b.writeLong(p.revision);SettingsWire.write(b,p.settings);b.writeUUID(p.nonce);},
            b->new SettingsRequestPayload(b.readVarInt(),b.readLong(),SettingsWire.read(b),b.readUUID()));
    @Override public Type<SettingsRequestPayload> type() {return TYPE;}
}
