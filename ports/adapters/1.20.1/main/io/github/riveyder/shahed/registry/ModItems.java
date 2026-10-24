package io.github.riveyder.shahed.registry;

import io.github.riveyder.shahed.ShahedMod;
import io.github.riveyder.shahed.item.DroneItem;
import io.github.riveyder.shahed.item.ControllerItem;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;



public final class ModItems {
    public static final Item DRONE = register("drone", DroneItem::new, equipmentProperties("drone"));
    public static final Item TRAINING_DRONE = register("training_drone", properties -> new DroneItem(properties,true), equipmentProperties("training_drone"));
    public static final Item CONTROLLER = register("controller", ControllerItem::new, equipmentProperties("controller"));
    public static final Item FPV_GOGGLES = register("fpv_goggles",io.github.riveyder.shahed.item.GogglesItem::new,equipmentProperties("fpv_goggles"));

    public static final Item QUAD=register("quad",p->new DroneItem(p,false,io.github.riveyder.shahed.entity.AircraftKind.QUAD),equipmentProperties("quad"));
    public static final Item CONTACT_QUAD=register("contact_quad",p->new DroneItem(p,false,io.github.riveyder.shahed.entity.AircraftKind.CONTACT),equipmentProperties("contact_quad"));
    public static final Item JET=register("jet",p->new DroneItem(p,false,io.github.riveyder.shahed.entity.AircraftKind.JET),equipmentProperties("jet"));
    public static final Item PAYLOAD=register("arcade_payload",Item::new,equipmentProperties("arcade_payload"));

    public static final ResourceKey<CreativeModeTab> EQUIPMENT_TAB_KEY =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, ShahedMod.id("equipment"));

    public static final CreativeModeTab EQUIPMENT_TAB = FabricItemGroup.builder()
            .title(Component.translatable("itemGroup.shahed.equipment"))
            .icon(() -> new ItemStack(DRONE))
            .displayItems((parameters, output) -> {
                output.accept(DRONE);
                output.accept(TRAINING_DRONE);
                output.accept(CONTROLLER);
                output.accept(FPV_GOGGLES);
                output.accept(QUAD);output.accept(CONTACT_QUAD);output.accept(JET);output.accept(PAYLOAD);
            })
            .build();

    private ModItems() {
    }

    private static Item.Properties equipmentProperties(String name){return new Item.Properties().stacksTo(1);}

    private static Item register(String path, Function<Item.Properties, Item> factory, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, ShahedMod.id(path));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties));
    }

    public static void initialize() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, EQUIPMENT_TAB_KEY, EQUIPMENT_TAB);
    }
}
