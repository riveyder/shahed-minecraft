package io.github.riveyder.shahed.test;

import io.github.riveyder.shahed.control.DroneInput;
import io.github.riveyder.shahed.control.DroneSessions;
import io.github.riveyder.shahed.entity.DroneEntity;
import io.github.riveyder.shahed.item.ControllerItem;
import io.github.riveyder.shahed.network.ExitPayload;
import io.github.riveyder.shahed.network.InputPayload;
import io.github.riveyder.shahed.network.SessionPayload;
import io.github.riveyder.shahed.network.TelemetryPayload;
import io.github.riveyder.shahed.registry.ModEntities;
import io.github.riveyder.shahed.registry.ModItems;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

/** Exercises the real server entity, equipment and session paths without a graphical client. */
public final class StageTwoGameTests {
    @GameTest
    public void controlAndTelemetryPacketsRoundTripThroughTheirWireCodecs(GameTestHelper helper) {
        UUID session = UUID.fromString("a4117936-f85e-4c54-b7f6-5cadf8b60d36");
        UUID drone = UUID.fromString("71abde89-3e46-4e5d-85aa-c57c6d16f879");
        InputPayload input = new InputPayload(session, drone, 413,
                input(0.875F, -0.125F, 0.25F, -0.625F, 0.375F, true));
        ExitPayload exit = new ExitPayload(session);
        SessionPayload connected = new SessionPayload(true, session, drone, 13579, "");
        SessionPayload ended = new SessionPayload(false, session, drone, 13579,
                "message.shahed.session.reason.equipment");
        TelemetryPayload telemetry = new TelemetryPayload(session, drone, 12.75, 103.125, 12345);
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                helper.getLevel().registryAccess());
        try {
            // Sequential packets also reveal codecs that accidentally leave unread or consume extra bytes.
            InputPayload.CODEC.encode(buffer, input);
            ExitPayload.CODEC.encode(buffer, exit);
            SessionPayload.CODEC.encode(buffer, connected);
            SessionPayload.CODEC.encode(buffer, ended);
            TelemetryPayload.CODEC.encode(buffer, telemetry);

            helper.assertTrue(input.equals(InputPayload.CODEC.decode(buffer)),
                    "The input codec must preserve session, drone, sequence and every distinct control axis");
            helper.assertTrue(exit.equals(ExitPayload.CODEC.decode(buffer)),
                    "The exit codec must preserve the session UUID");
            helper.assertTrue(connected.equals(SessionPayload.CODEC.decode(buffer)),
                    "The session codec must preserve the active camera target");
            helper.assertTrue(ended.equals(SessionPayload.CODEC.decode(buffer)),
                    "The session codec must preserve termination state and reason");
            helper.assertTrue(telemetry.equals(TelemetryPayload.CODEC.decode(buffer)),
                    "The telemetry codec must preserve speed, altitude and flight time");
            helper.assertFalse(buffer.isReadable(), "All encoded packet bytes must be consumed exactly once");
            helper.succeed();
        } finally {
            buffer.release();
        }
    }

    @GameTest
    public void controlledFlightAcceleratesAndCountsFlightTime(GameTestHelper helper) {
        DroneEntity drone = helper.spawn(ModEntities.DRONE, 3, 24, 3);
        try {
            Vec3 start = drone.position();
            drone.setControlInput(input(1, 0, 1, 0, 0, false));
            drone.tick();
            double initialSpeed = drone.getDeltaMovement().length();
            for (int tick = 0; tick < 29; tick++) {
                drone.setControlInput(input(1, 0, 1, 0, 0, false));
                drone.tick();
            }

            helper.assertTrue(drone.position().z > start.z + 0.1, "Forward input must move the drone forward");
            helper.assertTrue(drone.getDeltaMovement().length() > initialSpeed,
                    "Holding the throttle must accelerate rather than teleport to a fixed speed");
            helper.assertTrue(drone.getFlightTicks() >= 30, "Flight time must advance while flying");
            helper.assertTrue(drone.isFlying(), "Applying controls must arm the flight simulation");
            helper.succeed();
        } finally {
            drone.discard();
        }
    }

    @GameTest
    public void yawPitchAndStrafeProduceAControlledBankedTurn(GameTestHelper helper) {
        DroneEntity drone = helper.spawn(ModEntities.DRONE, 3, 24, 3);
        try {
            drone.setYRot(0);
            drone.setXRot(0);
            // A wing needs airflow before banking can redirect lift and the flight path.
            drone.setDeltaMovement(new Vec3(0, 0, 0.65));
            drone.setControlInput(input(1, 1, 0, 1, 1, false));
            tick(drone, 6);

            helper.assertTrue(drone.getYRot() > 0 && drone.getYRot() <= 27.001F,
                    "Yaw input must turn progressively within the server turn-rate limit");
            helper.assertTrue(drone.getXRot() > 0 && drone.getXRot() <= 65.001F,
                    "Pitch input must tilt the drone within the server pitch limit");
            helper.assertTrue(Math.abs(drone.getRoll()) > 0.1F,
                    "Strafing and turning must produce a visible bank");
            helper.assertTrue(Math.abs(drone.getDeltaMovement().x) > 0.01,
                    "Turning/strafe input must affect the flight trajectory");
            helper.succeed();
        } finally {
            drone.discard();
        }
    }

    @GameTest
    public void brakingRemovesMomentumFasterThanReleasingTheThrottle(GameTestHelper helper) {
        DroneEntity coasting = helper.spawn(ModEntities.DRONE, 2, 24, 2);
        DroneEntity braking = helper.spawn(ModEntities.DRONE, 6, 24, 2);
        try {
            // Identical already-airborne fixtures isolate passive drag from residual engine spool.
            Vec3 cruisingMomentum = new Vec3(0.12, -0.05, 0.5);
            coasting.setDeltaMovement(cruisingMomentum);
            braking.setDeltaMovement(cruisingMomentum);
            double poweredSpeed = horizontalSpeed(cruisingMomentum);
            coasting.setControlInput(DroneInput.IDLE);
            braking.setControlInput(input(0, 0, 0, 0, 0, true));
            for (int tick = 0; tick < 3; tick++) {
                coasting.tick();
                braking.tick();
            }

            helper.assertTrue(horizontalSpeed(coasting.getDeltaMovement()) > 0.001,
                    "Releasing the throttle must preserve some arcade inertia");
            helper.assertTrue(horizontalSpeed(coasting.getDeltaMovement()) < poweredSpeed,
                    "An unpowered drone must gradually lose momentum");
            helper.assertTrue(horizontalSpeed(braking.getDeltaMovement()) < horizontalSpeed(coasting.getDeltaMovement()),
                    "The brake must remove momentum faster than passive drag");
            helper.succeed();
        } finally {
            coasting.discard();
            braking.discard();
        }
    }

    @GameTest
    public void stoppingControlPreservesMomentumAndLetsAnUnpilotedAircraftDescend(GameTestHelper helper) {
        DroneEntity drone = helper.spawn(ModEntities.DRONE, 3, 40, 3);
        try {
            drone.setControlInput(input(1, 0, 0, 1, 0, false));
            tick(drone, 8);
            Vec3 momentum = drone.getDeltaMovement();
            helper.assertTrue(horizontalSpeed(momentum) > 0.05, "The fixture must first be flying forward");
            drone.stopControl();
            helper.assertTrue(drone.getDeltaMovement().equals(momentum),
                    "Leaving FPV must not instantly erase the aircraft's linear momentum");
            Vec3 stopPosition = drone.position();
            tick(drone, 8);

            helper.assertTrue(horizontalDistance(drone.position().subtract(stopPosition)) > 0.1,
                    "An unpiloted aircraft must keep coasting after control ends");
            helper.assertTrue(drone.position().y < stopPosition.y,
                    "An unpiloted aircraft with insufficient thrust must descend instead of hovering");
            helper.assertTrue(horizontalSpeed(drone.getDeltaMovement()) > 0,
                    "Passive drag must not be replaced by an abrupt velocity reset");
            helper.assertFalse(drone.isFlying(), "Leaving FPV must remove the active pilot's throttle authority");
            helper.succeed();
        } finally {
            drone.discard();
        }
    }

    @GameTest
    public void malformedAndExcessiveAxesCannotCorruptFlightState(GameTestHelper helper) {
        DroneEntity drone = helper.spawn(ModEntities.DRONE, 3, 24, 3);
        try {
            drone.setControlInput(input(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
                    Float.NaN, Float.POSITIVE_INFINITY, false));
            drone.tick();
            helper.assertTrue(finite(drone.position()) && finite(drone.getDeltaMovement()),
                    "Non-finite control values must never enter entity position or velocity");
            helper.assertTrue(Float.isFinite(drone.getYRot()) && Float.isFinite(drone.getXRot()),
                    "Non-finite control values must never enter entity rotation");

            // An out-of-band excessive velocity must also be reduced by the server speed limit.
            drone.setDeltaMovement(new Vec3(10, 10, 10));
            drone.setControlInput(input(1000, -1000, 1000, 1000, -1000, false));
            drone.tick();
            helper.assertTrue(drone.getDeltaMovement().length() <= 0.800001,
                    "Excessive control values must respect the server speed limit");
            helper.assertTrue(Math.abs(drone.getXRot()) <= 65.001F,
                    "Excessive pitch input must respect the server pitch limit");
            helper.succeed();
        } finally {
            drone.discard();
        }
    }

    @GameTest
    public void flightCannotPassThroughASolidWall(GameTestHelper helper) {
        for (int x = 1; x <= 5; x++) {
            for (int y = 8; y <= 30; y++) {
                helper.setBlock(x, y, 7, Blocks.STONE);
            }
        }
        DroneEntity drone = helper.spawn(ModEntities.DRONE, 3, 24, 3);
        try {
            drone.setYRot(0);
            double startZ = drone.getZ();
            for (int tick = 0; tick < 20; tick++) {
                drone.setControlInput(input(1, 0, 0, 0, 0, false));
                drone.tick();
            }

            double wallZ = helper.absolutePos(new BlockPos(3, 24, 7)).getZ();
            helper.assertTrue(drone.getZ() > startZ + 0.1, "The drone must first move towards the wall");
            helper.assertTrue(drone.getBoundingBox().maxZ <= wallZ + 0.00001,
                    "The complete drone hitbox must remain in front of the solid wall");
            helper.assertTrue(helper.getLevel().noCollision(drone, drone.getBoundingBox()),
                    "Flight collision handling must leave no solid block inside the drone hitbox");
            helper.assertTrue(Math.abs(drone.getDeltaMovement().z) < 0.000001,
                    "A blocked flight axis must lose its velocity instead of accumulating momentum");
            helper.succeed();
        } finally {
            drone.discard();
        }
    }

    @GameTest
    public void anAbandonedInputStreamReturnsToNeutralWithoutFreezingTheAircraft(GameTestHelper helper) {
        DroneEntity drone = helper.spawn(ModEntities.DRONE, 3, 64, 3);
        try {
            drone.setYRot(0);
            drone.setControlInput(input(1, 0, 0, 0, 0, false));
            tick(drone, 8);
            helper.assertTrue(drone.getDeltaMovement().z > 0.02, "The fixture must first gain forward momentum");
            tick(drone, 38);
            Vec3 before = drone.position();
            double horizontalSpeed = horizontalSpeed(drone.getDeltaMovement());
            tick(drone, 3);

            helper.assertTrue(drone.position().y < before.y,
                    "Timed-out controls must let the aircraft descend, never park it in the sky");
            helper.assertTrue(horizontalSpeed(drone.getDeltaMovement()) < horizontalSpeed,
                    "A stale throttle must be released so drag can reduce forward momentum");
            helper.assertTrue(drone.getDeltaMovement().y < 0,
                    "Input silence must not suppress gravity or zero the complete velocity");
            helper.succeed();
        } finally {
            drone.discard();
        }
    }

    @GameTest
    public void aReloadedDroneRetainsItsOwnerButCannotRestoreAnOldThrottle(GameTestHelper helper) {
        DroneEntity original = helper.spawn(ModEntities.DRONE, 3, 24, 3);
        try {
            UUID owner = UUID.randomUUID();
            original.setOwnerUuid(owner);
            original.setControlInput(input(1, 0, 1, 0, 0, false));
            tick(original, 3);
            TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
            original.saveWithoutId(output);

            DroneEntity restored = new DroneEntity(ModEntities.DRONE, helper.getLevel());
            restored.load(TagValueInput.create(ProblemReporter.DISCARDING,
                    helper.getLevel().registryAccess(), output.buildResult()));
            Vec3 position = restored.position();
            Vec3 savedMomentum = original.getDeltaMovement();
            helper.assertTrue(restored.getDeltaMovement().distanceToSqr(savedMomentum) < 0.000001,
                    "Loading must preserve physical momentum without restoring a pilot or queued throttle");
            restored.tick();
            helper.assertTrue(restored.getOwnerUuid().filter(owner::equals).isPresent(),
                    "Reloading must preserve drone ownership");
            helper.assertTrue(restored.getUUID().equals(original.getUUID()),
                    "Reloading must preserve the UUID used by the controller");
            helper.assertFalse(restored.isFlying(), "A saved drone must not restore a live pilot session");
            helper.assertTrue(restored.getDeltaMovement().y < savedMomentum.y,
                    "A reloaded airborne drone must again experience gravity without an old engine command");
            helper.assertTrue(horizontalSpeed(restored.getDeltaMovement()) < horizontalSpeed(savedMomentum),
                    "A reload must coast under drag rather than resume the old engine command");
            helper.assertTrue(restored.position().distanceToSqr(position) > 0,
                    "Reloading an airborne drone must not create a stationary display object");
            helper.succeed();
        } finally {
            original.discard();
        }
    }

    @GameTest
    public void damageDestroysTheDroneAndEndsItsControlSession(GameTestHelper helper) {
        ServerPlayer pilot = pilot(helper, 2, 2, 2);
        DroneEntity drone = ownedDrone(helper, pilot, 4, 3, 4);
        try {
            ItemStack controller = equippedController(pilot, drone);
            helper.assertTrue(DroneSessions.connect(pilot, drone, controller), "The owner must connect first");
            helper.assertTrue(drone.hurtServer(helper.getLevel(), drone.damageSources().generic(), 20),
                    "A destructible drone must accept finite damage");
            DroneSessions.tick(helper.getLevel().getServer());
            helper.assertTrue(drone.isRemoved(), "Lethal damage must remove the drone");
            helper.assertFalse(DroneSessions.isControlling(pilot),
                    "Destruction must release the player's FPV session");
            helper.succeed();
        } finally {
            DroneSessions.end(pilot, "test_cleanup");
            drone.discard();
        }
    }

    @GameTest
    public void controllerBindingPersistsAndCannotBeUsedByAnotherOwner(GameTestHelper helper) {
        ServerPlayer owner = pilot(helper, 2, 2, 2);
        ServerPlayer stranger = pilot(helper, 3, 2, 2);
        DroneEntity drone = ownedDrone(helper, owner, 4, 3, 4);
        try {
            ItemStack controller = equippedController(owner, drone);
            var ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
            var encoded = ItemStack.CODEC.encodeStart(ops, controller).getOrThrow();
            ItemStack restored = ItemStack.CODEC.parse(ops, encoded).getOrThrow();
            helper.assertTrue(ControllerItem.getBoundDrone(restored).filter(drone.getUUID()::equals).isPresent(),
                    "A saved item stack must retain its exact drone UUID");
            helper.assertTrue(ControllerItem.getBoundOwner(restored).filter(owner.getUUID()::equals).isPresent(),
                    "A saved item stack must retain its binding owner UUID");

            ItemStack unbound = new ItemStack(ModItems.CONTROLLER);
            helper.assertFalse(ControllerItem.bind(stranger, drone, unbound),
                    "A stranger must not bind a controller to an owned drone");
            stranger.setItemSlot(EquipmentSlot.MAINHAND, restored);
            stranger.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.FPV_GOGGLES));
            helper.assertFalse(DroneSessions.connect(stranger, drone, restored),
                    "Copying or stealing the owner's controller must not grant control");
            helper.assertFalse(drone.isFlying(), "Rejected authorization must not start the drone");
            helper.succeed();
        } finally {
            DroneSessions.end(owner, "test_cleanup");
            DroneSessions.end(stranger, "test_cleanup");
            drone.discard();
        }
    }

    @GameTest
    public void connectionRequiresWornGogglesAndTheHeldBoundController(GameTestHelper helper) {
        ServerPlayer pilot = pilot(helper, 2, 2, 2);
        DroneEntity drone = ownedDrone(helper, pilot, 4, 3, 4);
        try {
            ItemStack controller = equippedController(pilot, drone);
            pilot.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            helper.assertFalse(DroneSessions.connect(pilot, drone, controller),
                    "Holding a controller without wearing goggles must not enter FPV");
            pilot.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.FPV_GOGGLES));
            pilot.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            helper.assertFalse(DroneSessions.connect(pilot, drone, controller),
                    "A controller outside the player's hands must not authorize FPV");
            pilot.setItemSlot(EquipmentSlot.MAINHAND, controller);
            helper.assertTrue(DroneSessions.connect(pilot, drone, controller),
                    "The correct held controller and worn goggles must authorize FPV");
            pilot.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            DroneSessions.tick(helper.getLevel().getServer());
            helper.assertFalse(DroneSessions.isControlling(pilot),
                    "Removing goggles must terminate an existing FPV session");
            helper.assertFalse(drone.isFlying(), "Equipment loss must release the pilot controls");
            helper.succeed();
        } finally {
            DroneSessions.end(pilot, "test_cleanup");
            drone.discard();
        }
    }

    @GameTest
    public void usingTheBoundControllerConnectsAndTogglesFpvOff(GameTestHelper helper) {
        ServerPlayer pilot = pilot(helper, 2, 2, 2);
        DroneEntity drone = ownedDrone(helper, pilot, 4, 3, 4);
        try {
            ItemStack controller = equippedController(pilot, drone);
            // ItemStack.use decorates successful results with the transformed held stack;
            // the returned Success is not the InteractionResult.SUCCESS singleton.
            helper.assertTrue(controller.use(helper.getLevel(), pilot, InteractionHand.MAIN_HAND)
                            instanceof InteractionResult.Success,
                    "Using the bound controller while wearing goggles must succeed");
            helper.assertTrue(DroneSessions.isControlling(pilot),
                    "The actual controller item use path must start the owner's FPV session");
            helper.assertTrue(DroneSessions.controlledDrone(pilot).filter(drone.getUUID()::equals).isPresent(),
                    "The controller must select the exact drone UUID saved in the item");
            controller.use(helper.getLevel(), pilot, InteractionHand.MAIN_HAND);
            helper.assertFalse(DroneSessions.isControlling(pilot),
                    "Using the controller again must terminate FPV");
            helper.assertFalse(drone.isFlying(), "Toggling FPV off must release pilot controls");
            helper.succeed();
        } finally {
            DroneSessions.end(pilot, "test_cleanup");
            drone.discard();
        }
    }

    @GameTest
    public void sessionRejectsSpoofedReplayedAndNonFiniteInput(GameTestHelper helper) {
        ServerPlayer owner = pilot(helper, 2, 2, 2);
        ServerPlayer stranger = pilot(helper, 3, 2, 2);
        DroneEntity drone = ownedDrone(helper, owner, 4, 3, 4);
        try {
            ItemStack controller = equippedController(owner, drone);
            helper.assertTrue(DroneSessions.connect(owner, drone, controller), "Owner must connect first");
            UUID session = DroneSessions.sessionId(owner).orElseThrow();
            InputPayload valid = new InputPayload(session, drone.getUUID(), 1, input(1, 0, 0, 0, 0, false));
            helper.assertFalse(DroneSessions.accept(stranger, valid),
                    "Another player must not inject controls into the owner's session");
            helper.assertFalse(DroneSessions.accept(owner, new InputPayload(UUID.randomUUID(), drone.getUUID(),
                            1, input(1, 0, 0, 0, 0, false))),
                    "A forged session UUID must be rejected");
            helper.assertFalse(DroneSessions.accept(owner, new InputPayload(session, UUID.randomUUID(),
                            1, input(1, 0, 0, 0, 0, false))),
                    "A valid session must not authorize a different drone UUID");
            helper.assertFalse(DroneSessions.accept(owner, new InputPayload(session, drone.getUUID(),
                            1, input(Float.NaN, 0, 0, 0, 0, false))),
                    "Network controls containing NaN must be rejected");
            helper.assertTrue(DroneSessions.accept(owner, valid), "A valid owner's first input must be accepted");
            helper.assertFalse(DroneSessions.accept(owner, valid), "An already accepted sequence must not replay");
            DroneSessions.tick(helper.getLevel().getServer());
            drone.tick();
            helper.assertTrue(drone.getDeltaMovement().z > 0,
                    "The accepted owner's input must reach the server flight simulation");
            helper.succeed();
        } finally {
            DroneSessions.end(owner, "test_cleanup");
            DroneSessions.end(stranger, "test_cleanup");
            drone.discard();
        }
    }

    @GameTest
    public void independentPilotsKeepTheirBodiesAndControlsSeparate(GameTestHelper helper) {
        ServerPlayer first = pilot(helper, 2, 2, 2);
        ServerPlayer second = pilot(helper, 5, 2, 2);
        DroneEntity forward = ownedDrone(helper, first, 2, 40, 4);
        DroneEntity climbing = ownedDrone(helper, second, 6, 40, 4);
        try {
            helper.assertTrue(DroneSessions.connect(first, forward, equippedController(first, forward)),
                    "The first pilot must connect to their drone");
            helper.assertTrue(DroneSessions.connect(second, climbing, equippedController(second, climbing)),
                    "A second pilot must independently connect at the same time");
            Vec3 firstBody = first.position();
            Vec3 secondBody = second.position();
            Vec3 forwardStart = forward.position();
            Vec3 climbingStart = climbing.position();
            forward.setDeltaMovement(new Vec3(0, 0, 0.65));
            climbing.setDeltaMovement(new Vec3(0, 0, 0.65));
            helper.assertTrue(DroneSessions.accept(first, new InputPayload(
                            DroneSessions.sessionId(first).orElseThrow(), forward.getUUID(), 1,
                            input(1, 0, 0, 0, 0, false))), "The first pilot's input must be accepted");
            helper.assertTrue(DroneSessions.accept(second, new InputPayload(
                            DroneSessions.sessionId(second).orElseThrow(), climbing.getUUID(), 1,
                            input(0, 0, 1, 0, 0, false))), "The second pilot's input must be accepted");
            for (int tick = 0; tick < 6; tick++) {
                DroneSessions.tick(helper.getLevel().getServer());
                forward.tick();
                climbing.tick();
            }

            helper.assertTrue(forward.getZ() - forwardStart.z > climbing.getZ() - climbingStart.z,
                    "The first pilot's full engine command must produce greater forward acceleration");
            helper.assertTrue(climbing.getDeltaMovement().y > forward.getDeltaMovement().y
                            && climbing.getXRot() < forward.getXRot(),
                    "Only the second pilot's climb command must pitch its aircraft up and redirect acceleration");
            helper.assertTrue(first.position().equals(firstBody) && second.position().equals(secondBody),
                    "Controlling drones must leave both player bodies at their original positions");
            DroneSessions.end(first, "test_exit");
            helper.assertFalse(DroneSessions.isControlling(first), "The exiting pilot must release control");
            helper.assertTrue(DroneSessions.isControlling(second), "One pilot exiting must preserve the other session");
            helper.assertFalse(forward.isFlying(), "The exiting pilot's throttle authority must end");
            helper.succeed();
        } finally {
            DroneSessions.end(first, "test_cleanup");
            DroneSessions.end(second, "test_cleanup");
            forward.discard();
            climbing.discard();
        }
    }

    @GameTest
    public void fpvSuspendsGravityWithoutChangingThePlayersPersistentGravityFlag(GameTestHelper helper) {
        ServerPlayer pilot = pilot(helper, 2, 2, 2);
        DroneEntity drone = ownedDrone(helper, pilot, 4, 4, 4);
        try {
            ItemStack controller = equippedController(pilot, drone);
            pilot.setNoGravity(false);
            helper.assertTrue(pilot.getGravity() > 0, "The fixture must begin with ordinary player gravity");
            helper.assertTrue(DroneSessions.connect(pilot, drone, controller), "The owner must connect first");
            helper.assertFalse(pilot.isNoGravity(),
                    "FPV must not change the persistent NoGravity flag on the player's body");
            helper.assertTrue(pilot.getGravity() == 0,
                    "The server must transiently suspend physical gravity during FPV");
            TagValueOutput activeSave = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
            pilot.saveWithoutId(activeSave);
            helper.assertFalse(activeSave.buildResult().getBooleanOr("NoGravity", false),
                    "Saving a player during FPV must not persist disabled gravity after a crash/restart");
            DroneSessions.end(pilot, "test_exit");
            helper.assertFalse(pilot.isNoGravity(), "Exiting FPV must preserve the original ordinary gravity flag");
            helper.assertTrue(pilot.getGravity() > 0, "Physical gravity must resume after leaving FPV");

            pilot.setNoGravity(true);
            helper.assertTrue(DroneSessions.connect(pilot, drone, controller),
                    "A player whose gravity was already disabled must still be able to connect");
            TagValueOutput preexistingSave = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
            pilot.saveWithoutId(preexistingSave);
            helper.assertTrue(preexistingSave.buildResult().getBooleanOr("NoGravity", false),
                    "FPV must preserve a preexisting persistent NoGravity flag");
            DroneSessions.end(pilot, "test_exit");
            helper.assertTrue(pilot.isNoGravity(), "Cleanup must preserve a preexisting disabled-gravity flag");
            helper.succeed();
        } finally {
            DroneSessions.end(pilot, "test_cleanup");
            drone.discard();
        }
    }

    @GameTest
    public void aSilentControlSessionReleasesStaleThrottleButStaysConnectedAndCanResume(GameTestHelper helper) {
        ServerPlayer pilot = pilot(helper, 2, 2, 2);
        DroneEntity drone = ownedDrone(helper, pilot, 4, 40, 4);
        try {
            ItemStack controller = equippedController(pilot, drone);
            helper.assertTrue(DroneSessions.connect(pilot, drone, controller), "The owner must connect first");
            UUID session = DroneSessions.sessionId(pilot).orElseThrow();
            helper.assertTrue(DroneSessions.accept(pilot, new InputPayload(session, drone.getUUID(), 1,
                            input(1, 0, 0, 0, 0, false))), "Initial throttle must be accepted");
            // Age the real session without simulating flight: the next physics tick must see neutral
            // controls, rather than the old throttle. No artificial drag or velocity reset is injected.
            for (int tick = 0; tick < 100; tick++) {
                DroneSessions.tick(helper.getLevel().getServer());
            }
            helper.assertTrue(DroneSessions.isControlling(pilot),
                    "Missing input packets must not disconnect a live pilot from an existing drone");
            helper.assertTrue(DroneSessions.sessionId(pilot).filter(session::equals).isPresent(),
                    "The same authorized session must survive silence beyond the old timeout");
            Vec3 neutralStart = drone.position();
            tick(drone, 8);
            helper.assertTrue(Math.abs(drone.getZ() - neutralStart.z) < 0.000001,
                    "A silent session must stop replaying the previous forward throttle");
            helper.assertTrue(drone.getY() < neutralStart.y,
                    "Neutral controls must preserve gravity rather than magically hover");
            helper.assertTrue(DroneSessions.accept(pilot, new InputPayload(session, drone.getUUID(), 2,
                            input(1, 0, 1, 0, 0, false))),
                    "A fresh valid packet must resume control in the existing session");
            DroneSessions.tick(helper.getLevel().getServer());
            tick(drone, 6);
            helper.assertTrue(drone.getZ() > neutralStart.z + 0.01,
                    "Recovered input must reach the actual aircraft simulation");
            helper.succeed();
        } finally {
            DroneSessions.end(pilot, "test_cleanup");
            drone.discard();
        }
    }

    @GameTest
    public void deathAndExplicitDisconnectCleanupReleaseTheDrone(GameTestHelper helper) {
        ServerPlayer pilot = pilot(helper, 2, 2, 2);
        DroneEntity drone = ownedDrone(helper, pilot, 4, 3, 4);
        try {
            ItemStack controller = equippedController(pilot, drone);
            helper.assertTrue(DroneSessions.connect(pilot, drone, controller), "The owner must connect first");
            pilot.setHealth(0);
            DroneSessions.tick(helper.getLevel().getServer());
            helper.assertFalse(DroneSessions.isControlling(pilot), "A dead pilot must not retain a session");
            helper.assertFalse(drone.isFlying(), "A dead pilot must lose throttle authority");

            pilot.setHealth(20);
            helper.assertTrue(DroneSessions.connect(pilot, drone, controller), "Cleanup must allow a later new session");
            UUID previousSession = DroneSessions.sessionId(pilot).orElseThrow();
            DroneSessions.end(pilot, "disconnect");
            helper.assertFalse(DroneSessions.isControlling(pilot), "Disconnect cleanup must release the session");
            helper.assertTrue(DroneSessions.connect(pilot, drone, controller), "The owner must be able to reconnect");
            helper.assertFalse(DroneSessions.accept(pilot, new InputPayload(previousSession, drone.getUUID(), 50,
                            input(1, 0, 0, 0, 0, false))),
                    "Packets from an ended session must not control the drone after reconnecting");
            helper.succeed();
        } finally {
            DroneSessions.end(pilot, "test_cleanup");
            drone.discard();
        }
    }

    private static ServerPlayer pilot(GameTestHelper helper, int x, int y, int z) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
        Vec3 position = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(x, y, z)));
        player.setPos(position.x, position.y, position.z);
        return player;
    }

    private static DroneEntity ownedDrone(GameTestHelper helper, ServerPlayer player, int x, int y, int z) {
        DroneEntity drone = helper.spawn(ModEntities.DRONE, x, y, z);
        drone.setOwnerUuid(player.getUUID());
        drone.setYRot(0);
        return drone;
    }

    private static ItemStack equippedController(ServerPlayer player, DroneEntity drone) {
        ItemStack controller = new ItemStack(ModItems.CONTROLLER);
        if (!ControllerItem.bind(player, drone, controller)) {
            throw new AssertionError("The drone owner could not bind their controller");
        }
        player.setItemSlot(EquipmentSlot.MAINHAND, controller);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.FPV_GOGGLES));
        return controller;
    }

    private static DroneInput input(float forward, float strafe, float vertical,
            float yaw, float pitch, boolean brake) {
        return new DroneInput(forward, strafe, vertical, yaw, pitch, brake);
    }

    private static void tick(DroneEntity drone, int count) {
        for (int tick = 0; tick < count; tick++) {
            drone.tick();
        }
    }

    private static void tickSession(GameTestHelper helper, DroneEntity drone, int count) {
        for (int tick = 0; tick < count; tick++) {
            DroneSessions.tick(helper.getLevel().getServer());
            drone.tick();
        }
    }

    private static double horizontalSpeed(Vec3 vector) {
        return Math.sqrt(vector.x * vector.x + vector.z * vector.z);
    }

    private static double horizontalDistance(Vec3 vector) {
        return horizontalSpeed(vector);
    }

    private static boolean finite(Vec3 vector) {
        return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }
}
