package io.github.riveyder.shahed.item;
public final class EquipmentTooltips {
    public static void append(net.minecraft.world.item.ItemStack stack,java.util.List<net.minecraft.network.chat.Component> lines){
        var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        if(!id.getNamespace().equals("shahed"))return;
        lines.add(net.minecraft.network.chat.Component.translatable("item.shahed."+id.getPath()+".hint"));
        if(id.getPath().equals("controller"))lines.add(net.minecraft.network.chat.Component.translatable("item.shahed.controller.hint2"));
    }
}
