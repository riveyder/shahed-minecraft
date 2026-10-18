package io.github.riveyder.shahed.test;

import io.github.riveyder.shahed.control.DroneConfig;
import io.github.riveyder.shahed.control.DroneInput;
import io.github.riveyder.shahed.control.DroneSessions;
import io.github.riveyder.shahed.item.ControllerItem;
import io.github.riveyder.shahed.network.ReplayControlPayload;
import io.github.riveyder.shahed.registry.ModItems;
import io.github.riveyder.shahed.replay.DroneReplays;
import io.github.riveyder.shahed.registry.ModEntities;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class DestructionGameTests {
    @GameTest
    public void disconnectedPilotCannotPreventSubsequentCrash(GameTestHelper helper) {
        var pilot=(ServerPlayer)helper.makeMockServerPlayer(GameType.CREATIVE);
        var drone=helper.spawn(ModEntities.DRONE,3,90,3);
        drone.setOwnerUuid(pilot.getUUID());
        var controller=new ItemStack(ModItems.CONTROLLER);
        ControllerItem.bind(pilot,drone,controller);
        pilot.setItemSlot(EquipmentSlot.MAINHAND,controller);
        pilot.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ModItems.FPV_GOGGLES));
        for(int y=89;y<=92;y++) for(int z=-1;z<=7;z++) helper.setBlock(7,y,z,Blocks.STONE);
        try(var effects=new CrashEffectsProbe()) {
            helper.assertTrue(DroneSessions.connect(pilot,drone,controller),"Pilot must connect before disconnect");
            drone.tick(); // Start a flight recording.
            double wall=helper.absolutePos(new BlockPos(7,90,3)).getX();
            drone.setPos(wall-drone.getBbWidth()/2.0-.65,drone.getY(),drone.getZ());
            drone.setYRot(-90);
            drone.setDeltaMovement(new Vec3(.8,0,0));
            DroneSessions.end(pilot,"disconnect");
            helper.assertFalse(DroneSessions.hasFlightSession(pilot),"Disconnect must release operator");
            drone.tick();
            helper.assertTrue(drone.isDestroyed() && drone.isRemoved() && effects.explosions==1,
                    "Crash detection must continue after the operator's session has disconnected");
            helper.assertTrue(drone.replayFrames().size()>=2,"Disconnected trajectory must still reach terminal frame");
            helper.succeed();
        } finally { DroneSessions.end(pilot,"cleanup");drone.discard(); }
    }

    @GameTest
    public void negativeAxisAndCeilingCollisionsUsePreImpactSpeed(GameTestHelper helper) {
        for(int y=69;y<=74;y++) for(int z=-1;z<=7;z++) helper.setBlock(-1,y,z,Blocks.STONE);
        for(int x=0;x<=7;x++) for(int z=-1;z<=7;z++) helper.setBlock(x,74,z,Blocks.STONE);
        for(boolean ceiling:new boolean[]{false,true}) {
            var drone=helper.spawn(ModEntities.DRONE,3,70,3);
            try(var effects=new CrashEffectsProbe()) {
                if(ceiling) {
                    double surface=helper.absolutePos(new BlockPos(3,74,3)).getY();
                    drone.setPos(drone.getX(),surface-drone.getBbHeight()-.65,drone.getZ());
                    drone.setDeltaMovement(new Vec3(0,.8,0));
                } else {
                    double surface=helper.absolutePos(new BlockPos(-1,70,3)).getX()+1;
                    drone.setPos(surface+drone.getBbWidth()/2.0+.65,drone.getY(),drone.getZ());
                    drone.setYRot(90);
                    drone.setDeltaMovement(new Vec3(-.8,0,0));
                }
                drone.tick();
                helper.assertTrue(drone.isDestroyed() && effects.explosions==1,"Impact must work on either side of a collision axis");
            } finally { drone.discard(); }
        }
        helper.succeed();
    }

    @GameTest
    public void crashEmitsEffectsOnceNotifiesPilotAndRetainsReplayWithoutBreakingBlocks(GameTestHelper helper) {
        var pilot=(ServerPlayer)helper.makeMockServerPlayer(GameType.CREATIVE);
        var drone=helper.spawn(ModEntities.DRONE,3,80,3);
        drone.setOwnerUuid(pilot.getUUID());
        var controller=new ItemStack(ModItems.CONTROLLER);
        ControllerItem.bind(pilot,drone,controller);
        pilot.setItemSlot(EquipmentSlot.MAINHAND,controller);
        pilot.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ModItems.FPV_GOGGLES));
        var wall=helper.absolutePos(new BlockPos(7,80,3));
        helper.setBlock(7,80,3,Blocks.GLASS);
        try (var effects=new CrashEffectsProbe()) {
            helper.assertFalse(DroneConfig.get().destroyBlocks(),"Test server must use safe default config");
            helper.assertTrue(DroneSessions.connect(pilot,drone,controller),"Pilot must connect");
            var session=DroneSessions.sessionId(pilot).orElseThrow();
            drone.setPos(wall.getX()-drone.getBbWidth()/2.0-.65,drone.getY(),drone.getZ());
            drone.setYRot(-90);
            drone.setDeltaMovement(new Vec3(.8,0,0));
            drone.tick();
            helper.assertTrue(drone.isDestroyed() && drone.isRemoved(),"Collision must remove drone: position="
                    +drone.position()+", velocity="+drone.getDeltaMovement()+", bounds="+drone.getBoundingBox()+", wall="+wall);
            helper.assertTrue(effects.explosions==1,"Actual vanilla explosion must run exactly once");
            helper.assertTrue(effects.explosionParticles>=1 && effects.smokeParticles>=20 && effects.blastSounds==1,
                    "Server must emit visible particles, smoke and audible blast even with destroyBlocks=false");
            helper.assertTrue(Math.abs(effects.blastPosition.x-(wall.getX()-.01))<.001,
                    "Blast must originate at wall contact, not at the centre of the wide drone");
            helper.assertBlockPresent(Blocks.GLASS,7,80,3);
            helper.assertTrue(effects.sessions.stream().filter(p->!p.active()
                    && p.sessionId().equals(session) && p.droneUuid().equals(drone.getUUID())
                    && p.reason().equals("message.shahed.session.reason.destroyed")).count()==1,
                    "Server must produce exactly one destroyed notification for the current pilot");
            helper.assertFalse(DroneSessions.hasFlightSession(pilot),"Destroyed flight session must end");
            helper.assertTrue(drone.replayFrames().size()>=2,"Flight and terminal collision frame must be retained");
            helper.assertTrue(drone.replayFrames().getLast().position().distanceToSqr(drone.position())<.0001,
                    "Replay must end at the collision position");
            helper.assertTrue(DroneReplays.request(pilot,new ReplayControlPayload(session,2)),
                    "Collision must capture replay automatically before ending the session");
            helper.assertFalse(drone.destroy(),"Duplicate destruction must be rejected");
            drone.hurtServer(helper.getLevel(),drone.damageSources().generic(),100);
            drone.tick();
            helper.assertTrue(effects.explosions==1 && effects.blastSounds==1,"Replay and late damage must not explode again");
            helper.succeed();
        } finally { DroneReplays.stop(pilot,false);DroneSessions.end(pilot,"cleanup");drone.discard(); }
    }

    @GameTest
    public void thinWallImpactsAtDifferentTickOffsetsAndSpeedsAlwaysExplode(GameTestHelper helper) {
        for(int y=69;y<=72;y++) for(int z=-1;z<=7;z++) helper.setBlock(7,y,z,Blocks.IRON_BARS);
        double surface=helper.absolutePos(new BlockPos(7,70,3)).getX()+.4375;
        for(double speed:new double[]{.4,.8}) for(double fraction:new double[]{.02,.5,.9}) {
            var drone=helper.spawn(ModEntities.DRONE,3,70,3);
            try (var effects=new CrashEffectsProbe()) {
                drone.setPos(surface-drone.getBbWidth()/2.0-speed*fraction,drone.getY(),drone.getZ());
                drone.setYRot(-90);
                drone.setDeltaMovement(new Vec3(speed,0,0));
                drone.tick();
                helper.assertTrue(drone.isDestroyed() && effects.explosions==1,
                        "Thin wall must explode drone at speed "+speed+" and tick fraction "+fraction);
                helper.assertTrue(drone.getBoundingBox().maxX<=surface+.001,"Swept box must not tunnel through thin wall");
            } finally { drone.discard(); }
        }
        helper.succeed();
    }

    @GameTest
    public void crashAfterDisarmingAndDuringMinorImpactCooldownStillExplodes(GameTestHelper helper) {
        var drone=helper.spawn(ModEntities.DRONE,3,71,3);
        for(int x=-1;x<=7;x++) for(int z=-1;z<=7;z++) helper.setBlock(x,70,z,Blocks.STONE);
        try (var effects=new CrashEffectsProbe()) {
            drone.setControlInput(DroneInput.IDLE);
            drone.setDeltaMovement(new Vec3(0,-.20,0));
            drone.tick(); // Small contact starts the damage cooldown.
            helper.assertFalse(drone.isRemoved(),"Small impact should not destroy a healthy drone");
            drone.stopControl();
            drone.setPos(drone.getX(),drone.getY()+.65,drone.getZ());
            drone.setDeltaMovement(new Vec3(0,-.8,0));
            drone.tick();
            helper.assertTrue(!drone.isFlying() && drone.isDestroyed() && effects.explosions==1,
                    "Disarmed drone must explode on hard impact even during the minor-impact cooldown");
            helper.succeed();
        } finally { drone.discard(); }
    }

    @GameTest
    public void softTrainingLandingAndFastGrazingContactDoNotExplode(GameTestHelper helper) {
        for(int x=-1;x<=7;x++) for(int z=-1;z<=7;z++) helper.setBlock(x,70,z,Blocks.STONE);
        for(boolean training:new boolean[]{false,true}) {
            var drone=helper.spawn(ModEntities.DRONE,3,71,3);
            try(var effects=new CrashEffectsProbe()) {
                drone.setTraining(training);
                drone.setYRot(-90);
                drone.setDeltaMovement(training?new Vec3(0,-.04,0):new Vec3(.8,-.04,0));
                drone.tick();
                helper.assertFalse(drone.isRemoved(),"Soft normal contact must survive even with fast tangential motion");
                helper.assertTrue(effects.explosions==0 && effects.blastSounds==0,"Soft contact must not emit a blast");
            } finally { drone.discard(); }
        }
        helper.succeed();
    }

    @GameTest
    public void fastFrontalImpactNearEndOfTickMustExplode(GameTestHelper helper) {
        var drone=helper.spawn(ModEntities.DRONE,3,70,3);
        for(int y=69;y<=72;y++) for(int z=-1;z<=7;z++) helper.setBlock(7,y,z,Blocks.STONE);
        try {
            double wall=helper.absolutePos(new BlockPos(7,70,3)).getX();
            drone.setPos(wall-drone.getBbWidth()/2.0-.65,drone.getY(),drone.getZ());
            drone.setYRot(-90);
            drone.setDeltaMovement(new Vec3(.8,0,0));
            drone.tick();
            helper.assertTrue(drone.isDestroyed() && drone.isRemoved(),
                    "A fast wall impact must explode even after travelling most of this tick's distance");
            helper.succeed();
        } finally { drone.discard(); }
    }

    @GameTest
    public void fastVerticalImpactNearEndOfTickMustExplode(GameTestHelper helper) {
        var drone=helper.spawn(ModEntities.DRONE,3,71,3);
        for(int x=-1;x<=7;x++) for(int z=-1;z<=7;z++) helper.setBlock(x,70,z,Blocks.STONE);
        try {
            drone.setPos(drone.getX(),drone.getY()+.65,drone.getZ());
            drone.setDeltaMovement(new Vec3(0,-.8,0));
            drone.tick();
            helper.assertTrue(drone.isDestroyed(),"Fast fall must explode independently of remaining tick distance");
            helper.succeed();
        } finally { drone.discard(); }
    }

    @GameTest
    public void fastImpactWithLivingEntityMustExplode(GameTestHelper helper) {
        var target=helper.spawn(net.minecraft.world.entity.EntityTypes.COW,7,70,3);
        var drone=helper.spawn(ModEntities.DRONE,3,70,3);
        try {
            target.setNoAi(true);
            target.setNoGravity(true);
            drone.setPos(target.getBoundingBox().minX-drone.getBbWidth()/2.0-.65,drone.getY(),target.getZ());
            drone.setYRot(-90);
            drone.setDeltaMovement(new Vec3(.8,0,0));
            drone.tick();
            helper.assertTrue(drone.isDestroyed(),"Living entities must participate in swept drone collisions");
            helper.succeed();
        } finally { drone.discard();target.discard(); }
    }

    @GameTest
    public void destructionIsIdempotentAndDefaultExplosionPreservesBlocks(GameTestHelper helper) {
        var drone=helper.spawn(ModEntities.DRONE,3,40,3);
        var block=helper.absolutePos(new BlockPos(3,39,3));
        helper.getLevel().setBlockAndUpdate(block,Blocks.GLASS.defaultBlockState());
        try {
            helper.assertFalse(DroneConfig.DEFAULTS.destroyBlocks(),"Default must preserve blocks");
            helper.assertTrue(drone.destroy(),"First destruction must be accepted");
            helper.assertTrue(drone.isDestroyed() && drone.isRemoved(),"Server must mark and remove wreck");
            helper.assertFalse(drone.destroy(),"Duplicate destruction must not repeat effects");
            helper.assertFalse(drone.hurtServer(helper.getLevel(),drone.damageSources().generic(),100),
                    "Late damage must not repeat explosion");
            helper.assertTrue(helper.getLevel().getBlockState(block).is(Blocks.GLASS),
                    "Default explosion must preserve fragile nearby blocks");
            helper.assertTrue(new DroneConfig.Settings(true,Float.NaN).sanitized().explosionPower()==8,
                    "Non-finite power must use default");
            helper.succeed();
        } finally { drone.discard(); helper.getLevel().setBlockAndUpdate(block,Blocks.AIR.defaultBlockState()); }
    }

    @GameTest
    public void hardLandingDestroysButGentleContactSurvives(GameTestHelper helper) {
        var hard=helper.spawn(ModEntities.DRONE,3,41,3);
        io.github.riveyder.shahed.entity.DroneEntity soft=null;
        for(int x=-1;x<=7;x++) for(int z=-1;z<=7;z++) helper.setBlock(x,40,z,Blocks.STONE);
        try {
            // Bottom is one block above the floor; use a short fall with a fast impact.
            hard.setDeltaMovement(new Vec3(0,-.8,0));
            for(int i=0;i<3 && !hard.isRemoved();i++) hard.tick();
            helper.assertTrue(hard.isDestroyed(),"High impact must destroy the drone");
            soft=helper.spawn(ModEntities.DRONE,3,41,3);
            soft.setDeltaMovement(new Vec3(0,-.04,0));
            soft.tick();
            helper.assertFalse(soft.isRemoved(),"Gentle ground contact must survive");
            helper.succeed();
        } finally { hard.discard();if(soft!=null) soft.discard(); }
    }
}
