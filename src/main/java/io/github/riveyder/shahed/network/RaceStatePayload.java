package io.github.riveyder.shahed.network;

import io.github.riveyder.shahed.ShahedMod;
import io.github.riveyder.shahed.race.RaceCourse.Gate;
import java.util.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** 0 armed, 1 running, 2 finished, 3 cancelled, 4 error, 5 preview. Geometry only on start/progress. */
public record RaceStatePayload(String course,int kind,int status,int next,int total,long millis,long best,long record,
                               String reason,List<Gate> gates) implements CustomPacketPayload {
    public RaceStatePayload {gates=List.copyOf(gates);if(gates.size()>64)throw new IllegalArgumentException("Gate count");}
    public static final Type<RaceStatePayload> TYPE=new Type<>(ShahedMod.id("race_state_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf,RaceStatePayload> CODEC=StreamCodec.ofMember(
        (p,b)->{
            b.writeUtf(p.course,32);b.writeVarInt(p.kind);b.writeVarInt(p.status);b.writeVarInt(p.next);b.writeVarInt(p.total);
            b.writeVarLong(p.millis);b.writeVarLong(p.best);b.writeVarLong(p.record);b.writeUtf(p.reason,64);b.writeVarInt(p.gates.size());
            for(var g:p.gates){b.writeDouble(g.x());b.writeDouble(g.y());b.writeDouble(g.z());b.writeFloat(g.yaw());b.writeFloat(g.radius());}
        },b->{
            String id=b.readUtf(32);int kind=b.readVarInt(),status=b.readVarInt(),next=b.readVarInt(),total=b.readVarInt();
            long time=b.readVarLong(),best=b.readVarLong(),record=b.readVarLong();String reason=b.readUtf(64);int count=b.readVarInt();
            if(count<0||count>64||kind<0||kind>2||status<0||status>5||total<0||total>64||next<0||next>total
                    ||time<0||best<0||record<0)throw new IllegalArgumentException("Invalid race state");
            var gates=new ArrayList<Gate>();for(int i=0;i<count;i++)gates.add(new Gate(b.readDouble(),b.readDouble(),b.readDouble(),b.readFloat(),b.readFloat()));
            return new RaceStatePayload(id,kind,status,next,total,time,best,record,reason,gates);
        });
    @Override public Type<RaceStatePayload> type(){return TYPE;}
}
