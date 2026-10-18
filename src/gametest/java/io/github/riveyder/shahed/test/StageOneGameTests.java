package io.github.riveyder.shahed.test;

import java.util.List;
import java.util.UUID;

import io.github.riveyder.shahed.entity.DroneEntity;
import io.github.riveyder.shahed.registry.ModEntities;
import io.github.riveyder.shahed.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/** Regression checks for registration, placement, persistence and an unpiloted drone's gravity. */
public final class StageOneGameTests {
    @GameTest
    public void survivalSpawningPreservesTheCreatorsIdentity(GameTestHelper helper) {
        Vec3 position = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(3, 2, 3)));
        Player survivalPlayer = helper.makeMockServerPlayer(GameType.SURVIVAL);
        survivalPlayer.setYRot(135.0F);
        helper.assertTrue(DroneEntity.spawn(helper.getLevel(), survivalPlayer, position),
                "Survival players must be able to create a drone at an unobstructed position");

        DroneEntity drone = helper.findOneEntity(ModEntities.DRONE);
        helper.assertTrue(drone.getOwnerUuid().filter(survivalPlayer.getUUID()::equals).isPresent(),
                "The spawned drone must store its creator UUID");
        helper.assertTrue(Math.abs(drone.getYRot() - 135.0F) < 0.001F,
                "The spawned drone must face the same direction as its creator");
        helper.assertTrue(drone.position().equals(position), "The drone must spawn at the requested position");
        helper.assertFalse(DroneEntity.spawn(helper.getLevel(), survivalPlayer, position),
                "A second drone must not overlap the first drone");
        helper.assertEntitiesPresent(ModEntities.DRONE, 1);
        helper.succeed();
    }

    @GameTest
    public void spawningRejectsSolidBlocksAndPositionsOutsideTheBuildHeight(GameTestHelper helper) {
        Player creativePlayer = helper.makeMockServerPlayer(GameType.CREATIVE);
        BlockPos obstructedPosition = new BlockPos(3, 2, 3);
        helper.setBlock(obstructedPosition, Blocks.STONE);
        Vec3 position = Vec3.atBottomCenterOf(helper.absolutePos(obstructedPosition));

        helper.assertFalse(DroneEntity.spawn(helper.getLevel(), creativePlayer, position),
                "A drone must not spawn with its hitbox inside a solid block");

        int minimumY = helper.getLevel().dimensionType().minY();
        int maximumY = minimumY + helper.getLevel().dimensionType().height();
        helper.assertFalse(DroneEntity.spawn(helper.getLevel(), creativePlayer,
                        new Vec3(position.x, minimumY - 1, position.z)),
                "A drone must not spawn below the dimension build height");
        helper.assertFalse(DroneEntity.spawn(helper.getLevel(), creativePlayer,
                        new Vec3(position.x, maximumY - 0.5, position.z)),
                "A drone hitbox must fit fully below the upper dimension build height");
        helper.assertEntityNotPresent(ModEntities.DRONE);
        helper.succeed();
    }

    @GameTest
    public void registeredUnpilotedDroneFallsInsteadOfHovering(GameTestHelper helper) {
        helper.assertTrue(id("drone").equals(BuiltInRegistries.ENTITY_TYPE.getKey(ModEntities.DRONE)),
                "The drone entity type must be registered as shahed:drone");

        DroneEntity drone = helper.spawn(ModEntities.DRONE, 3, 24, 3);
        try {
            UUID entityUuid = drone.getUUID();
            Vec3 position = drone.position();
            helper.assertTrue(drone.getType() == ModEntities.DRONE, "Spawned entity has the wrong type");
            helper.assertTrue(drone.level() == helper.getLevel(), "Drone must belong to the server test world");
            helper.assertFalse(drone.isFlying(), "An unpiloted drone must not have active control authority");
            for (int tick = 0; tick < 5; tick++) {
                drone.tick();
            }

            helper.assertTrue(drone.getY() < position.y - 0.05,
                    "An uncontrolled airborne drone without thrust must lose altitude");
            helper.assertTrue(drone.getDeltaMovement().y < 0,
                    "Gravity must act on an unpiloted drone rather than freezing it in the air");
            helper.assertTrue(drone.getUUID().equals(entityUuid), "Ticking must preserve the entity UUID");
            helper.succeed();
        } finally {
            drone.discard();
        }
    }

    @GameTest
    public void equipmentIsRegisteredAndGogglesFitTheHeadSlot(GameTestHelper helper) {
        List<Item> equipment = List.of(ModItems.DRONE, ModItems.CONTROLLER, ModItems.FPV_GOGGLES);
        List<String> names = List.of("drone", "controller", "fpv_goggles");

        for (int index = 0; index < equipment.size(); index++) {
            Item item = equipment.get(index);
            helper.assertTrue(id(names.get(index)).equals(BuiltInRegistries.ITEM.getKey(item)),
                    "Equipment is missing its expected registry identifier: " + names.get(index));
            helper.assertFalse(new ItemStack(item).isEmpty(), "Registered equipment must create a usable item stack");
        }

        Equippable equippable = new ItemStack(ModItems.FPV_GOGGLES).get(DataComponents.EQUIPPABLE);
        helper.assertTrue(equippable != null, "FPV goggles must define an equippable component");
        if (equippable != null) {
            helper.assertTrue(equippable.slot() == EquipmentSlot.HEAD, "FPV goggles must equip in the head slot");
        }
        helper.succeed();
    }

    @GameTest
    public void droneSaveLoadPreservesOwnershipAndOrientation(GameTestHelper helper) {
        UUID ownerUuid = UUID.fromString("88f9de5d-ea85-4dc0-b09e-26d660d59df4");
        DroneEntity original = helper.spawn(ModEntities.DRONE, 3, 40, 3);
        original.setOwnerUuid(ownerUuid);
        original.setYRot(90.0F);

        TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        original.saveWithoutId(output);

        // Keep the restored entity out of the world to avoid adding the saved UUID twice.
        DroneEntity restored = new DroneEntity(ModEntities.DRONE, helper.getLevel());
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING,
                helper.getLevel().registryAccess(), output.buildResult()));

        helper.assertTrue(restored.getOwnerUuid().filter(ownerUuid::equals).isPresent(),
                "Saving and loading must preserve the drone owner UUID");
        helper.assertTrue(restored.getUUID().equals(original.getUUID()),
                "Saving and loading must preserve the entity UUID");
        helper.assertTrue(Math.abs(restored.getYRot() - 90.0F) < 0.001F,
                "Saving and loading must preserve the drone yaw");
        helper.assertTrue(restored.position().distanceToSqr(original.position()) < 0.000001,
                "Saving and loading must preserve the drone position");
        helper.assertFalse(restored.isFlying(), "Loading must not restore a live control session");
        Vec3 savedPosition = restored.position();
        restored.tick();
        helper.assertTrue(restored.getY() < savedPosition.y,
                "A restored drone without thrust must obey gravity");
        helper.succeed();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("shahed", path);
    }
}
