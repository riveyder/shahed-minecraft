package io.github.riveyder.shahed.control;

import io.github.riveyder.shahed.entity.DroneEntity;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/** Session-only time trials. Route editing is restricted by the parent operator command. */
public final class DroneRacing {
    private static final Map<ServerLevel,List<Vec3>> COURSES=new HashMap<>();
    private static final Map<UUID,Entry> RUNS=new HashMap<>();
    private record Entry(ServerPlayer player,DroneEntity drone,RaceRun run) { }
    public static int add(ServerPlayer player) {
        var course=COURSES.computeIfAbsent(player.level(),key->new ArrayList<>());
        if(course.size()>=32) return 0;
        course.add(player.position());
        return course.size();
    }
    public static void clear(ServerLevel level) {
        COURSES.remove(level);
        RUNS.entrySet().removeIf(e->e.getValue().drone.level()==level);
    }
    public static boolean start(ServerPlayer player) {
        var course=COURSES.get(player.level());
        var id=DroneSessions.controlledDrone(player);
        if(course==null||course.size()<2||id.isEmpty()||RUNS.containsKey(player.getUUID())
                ||!(player.level().getEntity(id.get()) instanceof DroneEntity drone)
                ||!drone.getOwnerUuid().filter(player.getUUID()::equals).isPresent()) return false;
        RUNS.put(player.getUUID(),new Entry(player,drone,new RaceRun(course,drone.position())));
        return true;
    }
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server->{
            for(var e:new ArrayList<>(RUNS.values())) {
                if(e.player.level().getServer()!=server) continue;
                if(e.drone.isRemoved()||!DroneSessions.controlledDrone(e.player).filter(e.drone.getUUID()::equals).isPresent()) {
                    RUNS.remove(e.player.getUUID());continue;
                }
                e.run.tick(e.drone.position());
                if(e.player.connection!=null && (e.run.ticks()%10==0||e.run.finished()))
                    e.player.sendOverlayMessage(Component.literal(e.run.hud()));
                if(e.run.finished()) RUNS.remove(e.player.getUUID());
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->RUNS.remove(handler.player.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPING.register(server->{
            COURSES.keySet().removeIf(l->l.getServer()==server);
            RUNS.entrySet().removeIf(e->e.getValue().player.level().getServer()==server);
        });
    }
    private DroneRacing() { }
}
