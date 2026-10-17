package io.github.riveyder.shahed.test;

import io.github.riveyder.shahed.control.DroneInput;
import io.github.riveyder.shahed.entity.DroneEntity;
import io.github.riveyder.shahed.registry.ModEntities;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Acceptance regressions for physical coasting, drag, angular inertia and unpowered descent. */
public final class PolishFlightGameTests {
    @GameTest
    public void disconnectCutsThrustWithoutSnappingAndSettlesNoseDown(GameTestHelper helper) {
        DroneEntity drone = helper.spawn(ModEntities.DRONE, 3, 40, 3);
        try {
            for (int i=0;i<8;i++) {
                drone.setControlInput(new DroneInput(1,0,0,0,0,false));
                drone.tick();
            }
            helper.assertTrue(drone.getThrottle()>0.1F, "Fixture must be powered");
            Vec3 momentum=drone.getDeltaMovement();
            float attitude=drone.getXRot();
            drone.stopControl();
            helper.assertTrue(drone.getThrottle()==0, "Disconnect must cut engine thrust immediately");
            helper.assertTrue(drone.getDeltaMovement().equals(momentum), "Disarm must retain momentum");
            helper.assertTrue(drone.getXRot()==attitude, "Disarm must retain attitude");
            drone.tick();
            helper.assertTrue(drone.getXRot()>attitude && drone.getXRot()-attitude<0.3F,
                    "The nose must lower gradually through angular acceleration");
            helper.assertTrue(drone.getDeltaMovement().z>momentum.z*0.9,
                    "The first powerless tick must preserve most forward momentum");
            double altitude=drone.getY();
            for(int i=0;i<10;i++) drone.tick();
            helper.assertTrue(drone.getY()<altitude && drone.getXRot()>attitude,
                    "The unpiloted aircraft must descend nose-down");
            helper.succeed();
        } finally { drone.discard(); }
    }

    @GameTest
    public void releasingPoweredForwardFlightPreservesMotionAndDirection(GameTestHelper helper) {
        DroneEntity drone = helper.spawn(ModEntities.DRONE, 3, 40, 3);
        try {
            drone.setYRot(0);
            for (int tick = 0; tick < 8; tick++) {
                drone.setControlInput(new DroneInput(1, 0, 0, 0, 0, false));
                drone.tick();
            }
            Vec3 poweredVelocity = drone.getDeltaMovement();
            Vec3 releasePosition = drone.position();
            helper.assertTrue(poweredVelocity.z > 0.02, "The fixture must first accelerate forward");
            drone.setControlInput(DroneInput.IDLE);
            helper.assertTrue(drone.getDeltaMovement().equals(poweredVelocity),
                    "Releasing controls must not immediately replace physical momentum");
            drone.tick();
            helper.assertTrue(drone.getZ() > releasePosition.z + poweredVelocity.z * 0.7,
                    "The aircraft must continue along its previous forward trajectory after key release");
            helper.assertTrue(drone.getDeltaMovement().z > poweredVelocity.z * 0.7,
                    "A neutral input must preserve most of the previous tick's forward momentum");
            helper.succeed();
        } finally {
            drone.discard();
        }
    }

    @GameTest
    public void anUnpoweredAircraftLosesHorizontalSpeedGraduallyAtEveryTick(GameTestHelper helper) {
        DroneEntity drone = helper.spawn(ModEntities.DRONE, 3, 40, 3);
        try {
            // Engine-off cruising isolates aerodynamic drag from the powered engine's spool-down.
            drone.setDeltaMovement(new Vec3(0.1, 0, 0.45));
            double initialSpeed = horizontalSpeed(drone.getDeltaMovement());
            double previousSpeed = initialSpeed;
            Vec3 start = drone.position();
            for (int tick = 0; tick < 12; tick++) {
                drone.tick();
                double speed = horizontalSpeed(drone.getDeltaMovement());
                helper.assertTrue(speed < previousSpeed,
                        "Passive drag must reduce horizontal speed on each unpowered tick");
                helper.assertTrue(speed > previousSpeed * 0.75,
                        "Passive drag must slow the aircraft smoothly without an abrupt momentum reset");
                previousSpeed = speed;
            }
            helper.assertTrue(previousSpeed > initialSpeed * 0.2,
                    "A short unpowered glide must retain meaningful forward momentum");
            helper.assertTrue(drone.getZ() > start.z + 2,
                    "The aircraft must travel through the world while its momentum decays");
            helper.assertTrue(drone.getY() < start.y,
                    "Insufficient thrust and lift must produce descent during the glide");
            helper.succeed();
        } finally {
            drone.discard();
        }
    }

