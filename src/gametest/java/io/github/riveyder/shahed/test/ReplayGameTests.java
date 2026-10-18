package io.github.riveyder.shahed.test;

import io.github.riveyder.shahed.replay.*;
import io.github.riveyder.shahed.network.*;
import io.github.riveyder.shahed.control.DroneSessions;
import io.github.riveyder.shahed.registry.ModEntities;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

public final class ReplayGameTests {
    @GameTest
    public void crashSitePresetTracksImpactChunksBeforePlaybackReachesImpact(GameTestHelper helper) {
        var pilot=(ServerPlayer)helper.makeMockServerPlayer(GameType.CREATIVE);
        var drone=helper.spawn(ModEntities.DRONE,3,80,3);
        var session=UUID.randomUUID();
        try {
            drone.setTraining(true);drone.setControlInput(io.github.riveyder.shahed.control.DroneInput.IDLE);
            drone.tick();drone.setPos(drone.getX()+12,drone.getY(),drone.getZ());drone.destroy();
            DroneReplays.capture(pilot,session,drone);
            helper.assertFalse(DroneReplays.request(pilot,new ReplayControlPayload(session,1,3)),"Reject invalid camera preset");
            helper.assertTrue(DroneReplays.request(pilot,new ReplayControlPayload(session,1,2)),"Crash-site camera must start");
            helper.assertTrue(DroneReplays.position(pilot).distanceToSqr(drone.replayImpact().position())<.001,
                    "Server must stream impact chunks immediately for stationary crash-site camera");
            var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),helper.getLevel().registryAccess());
            try {var payload=new ReplayControlPayload(session,2,2);ReplayControlPayload.CODEC.encode(buffer,payload);
                helper.assertTrue(payload.equals(ReplayControlPayload.CODEC.decode(buffer)),"Camera preset must survive protocol");}
            finally {buffer.release();}
        } finally {DroneReplays.stop(pilot,false);drone.discard();}
        helper.succeed();
    }
    @GameTest
    public void aftermathLastsSixtyTicksAtBothPlaybackSpeeds(GameTestHelper helper) {
        for(int mode:new int[]{1,2}) {
            double impact=199;
            helper.assertFalse(ReplayTimeline.finished(impact+59*ReplayTimeline.rate(mode),200,mode),"Aftermath must retain camera for 59 ticks");
            helper.assertTrue(ReplayTimeline.finished(impact+60*ReplayTimeline.rate(mode),200,mode),"Aftermath must end after 60 ticks at either speed");
        }
        helper.succeed();
    }
    @GameTest(maxTicks=100)
    public void serverKeepsReplayLeaseAfterDestroyedDroneIsGone(GameTestHelper helper) {
        var pilot=(ServerPlayer)helper.makeMockServerPlayer(GameType.CREATIVE);
        var drone=helper.spawn(ModEntities.DRONE,3,80,3);
        drone.setTraining(true);drone.destroy();
        var session=UUID.randomUUID();
        DroneReplays.capture(pilot,session,drone);
        helper.assertTrue(DroneReplays.request(pilot,new ReplayControlPayload(session,1)),"Replay must start after entity removal");
        helper.runAtTickTime(40,()->helper.assertTrue(DroneReplays.isViewing(pilot),"Aftermath lease must outlive the flight frames"));
        helper.runAtTickTime(65,()->{
            helper.assertFalse(DroneReplays.isViewing(pilot),"Finished aftermath must release tracking/body");
            helper.assertTrue(drone.isRemoved(),"Replay must not resurrect the drone");helper.succeed();
        });
    }
    @GameTest
    public void ringBufferKeepsLastTenSecondsAndWrapsYaw(GameTestHelper helper) {
        var buffer=new ReplayBuffer();
        for(int i=0;i<300;i++) buffer.add(new ReplayFrame(i,40,3,179,0,0));
        var snapshot=buffer.snapshot();
        helper.assertTrue(snapshot.size()==200 && snapshot.getFirst().x()==100 && snapshot.getLast().x()==299,
                "History must contain only the newest 200 samples in chronological order");
        buffer.add(new ReplayFrame(300,40,3,-179,0,0));
        helper.assertTrue(snapshot.getFirst().x()==100,"Snapshot must be immutable and independent");
        var middle=snapshot.getLast().blend(buffer.snapshot().getLast(),.5F);
        helper.assertTrue(Math.abs(middle.yaw()-180)<.001,"Interpolation must cross yaw boundary by the short arc");
        helper.succeed();
    }
    @GameTest
    public void replayPacketRoundTripAndOversizeRejection(GameTestHelper helper) {
        var ring=new ReplayBuffer();
        for(int i=0;i<200;i++) ring.add(new ReplayFrame(i+.125,-20,500,179,-20,25));
        var packet=new ReplayPayload(UUID.randomUUID(),ring.snapshot(),1273,15.8,-20.5,2,
                new ReplayImpact(new net.minecraft.world.phys.Vec3(200,-20,500),true,true,true,8));
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),helper.getLevel().registryAccess());
        try {
            ReplayPayload.CODEC.encode(buffer,packet);
            helper.assertTrue(buffer.readableBytes()<8192,"Replay transfer must stay below 8 KiB");
            helper.assertTrue(packet.equals(ReplayPayload.CODEC.decode(buffer)),"All samples and session must round trip");
            helper.assertFalse(buffer.isReadable(),"Codec must consume exactly its payload");
            buffer.clear();buffer.writeUUID(UUID.randomUUID());buffer.writeVarInt(201);
            boolean rejected=false;
            try {ReplayPayload.CODEC.decode(buffer);} catch(IllegalArgumentException exception) {rejected=true;}
            helper.assertTrue(rejected,"Oversize must be rejected before allocating frame data");
            helper.succeed();
        } finally {buffer.release();}
    }
    @GameTest
    public void onlyRecordingOwnerCanViewAndPlaybackCannotRepeatDestruction(GameTestHelper helper) {
        var pilot=(ServerPlayer)helper.makeMockServerPlayer(GameType.CREATIVE);
        var foreign=(ServerPlayer)helper.makeMockServerPlayer(GameType.CREATIVE);
        var drone=helper.spawn(ModEntities.DRONE,3,40,3);
        UUID session=UUID.randomUUID();
        try {
            drone.destroy();
            // Rebuild a fragile block after the original explosion, then repeatedly view history.
            helper.setBlock(3,40,3,Blocks.GLASS);
            DroneReplays.capture(pilot,session,drone);
            helper.assertFalse(DroneReplays.request(foreign,new ReplayControlPayload(session,1)),"Foreign viewer must be rejected");
            helper.assertFalse(DroneReplays.request(pilot,new ReplayControlPayload(UUID.randomUUID(),1)),"Stale identity must be rejected");
            helper.assertFalse(DroneReplays.request(pilot,new ReplayControlPayload(session,99)),"Invalid mode must be rejected");
            helper.assertTrue(DroneReplays.request(pilot,new ReplayControlPayload(session,2)),"Owner may view in slow motion");
            helper.assertTrue(DroneSessions.isControlling(pilot),"Replay must anchor the body");
            helper.assertFalse(DroneSessions.hasFlightSession(pilot),"Replay must never grant flight input authority");
            helper.assertTrue(DroneSessions.trackingPosition(pilot).equals(drone.replayImpact().position()),
                    "A single terminal frame must track the crash site during aftermath, not the body");
            helper.assertFalse(DroneReplays.request(pilot,new ReplayControlPayload(session,1)),"Duplicate start must not allocate another viewer");
            DroneReplays.stop(pilot,false);
            helper.assertFalse(DroneSessions.isControlling(pilot),"Exit must release body and viewer");
            helper.assertTrue(DroneSessions.trackingPosition(pilot).equals(pilot.position()),"Exit must restore body tracking");
            helper.assertBlockPresent(Blocks.GLASS,3,40,3);
            helper.assertTrue(drone.isRemoved(),"Playback must not resurrect an authoritative drone");
            helper.succeed();
        } finally {DroneReplays.stop(pilot,false);drone.discard();}
    }
}
