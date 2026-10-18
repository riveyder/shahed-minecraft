package io.github.riveyder.shahed.test;

import io.github.riveyder.shahed.ShahedMod;
import io.github.riveyder.shahed.client.*;
import io.github.riveyder.shahed.control.DroneSessions;
import io.github.riveyder.shahed.control.DroneConfig;
import io.github.riveyder.shahed.network.SettingsRequestPayload;
import net.minecraft.server.players.NameAndId;
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
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;

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
        AtomicReference<DroneConfig.Settings> savedPolicy=new AtomicReference<>();
        try {
            connection.waitForChunksRender();
            serverContext.runOnServer(server->{
                savedPolicy.set(DroneConfig.get());
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
            context.waitFor(client->ServerSettingsClient.state()!=null);
            context.runOnClient(client->client.gui.setScreen(new FpvSettingsScreen(null,FpvController.settings())));
            context.waitTicks(12);
            ShahedMod.LOGGER.info("Settings screenshot: {}",context.takeScreenshot(label+"-shahed-settings"));
            context.clickScreenButton("Камера / HUD");
            context.clickScreenButton("HUD: FULL");
            context.clickScreenButton("Цвет: Зелёный");
            context.runOnClient(client->client.gui.screen().onClose());
            context.waitTicks(3);
            ShahedMod.LOGGER.info("Minimal white HUD: {}",context.takeScreenshot(label+"-shahed-hud-minimal"));
            context.runOnClient(client->{
                if(!FpvController.settings().minimalHud || FpvController.settings().hudColor!=1)
                    throw new AssertionError("Live menu did not change HUD preferences");
                FpvController.settings().minimalHud=false;FpvController.settings().hudColor=0;
            });
            context.waitTicks(12);
            context.runOnClient(client->ServerSettingsClient.request(1,DroneConfig.DEFAULTS.preset(2),SettingsRequestPayload.NONE));
            context.waitFor(client->ServerSettingsClient.state().status().contains("администратора"));
            serverContext.runOnServer(server->{
                if(!DroneConfig.get().equals(savedPolicy.get())) throw new AssertionError("Non-admin packet changed server policy");
                server.getPlayerList().op(new NameAndId(server.getPlayerList().getPlayers().getFirst().getGameProfile()));
            });
            context.waitTicks(12);
            context.runOnClient(client->{client.gui.setScreen(new FpvSettingsScreen(null,FpvController.settings()));});
            context.waitFor(client->ServerSettingsClient.state().admin());
            context.clickScreenButton("Урон сущностям [сервер]: Вкл");
            context.waitTicks(12);
            context.clickScreenButton("Применить серверные");
            context.waitFor(client->!ServerSettingsClient.state().settings().entityDamage());
            serverContext.runOnServer(server->{if(DroneConfig.get().entityDamage()) throw new AssertionError("Menu change did not reach authority");});
            context.runOnClient(client->{client.options.guiScale().set(1);client.resizeGui();});
            context.waitTicks(2);
            ShahedMod.LOGGER.info("Admin settings scale 1: {}",context.takeScreenshot(label+"-shahed-admin-settings"));
            context.clickScreenButton("Урон сущностям [сервер]: Выкл");
            context.waitTicks(12);
            context.clickScreenButton("Применить серверные");
            context.waitFor(client->ServerSettingsClient.state().settings().entityDamage());
            context.runOnClient(client->{client.gui.screen().onClose();client.options.guiScale().set(2);client.resizeGui();});
            context.runOnClient(client->FpvController.settings().nightVision=true);
            context.waitTicks(2);
            ShahedMod.LOGGER.info("Night vision screenshot: {}",context.takeScreenshot(label+"-shahed-night-vision"));
            context.runOnClient(client->FpvController.settings().nightVision=false);
            CrashEffectsProbe.CLIENT_PACKETS.clear();
            CrashEffectsProbe.captureClient=true;
            serverContext.runOnServer(server->{
                var p=server.getPlayerList().getPlayers().getFirst();
                var drone=(DroneEntity)p.level().getEntity(droneId.get());
                var wall=BlockPos.containing(drone.getX()+4,drone.getY(),drone.getZ());
                for(int dy=0;dy<4;dy++) for(int dz=-4;dz<=4;dz++)
                    p.level().setBlockAndUpdate(wall.offset(0,dy,dz),Blocks.GLASS.defaultBlockState());
                drone.setPos(wall.getX()-drone.getBbWidth()/2.0-.65,drone.getY(),drone.getZ());
                drone.setYRot(-90);
                drone.setDeltaMovement(new Vec3(.8,0,0));
                drone.tick();
                if(!drone.isDestroyed() || !drone.isRemoved()) throw new AssertionError("Real wall collision did not destroy drone");
                if(!p.level().getBlockState(wall).is(Blocks.GLASS)) throw new AssertionError("Safe explosion broke wall");
            });
            context.waitForScreen(SignalLostScreen.class);
            context.runOnClient(client->{if(!ReplayController.available()) throw new AssertionError("Server replay did not arrive");});
            context.waitFor(client->CrashEffectsProbe.CLIENT_PACKETS.stream().anyMatch(p->p instanceof ClientboundExplodePacket)
                    && CrashEffectsProbe.CLIENT_PACKETS.stream().anyMatch(p->p instanceof ClientboundSoundPacket sound
                        && sound.getSound().value()==ModSounds.BLAST && sound.getVolume()>0)
                    && CrashEffectsProbe.CLIENT_PACKETS.stream().anyMatch(p->p instanceof ClientboundLevelParticlesPacket particles
                        && particles.getParticle()==ParticleTypes.LARGE_SMOKE && particles.getCount()>=20));
            context.runOnClient(client->{
                long explosions=CrashEffectsProbe.CLIENT_PACKETS.stream().filter(p->p instanceof ClientboundExplodePacket).count();
                if(explosions!=1) throw new AssertionError("Expected one handled vanilla explosion packet, got "+explosions);
                if(client.getSoundManager().getSoundEvent(ShahedMod.id("blast"))==null)
                    throw new AssertionError("Blast sound resource missing");
                client.gui.setScreen(null);
                var explosion=(ClientboundExplodePacket)CrashEffectsProbe.CLIENT_PACKETS.stream()
                        .filter(p->p instanceof ClientboundExplodePacket).findFirst().orElseThrow();
                Vec3 look=explosion.center().subtract(client.player.getEyePosition());
                client.player.setYRot((float)Math.toDegrees(Math.atan2(-look.x,look.z)));
                client.player.setXRot((float)-Math.toDegrees(Math.atan2(look.y,look.horizontalDistance())));
                client.player.yRotO=client.player.getYRot();
                client.player.xRotO=client.player.getXRot();
            });
            // Packets queue particles for the next tick; a screenshot in the same tick can be empty.
            context.waitTicks(2);
            context.runOnClient(client->{
                if(CrashEffectsProbe.CLIENT_PACKETS.stream().noneMatch(p->p instanceof ClientboundLevelParticlesPacket particles
                        && particles.getParticle()==ParticleTypes.EXPLOSION_EMITTER))
                    throw new AssertionError("Independent explosion burst did not reach client");
                ShahedMod.LOGGER.info("Crash client verified: one vanilla explosion, blast sound, emitter, smoke; particles={}",
                        client.particleEngine.countParticles());
            });
            ShahedMod.LOGGER.info("Crash screenshot: {}",context.takeScreenshot(label+"-shahed-crash"));
            context.runOnClient(client->client.gui.setScreen(new SignalLostScreen()));
            ShahedMod.LOGGER.info("Signal screenshot: {}",context.takeScreenshot(label+"-shahed-signal"));
            context.clickScreenButton("REPLAY 0.25x");
            context.waitFor(client->ReplayController.isPlaying());
            context.waitTicks(12);
            ShahedMod.LOGGER.info("Replay screenshot: {}",context.takeScreenshot(label+"-shahed-replay"));
            // A complete 200-frame recording at quarter speed takes 40 seconds before impact.
            context.waitFor(client->ReplayController.isAftermath(),1000);
            context.waitTicks(35);
            context.runOnClient(client->{
                if(!ReplayController.isPlaying() || client.getCameraEntity()==client.player)
                    throw new AssertionError("Aftermath camera returned before three seconds");
                if(CrashEffectsProbe.CLIENT_PACKETS.stream().filter(p->p instanceof ClientboundExplodePacket).count()!=1)
                    throw new AssertionError("Replay repeated a server explosion");
            });
            ShahedMod.LOGGER.info("Aftermath screenshot: {}",context.takeScreenshot(label+"-shahed-aftermath"));
            context.runOnClient(client->FpvController.exit(true));
            context.waitFor(client->!FpvController.isActive() && client.getCameraEntity()==client.player);
            serverContext.waitFor(server->!DroneSessions.isControlling(server.getPlayerList().getPlayers().getFirst()));
        } finally {
            serverContext.runOnServer(server->{if(savedPolicy.get()!=null) DroneConfig.update(savedPolicy.get());});
            CrashEffectsProbe.captureClient=false;
            CrashEffectsProbe.CLIENT_PACKETS.clear();
            context.runOnClient(client->{FpvController.settings().nightVision=false;FpvController.exit(false);});
        }
    }
}
