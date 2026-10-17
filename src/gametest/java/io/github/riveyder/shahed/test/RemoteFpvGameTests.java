package io.github.riveyder.shahed.test;

import io.github.riveyder.shahed.control.DroneChunkTracking;
import io.github.riveyder.shahed.control.DroneSessions;
import io.github.riveyder.shahed.entity.DroneEntity;
import io.github.riveyder.shahed.item.ControllerItem;
import io.github.riveyder.shahed.mixin.ChunkMapFpvAccess;
import io.github.riveyder.shahed.registry.ModEntities;
import io.github.riveyder.shahed.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.phys.Vec3;

/** Actual server sessions and vanilla ticket storage; these do not replace a two-client multiplayer test. */
public final class RemoteFpvGameTests {
    @GameTest
    public void distanceAndSilenceKeepSessionAndViewingOriginSeparateFromBody(GameTestHelper helper) {
        ServerPlayer pilot = pilot(helper, 2, 2);
        ServerPlayer observer = pilot(helper, 4, 2);
        DroneEntity drone = helper.spawn(ModEntities.DRONE, 2, 3, 4);
        drone.setOwnerUuid(pilot.getUUID());
        ItemStack controller = equip(pilot, drone);
        Vec3 body = pilot.position();
        try {
            helper.assertTrue(DroneSessions.connect(pilot, drone, controller), "The owner must connect");
            drone.setPos(body.x + 4096, body.y + 50, body.z - 4096);
            for (int i = 0; i < 200; i++) DroneSessions.tick(helper.getLevel().getServer());
            helper.assertTrue(DroneSessions.isControlling(pilot),
                    "Distance and ten seconds of missing controls must not end a valid session");
            helper.assertTrue(DroneSessions.trackingPosition(pilot).equals(drone.position()),
                    "Entity tracking must use the distant drone instead of the physical pilot");
            helper.assertTrue(DroneSessions.trackingChunk(pilot).equals(drone.chunkPosition()),
                    "The streamed terrain must be centered on the distant drone");
            helper.assertTrue(pilot.position().equals(body), "Remote streaming must never teleport the body");
            helper.assertTrue(DroneSessions.trackingPosition(observer).equals(observer.position()),
                    "An unrelated player's tracking origin must remain their own body");
            DroneSessions.end(pilot, "exit");
            helper.assertTrue(DroneSessions.trackingPosition(pilot).equals(body),
                    "Exit must restore the body's entity viewing origin");
            helper.assertTrue(DroneSessions.trackingChunk(pilot).equals(pilot.chunkPosition()),
                    "Exit must restore the body's terrain center");
            // A still-loaded drone can also be reconnected remotely, without another distance gate.
            helper.assertTrue(DroneSessions.connect(pilot, drone, controller),
                    "An existing owned drone must remain connectable beyond the former range");
            helper.succeed();
        } finally {
            DroneSessions.end(pilot, "test_cleanup");
            drone.discard();
        }
    }

    @GameTest
    public void remoteTicketsFollowCameraShareLeasesAndCleanUpWithoutPersistence(GameTestHelper helper) {
        ServerPlayer pilot = pilot(helper, 2, 2);
        ClientInformation info = pilot.clientInformation();
        pilot.updateOptions(new ClientInformation(info.language(), 2, info.chatVisibility(), info.chatColors(),
                info.modelCustomisation(), info.mainHand(), info.textFilteringEnabled(), info.allowsListing(),
                info.particleStatus()));
        TicketStorage storage = ((ChunkMapFpvAccess) helper.getLevel().getChunkSource().chunkMap).shahed$ticketStorage();
        BlockPos position = helper.absolutePos(new BlockPos(2, 2, 2));
        ChunkPos original = new ChunkPos(position.getX() >> 4, position.getZ() >> 4);
        ChunkPos next = new ChunkPos(original.x() + 2, original.z() - 2);
        DroneChunkTracking first = new DroneChunkTracking(helper.getLevel());
        DroneChunkTracking second = new DroneChunkTracking(helper.getLevel());
        try {
            first.update(pilot, original);
            second.update(pilot, original);
            helper.assertTrue(hasTicket(storage, original, DroneChunkTracking.LOADING),
                    "A remote terrain loading ticket must exist");
            helper.assertTrue(hasTicket(storage, original, DroneChunkTracking.SIMULATION),
                    "A separate remote simulation ticket must exist");
            helper.assertFalse(DroneChunkTracking.LOADING.persist() || DroneChunkTracking.SIMULATION.persist(),
                    "FPV tickets must never force chunks after a server restart");
            helper.assertTrue(DroneChunkTracking.LOADING.hasTimeout() && DroneChunkTracking.SIMULATION.hasTimeout(),
                    "Abandoned leases must expire as an additional cleanup safeguard");
            first.close();
            helper.assertTrue(hasTicket(storage, original, DroneChunkTracking.LOADING)
                            && hasTicket(storage, original, DroneChunkTracking.SIMULATION),
                    "Exiting one of two pilots at the same center must retain the other's tickets");
            second.update(pilot, next);
            helper.assertFalse(hasTicket(storage, original, DroneChunkTracking.LOADING)
                            || hasTicket(storage, original, DroneChunkTracking.SIMULATION),
                    "Moving the last camera away must release both old-center tickets");
            helper.assertTrue(hasTicket(storage, next, DroneChunkTracking.LOADING)
                            && hasTicket(storage, next, DroneChunkTracking.SIMULATION),
                    "Both loading and simulation must follow the new camera center");
            second.close();
            helper.assertFalse(hasTicket(storage, next, DroneChunkTracking.LOADING)
                            || hasTicket(storage, next, DroneChunkTracking.SIMULATION),
                    "The final exit must release all remote tickets immediately");
            helper.succeed();
        } finally {
            first.close();
            second.close();
        }
    }

    private static boolean hasTicket(TicketStorage storage, ChunkPos center, TicketType type) {
        var tickets = storage.getTickets(center.pack());
        return tickets != null && tickets.stream().anyMatch(ticket -> ticket.getType() == type);
    }

    private static ServerPlayer pilot(GameTestHelper helper, int x, int z) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
        player.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(x, 2, z))));
        return player;
    }

    private static ItemStack equip(ServerPlayer player, DroneEntity drone) {
        ItemStack controller = new ItemStack(ModItems.CONTROLLER);
        ControllerItem.bind(player, drone, controller);
        player.setItemSlot(EquipmentSlot.MAINHAND, controller);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.FPV_GOGGLES));
        return controller;
    }
}