    @GameTest
    public void neutralControlsFromRestCannotCreateAMagicalHover(GameTestHelper helper) {
        DroneEntity drone = helper.spawn(ModEntities.DRONE, 3, 40, 3);
        try {
            Vec3 start = drone.position();
            drone.setControlInput(DroneInput.IDLE);
            drone.tick();
            double firstDownwardSpeed = -drone.getDeltaMovement().y;
            for (int tick = 0; tick < 7; tick++) {
                drone.setControlInput(DroneInput.IDLE);
                drone.tick();
            }
            helper.assertTrue(firstDownwardSpeed > 0,
                    "Gravity must act immediately on a stationary aircraft with no thrust");
            helper.assertTrue(-drone.getDeltaMovement().y > firstDownwardSpeed,
                    "An aircraft with insufficient lift must progressively accelerate downward");
            helper.assertTrue(drone.getY() < start.y - 0.2,
                    "Fresh neutral packets must not keep an armed drone parked in the air");
            helper.assertTrue(horizontalSpeed(drone.getDeltaMovement()) < 0.000001,
                    "An aircraft cannot generate forward drive from a neutral input");
            helper.succeed();
        } finally {
            drone.discard();
        }
    }

    @GameTest
    public void releasingTurnInputPreservesAndGraduallyDampsAngularMomentum(GameTestHelper helper) {
        DroneEntity drone = helper.spawn(ModEntities.DRONE, 3, 40, 3);
        try {
            drone.setYRot(0);
            float previousYaw = 0;
            for (int tick = 0; tick < 6; tick++) {
                previousYaw = drone.getYRot();
                drone.setControlInput(new DroneInput(0, 0, 0, 1, 0, false));
                drone.tick();
            }
            float poweredTurnRate = Mth.wrapDegrees(drone.getYRot() - previousYaw);
            float releaseYaw = drone.getYRot();
            float releaseRoll = drone.getRoll();
            drone.setControlInput(DroneInput.IDLE);
            drone.tick();
            float firstCoastingTurnRate = Mth.wrapDegrees(drone.getYRot() - releaseYaw);
            helper.assertTrue(poweredTurnRate > 0.1F, "The fixture must first build angular velocity");
            helper.assertTrue(firstCoastingTurnRate > poweredTurnRate * 0.2F,
                    "Releasing the turn key must retain angular momentum rather than freeze heading");
            helper.assertTrue(firstCoastingTurnRate < poweredTurnRate,
                    "Angular damping must progressively slow an uncommanded turn");
            helper.assertTrue(Math.abs(drone.getRoll()) > Math.abs(releaseRoll) * 0.4F,
                    "A banked aircraft must not instantly snap level when the turn key is released");
            float beforeSecondTick = drone.getYRot();
            drone.tick();
            float secondCoastingTurnRate = Mth.wrapDegrees(drone.getYRot() - beforeSecondTick);
            helper.assertTrue(secondCoastingTurnRate > 0 && secondCoastingTurnRate < firstCoastingTurnRate,
                    "Angular momentum must continue decaying smoothly over successive neutral ticks");
            helper.succeed();
        } finally {
            drone.discard();
        }
    }

