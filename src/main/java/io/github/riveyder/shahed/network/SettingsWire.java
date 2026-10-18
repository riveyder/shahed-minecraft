package io.github.riveyder.shahed.network;

import io.github.riveyder.shahed.control.DroneConfig.Settings;
import net.minecraft.network.RegistryFriendlyByteBuf;

/** Fixed-size policy codec. Values are bounded at the authority boundary, never trusted from clients. */
final class SettingsWire {
    static void write(RegistryFriendlyByteBuf b,Settings s) {
        b.writeBoolean(s.destroyBlocks());b.writeFloat(s.explosionPower());b.writeBoolean(s.entityDamage());
        b.writeFloat(s.damageScale());b.writeBoolean(s.particles());b.writeBoolean(s.sound());b.writeVarInt(s.maxBlocks());
        b.writeBoolean(s.stabilization());b.writeFloat(s.dragMultiplier());b.writeVarInt(s.activeLimit());
        b.writeBoolean(s.spectators());b.writeVarInt(s.telemetryInterval());
        b.writeBoolean(s.realFire());b.writeBoolean(s.fireSpread());b.writeFloat(s.effectScale());b.writeVarInt(s.audioRange());
        b.writeBoolean(s.deploymentEnabled());b.writeBoolean(s.deploymentAdminOnly());
    }
    static Settings read(RegistryFriendlyByteBuf b) {return new Settings(b.readBoolean(),b.readFloat(),b.readBoolean(),
            b.readFloat(),b.readBoolean(),b.readBoolean(),b.readVarInt(),b.readBoolean(),b.readFloat(),
            b.readVarInt(),b.readBoolean(),b.readVarInt(),b.readBoolean(),b.readBoolean(),b.readFloat(),b.readVarInt(),b.readBoolean(),b.readBoolean());}
    private SettingsWire() {}
}
