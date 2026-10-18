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
                    .sized(5.2F, 1.35F)
                    .eyeHeight(0.49F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(DRONE_KEY)
    );

    public static final EntityType<DroneEntity> QUAD = aircraft("quad",1.6F,.65F,.44F);
    public static final EntityType<DroneEntity> CONTACT_QUAD = aircraft("contact_quad",1.6F,.7F,.48F);
    public static final EntityType<DroneEntity> JET = aircraft("jet",2.5F,.9F,.55F);
    private static EntityType<DroneEntity> aircraft(String id,float width,float height,float eye){
        var key=ResourceKey.create(Registries.ENTITY_TYPE,ShahedMod.id(id));
        return Registry.register(BuiltInRegistries.ENTITY_TYPE,key,EntityType.Builder.<DroneEntity>of(DroneEntity::new,MobCategory.MISC)
                .sized(width,height).eyeHeight(eye).clientTrackingRange(12).updateInterval(1).build(key));
    }

    private ModEntities() {
    }

    public static void initialize() {
        // Loading the class registers the entity before commands and items use it.
    }
}
