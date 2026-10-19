package io.github.riveyder.shahed.compat;
import java.util.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
public final class PayloadTypeRegistry {
    private static final PayloadTypeRegistry SERVER=new PayloadTypeRegistry(),CLIENT=new PayloadTypeRegistry();
    private final Map<ResourceLocation,StreamCodec<FriendlyByteBuf,?>> codecs=new HashMap<>();
    public static PayloadTypeRegistry serverboundPlay(){return SERVER;}public static PayloadTypeRegistry clientboundPlay(){return CLIENT;}
    public <T extends CustomPacketPayload>void register(CustomPacketPayload.Type<T> t,StreamCodec<FriendlyByteBuf,T> codec){if(codecs.putIfAbsent(t.id(),codec)!=null)throw new IllegalStateException("Duplicate packet "+t.id());}
    @SuppressWarnings("unchecked") public <T extends CustomPacketPayload>StreamCodec<FriendlyByteBuf,T> codec(CustomPacketPayload.Type<T> t){return (StreamCodec<FriendlyByteBuf,T>)Objects.requireNonNull(codecs.get(t.id()),"Unregistered packet");}
}
