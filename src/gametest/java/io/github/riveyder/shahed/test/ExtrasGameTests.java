package io.github.riveyder.shahed.test;

import io.github.riveyder.shahed.control.*;
import io.github.riveyder.shahed.entity.DroneEntity;
import io.github.riveyder.shahed.item.ControllerItem;
import io.github.riveyder.shahed.network.InputPayload;
import io.github.riveyder.shahed.registry.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public final class ExtrasGameTests {
    @GameTest
    public void trainingSurvivesHardImpactAndCannotExplodeNearbyDrone(GameTestHelper helper) {
        var training=helper.spawn(ModEntities.DRONE,3,41,3);
        training.setTraining(true);training.setPaint(2);
        for(int x=-1;x<=7;x++) for(int z=-1;z<=7;z++) helper.setBlock(x,40,z,Blocks.STONE);
        DroneEntity target=null;
        try {
            training.setDeltaMovement(new Vec3(0,-.8,0));
            for(int i=0;i<3;i++) training.tick();
            helper.assertFalse(training.isRemoved(),"Training aircraft must survive hard landing");
            helper.assertTrue(training.getPaint()==2,"Customization must be retained");
            target=helper.spawn(ModEntities.DRONE,3,41,3);
            training.destroy();
            helper.assertFalse(target.isRemoved(),"Training destruction must not create an explosion");
            helper.succeed();
        } finally {training.discard();if(target!=null)target.discard();}
    }
    @GameTest
    public void spectatorRequiresConsentCannotControlAndDoesNotStopPilot(GameTestHelper helper) {
        var pilot=(ServerPlayer)helper.makeMockServerPlayer(GameType.CREATIVE);
        var viewer=(ServerPlayer)helper.makeMockServerPlayer(GameType.CREATIVE);
        var drone=helper.spawn(ModEntities.DRONE,3,40,3);
        drone.setOwnerUuid(pilot.getUUID());
        var controller=new ItemStack(ModItems.CONTROLLER);
        ControllerItem.bind(pilot,drone,controller);
        pilot.setItemSlot(EquipmentSlot.MAINHAND,controller);
        pilot.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ModItems.FPV_GOGGLES));
        viewer.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ModItems.FPV_GOGGLES));
        try {
            helper.assertTrue(DroneSessions.connect(pilot,drone,controller),"Owner must connect");
            helper.assertFalse(DroneSessions.spectate(viewer,drone),"Camera requires explicit owner opt-in");
            drone.allowSpectators(true);
            helper.assertTrue(DroneSessions.spectate(viewer,drone),"Authorized observer must connect");
            helper.assertFalse(DroneSessions.accept(viewer,new InputPayload(DroneSessions.sessionId(viewer).orElseThrow(),
                    drone.getUUID(),1,new DroneInput(1,0,0,1,0,false))),"Observer input must be rejected on server");
            DroneSessions.end(viewer,"exit");
            helper.assertTrue(drone.isFlying() && DroneSessions.hasFlightSession(pilot),"Observer exit must not disarm pilot");
            helper.assertTrue(DroneSessions.spectate(viewer,drone),"Observer may reconnect");
            drone.allowSpectators(false);
            DroneSessions.tick(helper.getLevel().getServer());
            helper.assertFalse(DroneSessions.isControlling(viewer),"Revoked permission must close observer session");
            helper.assertTrue(DroneSessions.isControlling(pilot),"Revocation must preserve owner session");
            helper.succeed();
        } finally {DroneSessions.end(viewer,"cleanup");DroneSessions.end(pilot,"cleanup");drone.discard();}
    }
    @GameTest
    public void raceRequiresOrderedCheckpointsAndSweepsFastMovement(GameTestHelper helper) {
        var race=new RaceRun(List.of(new Vec3(10,40,0),new Vec3(20,40,0)),new Vec3(0,40,0));
        race.tick(new Vec3(20,40,10));
        helper.assertTrue(race.next()==0,"Missing first gate must not count a later gate");
        race.tick(new Vec3(0,40,0));
        race.tick(new Vec3(15,40,0));
        helper.assertTrue(race.next()==1,"A segment crossing the gate counts even when neither endpoint is inside it");
        race.tick(new Vec3(25,40,0));
        helper.assertTrue(race.finished() && race.ticks()==4,"Finish time must use server ticks and ordered gates");
        race.tick(new Vec3(100,40,0));
        helper.assertTrue(race.ticks()==4,"Finished stopwatch must remain fixed");
        helper.succeed();
    }
}
