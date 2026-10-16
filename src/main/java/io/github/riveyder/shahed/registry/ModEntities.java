package io.github.riveyder.shahed.registry;

import io.github.riveyder.shahed.ShahedMod;
import io.github.riveyder.shahed.entity.DroneEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    private static final ResourceKey<EntityType<?>> DRONE_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, ShahedMod.id("drone"));

    public static final EntityType<DroneEntity> DRONE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            DRONE_KEY,
            EntityType.Builder.<DroneEntity>of(DroneEntity::new, MobCategory.MISC)
                    .sized(2.0F, 1.0F)
                    .clientTrackingRange(8)
                    .updateInterval(3)
                    .build(DRONE_KEY)
    );

    private ModEntities() {
    }

    public static void initialize() {
        // Loading the class registers the entity before commands and items use it.
    }
}