    @GameTest
    public void changingHeadingDoesNotTeleportTheExistingVelocityDirection(GameTestHelper helper) {
        DroneEntity drone = helper.spawn(ModEntities.DRONE, 3, 40, 3);
        try {
            drone.setDeltaMovement(new Vec3(0, 0, 0.45));
            drone.setYRot(90);
            drone.setControlInput(new DroneInput(1, 0, 0, 0, 0, false));
            drone.tick();
            helper.assertTrue(drone.getDeltaMovement().z > 0.35,
                    "Changing nose direction must not rotate accumulated forward momentum instantly");
            helper.assertTrue(drone.getDeltaMovement().x < 0,
                    "Fresh engine thrust must gradually add acceleration along the new nose direction");
            helper.assertTrue(Math.abs(drone.getDeltaMovement().x) < drone.getDeltaMovement().z,
                    "A one-tick heading change must remain dominated by the aircraft's existing momentum");
            helper.succeed();
        } finally {
            drone.discard();
        }
    }

    @GameTest
    public void pitchChangesTheDirectionOfThrustAndVerticalFlight(GameTestHelper helper) {
        DroneEntity level = helper.spawn(ModEntities.DRONE, 2, 40, 3);
        DroneEntity noseUp = helper.spawn(ModEntities.DRONE, 7, 40, 3);
        try {
            level.setYRot(0);
            noseUp.setYRot(0);
            level.setXRot(0);
            noseUp.setXRot(-30);
            for (int tick = 0; tick < 8; tick++) {
                level.setControlInput(new DroneInput(1, 0, 0, 0, 0, false));
                noseUp.setControlInput(new DroneInput(1, 0, 0, 0, 0, false));
                level.tick();
                noseUp.tick();
            }
            helper.assertTrue(noseUp.getDeltaMovement().y > level.getDeltaMovement().y + 0.005,
                    "Tilting the nose upward must affect vertical acceleration, not just the camera");
            helper.assertTrue(noseUp.getDeltaMovement().z < level.getDeltaMovement().z,
                    "Upward-pitched thrust must trade some forward acceleration for vertical acceleration");
            helper.succeed();
        } finally {
            level.discard();
            noseUp.discard();
        }
    }

    @GameTest
    public void climbAssistGainsAltitudeWhenTheWingsHaveAirspeed(GameTestHelper helper) {
        DroneEntity drone = helper.spawn(ModEntities.DRONE, 3, 40, 3);
        try {
            // This is a winged aircraft: climb must be checked at flying speed, not as a hovercraft.
            drone.setYRot(0);
            drone.setDeltaMovement(new Vec3(0, 0, 0.75));
            double startY = drone.getY();
            for (int tick = 0; tick < 30; tick++) {
                drone.setControlInput(new DroneInput(1, 0, 1, 0, 0, false));
                drone.tick();
            }
            helper.assertTrue(drone.getY() > startY + 0.1,
                    "Forward thrust and climb assist must gain altitude once sufficient airflow exists");
            helper.assertTrue(drone.getDeltaMovement().y > 0.02,
                    "A sustained climb input at flying speed must yield upward physical velocity");
            helper.assertTrue(drone.getDeltaMovement().z > 0.3,
                    "Climbing must preserve the forward flight required by the wings");
            helper.succeed();
        } finally {
            drone.discard();
        }
    }

    @GameTest
    public void anUnpoweredDroneCanTouchDownWithoutPassingThroughTheGround(GameTestHelper helper) {
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 7; z++) {
                helper.setBlock(x, 8, z, Blocks.STONE);
            }
        }
        DroneEntity drone = helper.spawn(ModEntities.DRONE, 3, 13, 3);
        try {
            for (int tick = 0; tick < 40; tick++) {
                drone.tick();
            }
            double surfaceY = helper.absolutePos(new net.minecraft.core.BlockPos(3, 8, 3)).getY() + 1;
            helper.assertTrue(Math.abs(drone.getBoundingBox().minY - surfaceY) < 0.00001,
                    "Gravity must bring the complete hitbox to the solid surface");
            helper.assertTrue(helper.getLevel().noCollision(drone, drone.getBoundingBox()),
                    "The settled aircraft must not overlap a solid ground block");
            Vec3 settled = drone.position();
            for (int tick = 0; tick < 5; tick++) {
                drone.tick();
            }
            helper.assertTrue(drone.position().distanceToSqr(settled) < 0.000001,
                    "Ground contact may stop downward movement without an artificial airborne hover");
            helper.succeed();
        } finally {
            drone.discard();
        }
    }

    private static double horizontalSpeed(Vec3 velocity) {
        return Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
    }
}
