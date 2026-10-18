package io.github.riveyder.shahed.network;

import io.github.riveyder.shahed.ShahedMod;
import io.github.riveyder.shahed.replay.ReplayFrame;
import io.github.riveyder.shahed.replay.ReplayBuffer;
import io.github.riveyder.shahed.replay.ReplayImpact;
import net.minecraft.world.phys.Vec3;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ReplayPayload(UUID sessionId,List<ReplayFrame> frames,int flightTicks,double maximumSpeed,double maximumAltitude,int paint,ReplayImpact impact) implements CustomPacketPayload {
    public ReplayPayload(UUID sessionId,List<ReplayFrame> frames,int flightTicks,double maximumSpeed,double maximumAltitude,int paint) {
        this(sessionId,frames,flightTicks,maximumSpeed,maximumAltitude,paint,
                ReplayImpact.none(frames.isEmpty()?Vec3.ZERO:frames.getLast().position()));
    }
    public ReplayPayload(UUID sessionId,List<ReplayFrame> frames) { this(sessionId,frames,0,0,0,0); }
    public ReplayPayload {
        frames=List.copyOf(frames);
        if(flightTicks<0 || !Double.isFinite(maximumSpeed) || maximumSpeed<0 || !Double.isFinite(maximumAltitude)
                || paint<0 || paint>3) throw new IllegalArgumentException("Replay statistics");
        if(frames.isEmpty()||frames.size()>ReplayBuffer.CAPACITY) throw new IllegalArgumentException("Replay size");
    }
    public static final Type<ReplayPayload> TYPE=new Type<>(ShahedMod.id("replay_frames_v2"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ReplayPayload> CODEC=StreamCodec.ofMember(
            ReplayPayload::write,ReplayPayload::read);
    private static ReplayPayload read(RegistryFriendlyByteBuf b) {
        UUID id=b.readUUID(); int count=b.readVarInt();
        if(count<1||count>ReplayBuffer.CAPACITY) throw new IllegalArgumentException("Replay size");
        List<ReplayFrame> frames=new ArrayList<>(count);
        for(int i=0;i<count;i++) frames.add(new ReplayFrame(b.readDouble(),b.readDouble(),b.readDouble(),
                b.readFloat(),b.readFloat(),b.readFloat()));
        return new ReplayPayload(id,frames,b.readVarInt(),b.readDouble(),b.readDouble(),b.readVarInt(),
                new ReplayImpact(new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readFloat()));
    }
    private void write(RegistryFriendlyByteBuf b) {
        b.writeUUID(sessionId); b.writeVarInt(frames.size());
        for(var f:frames) { b.writeDouble(f.x());b.writeDouble(f.y());b.writeDouble(f.z());
            b.writeFloat(f.yaw());b.writeFloat(f.pitch());b.writeFloat(f.roll()); }
        b.writeVarInt(flightTicks);b.writeDouble(maximumSpeed);b.writeDouble(maximumAltitude);b.writeVarInt(paint);
        b.writeDouble(impact.position().x);b.writeDouble(impact.position().y);b.writeDouble(impact.position().z);
        b.writeBoolean(impact.explosion());b.writeBoolean(impact.particles());b.writeBoolean(impact.sound());b.writeFloat(impact.power());
    }
    @Override public Type<ReplayPayload> type() { return TYPE; }
}
