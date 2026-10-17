package io.github.riveyder.shahed.client;

import io.github.riveyder.shahed.entity.DroneEntity;
import io.github.riveyder.shahed.registry.ModEntities;
import io.github.riveyder.shahed.network.ReplayPayload;
import io.github.riveyder.shahed.network.ReplayControlPayload;
import io.github.riveyder.shahed.replay.ReplayFrame;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Visual-only ghost. Never registered on the server; replay cannot reapply an impact. */
public final class ReplayController {
    private static ReplayPayload recording;
    private static long received;
    private static DroneEntity ghost;
    private static DroneEntity camera;
    private static ClientLevel level;
    private static Entity previousCamera;
    private static CameraType previousPerspective;
    private static double cursor;
    private static int mode;
    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(ReplayPayload.TYPE,(p,c)->c.client().execute(()->{
            if(FpvController.acceptsReplay(p.sessionId())) { recording=p;received=System.nanoTime(); }
        }));
        ClientPlayNetworking.registerGlobalReceiver(ReplayControlPayload.TYPE,(p,c)->c.client().execute(()->{
            if(recording==null||!recording.sessionId().equals(p.sessionId())) return;
            if(p.mode()==0) { boolean wasPlaying=isPlaying();stop(false);
                if(wasPlaying && c.client().player!=null && c.client().player.isAlive()) c.client().gui.setScreen(new SignalLostScreen());
            } else if(p.mode()==1||p.mode()==2) start(p.mode());
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->{stop(false);recording=null;});
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(!isPlaying()) return;
            if(client.level!=level||client.player==null||!client.player.isAlive()) {stop(true);recording=null;return;}
            cursor+=mode==2?.25:1;
            if(cursor>=recording.frames().size()) {stop(true);client.gui.setScreen(new SignalLostScreen());return;}
            ghost.setOldPosAndRot();
            var frame=frame(0);
            ghost.setPos(frame.x(),frame.y(),frame.z());ghost.setYRot(frame.yaw());ghost.setXRot(frame.pitch());
            ghost.setReplayRoll(frame.roll());
            camera.setOldPosAndRot();camera.setPos(frame.x(),frame.y(),frame.z());
        });
    }
    public static String blackBox() {
        if(recording==null) return "";
        return String.format(java.util.Locale.ROOT,"TIME %.1fs | MAX %.1f m/s | ALT %.1f m",
                recording.flightTicks()/20.0,recording.maximumSpeed(),recording.maximumAltitude());
    }
    public static boolean available() {return recording!=null && (System.nanoTime()-received)<60_000_000_000L;}
    public static boolean isPlaying() {return ghost!=null;}
    public static boolean isCamera(Entity entity) { return camera!=null && entity==camera; }
    public static boolean isGhost(Entity entity) {return entity==ghost;}
    public static void request(boolean slow) {
        if(available() && ClientPlayNetworking.canSend(ReplayControlPayload.TYPE))
            ClientPlayNetworking.send(new ReplayControlPayload(recording.sessionId(),slow?2:1));
    }
    private static void start(int speed) {
        var client=Minecraft.getInstance();
        if(client.level==null||client.player==null||!client.player.isAlive()) return;
        stop(false);
        level=client.level;mode=speed;cursor=0;
        previousCamera=client.getCameraEntity();previousPerspective=client.options.getCameraType();
        ghost=new DroneEntity(ModEntities.DRONE,level);
        int id=Integer.MIN_VALUE+100;
        while(level.getEntity(id)!=null) id++;
        ghost.setId(id);
        ghost.setReplayPaint(recording.paint());
        var frame=recording.frames().getFirst();
        ghost.setPos(frame.x(),frame.y(),frame.z());ghost.setYRot(frame.yaw());ghost.setXRot(frame.pitch());
        ghost.setReplayRoll(frame.roll());ghost.setOldPosAndRot();
        level.addEntity(ghost);
        camera=new DroneEntity(ModEntities.DRONE,level);
        camera.setPos(frame.x(),frame.y(),frame.z());camera.setOldPosAndRot();
        client.options.setCameraType(CameraType.FIRST_PERSON);client.setCameraEntity(camera);
        client.gui.setScreen(null);
        FpvController.beginReturnFade();
    }
    public static ReplayFrame frame(float partial) {
        double sample=Math.min(recording.frames().size()-1,cursor+partial*(mode==2?.25:1));
        int index=(int)sample;
        return recording.frames().get(index).blend(recording.frames().get(Math.min(index+1,recording.frames().size()-1)),(float)(sample-index));
    }
    public static Vec3 cameraPosition(float partial) {
        var f=frame(partial);double yaw=Math.toRadians(cameraYaw(partial));
        return f.position().add(Math.sin(yaw)*7,3,-Math.cos(yaw)*7);
    }
    public static float cameraYaw(float partial) {
        var f=frame(partial);
        return f.yaw()+(FpvController.settings().directorCamera?(float)((cursor+partial)*.7):0);
    }
    public static String label() {return (mode==2?"REPLAY 0.25x | ":"REPLAY 1x | ")+FpvKeys.EXIT.getTranslatedKeyMessage().getString()+" EXIT";}
    public static void stop(boolean notify) {
        if(!isPlaying()) return;
        var client=Minecraft.getInstance();
        if(notify && client.getConnection()!=null && ClientPlayNetworking.canSend(ReplayControlPayload.TYPE))
            ClientPlayNetworking.send(new ReplayControlPayload(recording.sessionId(),0));
        level.removeEntity(ghost.getId(),Entity.RemovalReason.DISCARDED);
        Entity restore=previousCamera;
        if(restore==null||restore.isRemoved()||restore.level()!=client.level) restore=client.player;
        client.setCameraEntity(restore);client.options.setCameraType(previousPerspective);
        ghost=null;camera=null;level=null;previousCamera=null;previousPerspective=null;
        net.minecraft.client.KeyMapping.releaseAll();
        FpvController.beginReturnFade();
    }
    private ReplayController() { }
}
