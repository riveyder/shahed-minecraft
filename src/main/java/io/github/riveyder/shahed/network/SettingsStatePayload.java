package io.github.riveyder.shahed.network;

import io.github.riveyder.shahed.ShahedMod;
import io.github.riveyder.shahed.control.DroneConfig.Settings;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SettingsStatePayload(long revision,Settings settings,boolean admin,String status,UUID nonce,String area)
        implements CustomPacketPayload {
    public static final Type<SettingsStatePayload> TYPE=new Type<>(ShahedMod.id("settings_state_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf,SettingsStatePayload> CODEC=StreamCodec.ofMember(
            (p,b)->{b.writeLong(p.revision);SettingsWire.write(b,p.settings);b.writeBoolean(p.admin);b.writeUtf(p.status,256);
                b.writeUUID(p.nonce);b.writeUtf(p.area,128);},
            b->new SettingsStatePayload(b.readLong(),SettingsWire.read(b),b.readBoolean(),b.readUtf(256),b.readUUID(),b.readUtf(128)));
    @Override public Type<SettingsStatePayload> type() {return TYPE;}
}
