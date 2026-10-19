package io.github.riveyder.shahed.compat;
import java.util.function.BiConsumer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.server.level.ServerPlayer;
public final class ServerPlayNetworking {
    public record Context(ServerPlayer player){}
    public static <T extends CustomPacketPayload>void registerGlobalReceiver(CustomPacketPayload.Type<T> t,BiConsumer<T,Context> receiver){
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(t.id(),(server,player,handler,buffer,sender)->{
            T packet=PayloadTypeRegistry.serverboundPlay().codec(t).decode(buffer);server.execute(()->receiver.accept(packet,new Context(player)));
        });
    }
    public static boolean canSend(ServerPlayer player,CustomPacketPayload.Type<?> t){return net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player,t.id());}
    @SuppressWarnings({"unchecked","rawtypes"}) public static void send(ServerPlayer player,CustomPacketPayload packet){
        var buffer=PacketByteBufs.create();((StreamCodec)PayloadTypeRegistry.clientboundPlay().codec(packet.type())).encode(buffer,packet);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,packet.type().id(),buffer);
    }
}
