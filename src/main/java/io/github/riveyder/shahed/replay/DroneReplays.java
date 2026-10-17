package io.github.riveyder.shahed.replay;

import io.github.riveyder.shahed.control.DroneChunkTracking;
import io.github.riveyder.shahed.control.DroneSessions;
import io.github.riveyder.shahed.entity.DroneEntity;
import io.github.riveyder.shahed.network.ReplayPayload;
import io.github.riveyder.shahed.network.ReplayControlPayload;
import io.github.riveyder.shahed.mixin.ChunkMapFpvAccess;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Bounded per-player replay leases. No replay method applies physics, damage or block changes. */
public final class DroneReplays {
    private static final Map<UUID,Recording> RECORDINGS=new HashMap<>();
    private static final Map<UUID,Viewer> VIEWERS=new HashMap<>();
    public static void register() {
        ServerTickEvents.START_SERVER_TICK.register(server->{
            for(var entry:new ArrayList<>(RECORDINGS.entrySet())) {
                var recording=entry.getValue();
                if(recording.level.getServer()==server && ++recording.age>1200) {
                    var viewer=VIEWERS.get(entry.getKey());
                    if(viewer!=null) stop(viewer.player,true);
                    RECORDINGS.remove(entry.getKey());
                }
            }
            for(var viewer:new ArrayList<>(VIEWERS.values())) {
                if(viewer.recording.level.getServer()!=server) continue;
                var p=viewer.player;
                if(!p.isAlive()||p.isRemoved()||p.level()!=viewer.recording.level
                        ||p.getHealth()<viewer.health||p.position().distanceToSqr(viewer.body)>0.04
                        ||p.isPassenger()||p.isSpectator()||p.isSleeping()) { stop(p,true);continue; }
                viewer.cursor+=viewer.mode==2?.25:1;
                if(viewer.cursor>=viewer.recording.payload.frames().size()) { stop(p,true);continue; }
                p.setDeltaMovement(Vec3.ZERO);
                if(p.connection!=null) {
                    p.connection.resetFlyingTicks();
                    viewer.chunks.update(p,ChunkPos.containing(BlockPos.containing(position(p))));
                    refresh(p);
                }
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{
            stop(handler.player,false);RECORDINGS.remove(handler.player.getUUID());
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server->{
            for(var viewer:new ArrayList<>(VIEWERS.values())) if(viewer.recording.level.getServer()==server) stop(viewer.player,false);
            RECORDINGS.entrySet().removeIf(e->e.getValue().level.getServer()==server);
        });
    }
    public static void capture(ServerPlayer player,UUID sessionId,DroneEntity drone) {
        var payload=new ReplayPayload(sessionId,drone.replayFrames(),drone.getFlightTicks(),drone.maximumSpeed(),drone.maximumAltitude(),drone.getPaint());
        RECORDINGS.put(player.getUUID(),new Recording(player.level(),payload));
        if(player.connection!=null && ServerPlayNetworking.canSend(player,ReplayPayload.TYPE)) ServerPlayNetworking.send(player,payload);
    }
    public static boolean request(ServerPlayer player,ReplayControlPayload request) {
        var recording=RECORDINGS.get(player.getUUID());
        if(recording==null||!recording.payload.sessionId().equals(request.sessionId())||request.mode()<0||request.mode()>2) return false;
        if(request.mode()==0) { stop(player,true);return true; }
        if(DroneSessions.hasFlightSession(player)||!player.isAlive()||player.level()!=recording.level
                ||player.isPassenger()||player.isSleeping()||player.isSpectator()||VIEWERS.containsKey(player.getUUID())) return false;
        if(player.connection!=null && !ServerPlayNetworking.canSend(player,ReplayControlPayload.TYPE)) return false;
        var viewer=new Viewer(player,recording,request.mode());
        VIEWERS.put(player.getUUID(),viewer);
        player.setDeltaMovement(Vec3.ZERO);
        if(player.connection!=null) {
            viewer.chunks.update(player,ChunkPos.containing(BlockPos.containing(position(player))));
            refresh(player);
            ServerPlayNetworking.send(player,request);
        }
        return true;
    }
    public static boolean isViewing(ServerPlayer p) { return VIEWERS.containsKey(p.getUUID()); }
    public static Vec3 position(ServerPlayer p) {
        var v=VIEWERS.get(p.getUUID());
        return v==null?p.position():v.recording.payload.frames().get(Math.min((int)v.cursor,v.recording.payload.frames().size()-1)).position();
    }
    public static void stop(ServerPlayer player,boolean notify) {
        var viewer=VIEWERS.remove(player.getUUID());
        if(viewer==null) return;
        viewer.chunks.close();
        if(player.connection!=null && !player.isRemoved()) {
            if(notify) {
                ServerPlayNetworking.send(player,new ReplayControlPayload(viewer.recording.payload.sessionId(),0));
                refresh(player);
                player.connection.teleport(player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot());
            }
            player.connection.resetFlyingTicks();
        }
    }
    private static void refresh(ServerPlayer p) {
        var chunks=p.level().getChunkSource();
        ((ChunkMapFpvAccess)chunks.chunkMap).shahed$updateChunkTracking(p);chunks.move(p);
    }
    private static final class Recording {
        final ServerLevel level; final ReplayPayload payload; int age;
        Recording(ServerLevel level,ReplayPayload payload) {this.level=level;this.payload=payload;}
    }
    private static final class Viewer {
        final ServerPlayer player; final Recording recording; final int mode;
        final Vec3 body; final float health; final DroneChunkTracking chunks; double cursor;
        Viewer(ServerPlayer p,Recording r,int mode) {
            player=p;recording=r;this.mode=mode;body=p.position();health=p.getHealth();chunks=new DroneChunkTracking(p.level());
        }
    }
    private DroneReplays() { }
}
