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
        var packet=new ReplayPayload(UUID.randomUUID(),ring.snapshot(),1273,15.8,-20.5,2);
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
            helper.assertTrue(DroneSessions.trackingPosition(pilot).equals(drone.replayFrames().getFirst().position()),
                    "Remote chunk origin must follow recorded camera, not body");
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
