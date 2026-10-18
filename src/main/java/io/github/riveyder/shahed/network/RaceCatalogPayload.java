package io.github.riveyder.shahed.network;

import io.github.riveyder.shahed.ShahedMod;
import java.util.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RaceCatalogPayload(List<Entry> courses) implements CustomPacketPayload {
    public record Entry(String id,int kind,int gates,long best,long record) {
        public Entry {if(id==null||!id.matches("[a-z0-9_-]{1,32}")||kind<0||kind>2||gates<2||gates>64||best<0||record<0||best>3_600_000||record>3_600_000)
            throw new IllegalArgumentException("Invalid catalogue entry");}
    }
    public RaceCatalogPayload {courses=List.copyOf(courses);if(courses.size()>64)throw new IllegalArgumentException("Course count");}
    public static final Type<RaceCatalogPayload> TYPE=new Type<>(ShahedMod.id("race_catalog_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf,RaceCatalogPayload> CODEC=StreamCodec.ofMember(
        (p,b)->{b.writeVarInt(p.courses.size());for(var c:p.courses){b.writeUtf(c.id,32);b.writeVarInt(c.kind);b.writeVarInt(c.gates);b.writeVarLong(c.best);b.writeVarLong(c.record);}},
        b->{int count=b.readVarInt();if(count<0||count>64)throw new IllegalArgumentException("Course count");
            var rows=new ArrayList<Entry>();for(int i=0;i<count;i++)rows.add(new Entry(b.readUtf(32),b.readVarInt(),b.readVarInt(),b.readVarLong(),b.readVarLong()));
            return new RaceCatalogPayload(rows);});
    @Override public Type<RaceCatalogPayload> type(){return TYPE;}
}
