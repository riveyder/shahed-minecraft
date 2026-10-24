package io.github.riveyder.shahed.compat.client;
import io.github.riveyder.shahed.compat.*;
import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
public final class ClientPlayNetworking {
    public record Context(Minecraft client){}
    public static <T extends CustomPacketPayload>void registerGlobalReceiver(CustomPacketPayload.Type<T> t,BiConsumer<T,Context> receiver){
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(t.id(),(client,handler,buffer,sender)->{
            T packet=PayloadTypeRegistry.clientboundPlay().codec(t).decode(buffer);client.execute(()->receiver.accept(packet,new Context(client)));
        });
    }
    public static boolean canSend(CustomPacketPayload.Type<?> t){return net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(t.id());}
    @SuppressWarnings({"unchecked","rawtypes"}) public static void send(CustomPacketPayload packet){
        var buffer=PacketByteBufs.create();((StreamCodec)PayloadTypeRegistry.serverboundPlay().codec(packet.type())).encode(buffer,packet);
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(packet.type().id(),buffer);
    }
}
