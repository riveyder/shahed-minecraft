package io.github.riveyder.shahed.test;

import io.github.riveyder.shahed.ShahedMod;
import io.github.riveyder.shahed.client.*;
import io.github.riveyder.shahed.control.DroneSessions;
import io.github.riveyder.shahed.entity.DroneEntity;
import io.github.riveyder.shahed.item.ControllerItem;
import io.github.riveyder.shahed.registry.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Opt-in real graphics integration; screenshots supplement, not replace, manual playtesting. */
public final class ClientSmokeTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try(var world=context.worldBuilder().create()) {
            exercise(context,world.getServer(),world.getConnection(),"integrated");
        }
        if(Boolean.getBoolean("shahed.test.dedicated")) try(var server=context.worldBuilder().createServer()) {
            try(var connection=server.connect()) { exercise(context,server,connection,"dedicated"); }
        }
    }

    private void exercise(ClientGameTestContext context,TestServerContext serverContext,TestServerConnection connection,String label) {
        AtomicReference<UUID> droneId=new AtomicReference<>();
        try {
            connection.waitForChunksRender();
            serverContext.runOnServer(server->{
                var player=server.getPlayerList().getPlayers().getFirst();
                player.setGameMode(GameType.CREATIVE);
                int x=player.blockPosition().getX(),z=player.blockPosition().getZ();
                for(int dx=-12;dx<=12;dx++) for(int dz=-12;dz<=12;dz++)
                    player.level().setBlockAndUpdate(new BlockPos(x+dx,99,z+dz),Blocks.STONE.defaultBlockState());
                player.connection.teleport(x+8,100,z+8,135,10);
                var drone=new DroneEntity(ModEntities.DRONE,player.level());
                drone.setPos(x+.5,100,z+.5);drone.setOwnerUuid(player.getUUID());
                player.level().addFreshEntity(drone);droneId.set(drone.getUUID());
                var controller=new ItemStack(ModItems.CONTROLLER);
                ControllerItem.bind(player,drone,controller);
                player.setItemSlot(EquipmentSlot.MAINHAND,controller);
                player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ModItems.FPV_GOGGLES));
            });
            connection.waitForClientboundEntityUpdates(ModEntities.DRONE);
            context.waitTicks(10);
            connection.waitForChunksRender();
            ShahedMod.LOGGER.info("Model screenshot: {}",context.takeScreenshot(label+"-shahed-model"));
            context.runOnClient(client->{
                if(client.getSoundManager().getSoundEvent(ShahedMod.id("engine"))==null)
                    throw new AssertionError("Engine sound resource did not load");
            });
            serverContext.runOnServer(server->{
                var p=server.getPlayerList().getPlayers().getFirst();
                var drone=(DroneEntity)p.level().getEntity(droneId.get());
                if(!DroneSessions.connect(p,drone,p.getMainHandItem())) throw new AssertionError("FPV connection failed");
            });
            context.waitFor(client->FpvController.isActive());
            context.waitTicks(12);
            ShahedMod.LOGGER.info("FPV screenshot: {}",context.takeScreenshot(label+"-shahed-fpv"));
            context.runOnClient(client->FpvController.settings().nightVision=true);
            context.waitTicks(2);
            ShahedMod.LOGGER.info("Night vision screenshot: {}",context.takeScreenshot(label+"-shahed-night-vision"));
            context.runOnClient(client->FpvController.settings().nightVision=false);
            serverContext.runOnServer(server->{
                var p=server.getPlayerList().getPlayers().getFirst();
                var drone=(DroneEntity)p.level().getEntity(droneId.get());
                if(!drone.destroy()) throw new AssertionError("Destruction was not applied");
            });
            context.waitForScreen(SignalLostScreen.class);
            context.runOnClient(client->{if(!ReplayController.available()) throw new AssertionError("Server replay did not arrive");});
            ShahedMod.LOGGER.info("Signal screenshot: {}",context.takeScreenshot(label+"-shahed-signal"));
            context.clickScreenButton("REPLAY 0.25x");
            context.waitFor(client->ReplayController.isPlaying());
            context.waitTicks(12);
            ShahedMod.LOGGER.info("Replay screenshot: {}",context.takeScreenshot(label+"-shahed-replay"));
            context.runOnClient(client->FpvController.exit(true));
            context.waitFor(client->!FpvController.isActive() && client.getCameraEntity()==client.player);
            serverContext.waitFor(server->!DroneSessions.isControlling(server.getPlayerList().getPlayers().getFirst()));
        } finally {
            context.runOnClient(client->{FpvController.settings().nightVision=false;FpvController.exit(false);});
        }
    }
}
