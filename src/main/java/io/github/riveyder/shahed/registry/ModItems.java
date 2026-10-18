package io.github.riveyder.shahed.registry;

import io.github.riveyder.shahed.ShahedMod;
import io.github.riveyder.shahed.item.DroneItem;
import io.github.riveyder.shahed.item.ControllerItem;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.equipment.Equippable;

public final class ModItems {
    public static final Item DRONE = register("drone", DroneItem::new, equipmentProperties("drone"));
    public static final Item TRAINING_DRONE = register("training_drone", properties -> new DroneItem(properties,true), equipmentProperties("training_drone"));
    public static final Item CONTROLLER = register("controller", ControllerItem::new, equipmentProperties("controller"));
    public static final Item FPV_GOGGLES = register("fpv_goggles", Item::new,
            equipmentProperties("fpv_goggles").component(DataComponents.EQUIPPABLE,
                    Equippable.builder(EquipmentSlot.HEAD).setDamageOnHurt(false).build()));

    public static final ResourceKey<CreativeModeTab> EQUIPMENT_TAB_KEY =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, ShahedMod.id("equipment"));

    public static final CreativeModeTab EQUIPMENT_TAB = FabricCreativeModeTab.builder()
            .title(Component.translatable("itemGroup.shahed.equipment"))
            .icon(() -> new ItemStack(DRONE))
            .displayItems((parameters, output) -> {
                output.accept(DRONE);
                output.accept(TRAINING_DRONE);
                output.accept(CONTROLLER);
                output.accept(FPV_GOGGLES);
            })
            .build();

    private ModItems() {
    }

    private static Item.Properties equipmentProperties(String name) {
        return new Item.Properties().stacksTo(1).component(DataComponents.LORE,
                new ItemLore(name.equals("controller")
                        ?List.of(Component.translatable("item.shahed.controller.hint"),Component.translatable("item.shahed.controller.hint2"))
                        :List.of(Component.translatable("item.shahed." + name + ".hint"))));
    }

    private static Item register(String path, Function<Item.Properties, Item> factory, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, ShahedMod.id(path));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
    }

    public static void initialize() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, EQUIPMENT_TAB_KEY, EQUIPMENT_TAB);
    }
}
