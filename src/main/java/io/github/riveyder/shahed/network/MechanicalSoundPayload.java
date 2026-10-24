package io.github.riveyder.shahed.network;
import io.github.riveyder.shahed.ShahedMod;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
/** Server-to-client audio only. Camera listeners may be far from their player's body. */
public record MechanicalSoundPayload(UUID id,Vec3 position,boolean impact) implements CustomPacketPayload {
 public static final Type<MechanicalSoundPayload> TYPE=new Type<>(ShahedMod.id("mechanical_sound_v1"));
 public static final StreamCodec<RegistryFriendlyByteBuf,MechanicalSoundPayload> CODEC=StreamCodec.ofMember(
  (p,b)->{b.writeUUID(p.id);b.writeDouble(p.position.x);b.writeDouble(p.position.y);b.writeDouble(p.position.z);b.writeBoolean(p.impact);},
  b->new MechanicalSoundPayload(b.readUUID(),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readBoolean()));
 public boolean valid(){return Double.isFinite(position.x)&&Double.isFinite(position.y)&&Double.isFinite(position.z)&&Math.abs(position.x)<=30_000_000&&Math.abs(position.z)<=30_000_000&&Math.abs(position.y)<=20_000_000;}
 @Override public Type<MechanicalSoundPayload> type(){return TYPE;}
 public static void emit(net.minecraft.server.level.ServerLevel level,Vec3 at,boolean impact){
  var event=new MechanicalSoundPayload(UUID.randomUUID(),at,impact);
  for(var player:level.players())if(player.connection!=null&&net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player,TYPE))net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,event);
 }
}
