package io.github.riveyder.shahed.network;

import io.github.riveyder.shahed.ShahedMod;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

/** One audiovisual event, not another gameplay explosion. Visual size is a local preference. */
public record ExplosionEffectPayload(UUID id,Vec3 position,long seed,boolean particles,boolean sound) implements CustomPacketPayload {
    public static final Type<ExplosionEffectPayload> TYPE=new Type<>(ShahedMod.id("explosion_effect_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ExplosionEffectPayload> CODEC=StreamCodec.ofMember(
            (p,b)->{b.writeUUID(p.id);b.writeDouble(p.position.x);b.writeDouble(p.position.y);b.writeDouble(p.position.z);
                b.writeLong(p.seed);b.writeBoolean(p.particles);b.writeBoolean(p.sound);},
            b->new ExplosionEffectPayload(b.readUUID(),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readLong(),b.readBoolean(),b.readBoolean()));
    public boolean valid() {return Double.isFinite(position.x)&&Double.isFinite(position.y)&&Double.isFinite(position.z)
            &&Math.abs(position.x)<=30_000_000&&Math.abs(position.z)<=30_000_000&&Math.abs(position.y)<=20_000_000;}
    @Override public Type<ExplosionEffectPayload> type() {return TYPE;}
}
