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
import io.github.riveyder.shahed.replay.ReplayTimeline;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;

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
    private static boolean impactShown;
    private static boolean autoPending;
    public static boolean isAftermath() {return isPlaying() && cursor>=recording.frames().size()-1;}
    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(ReplayPayload.TYPE,(p,c)->c.client().execute(()->{
            if(FpvController.acceptsReplay(p.sessionId())) { recording=p;received=System.nanoTime();autoPending=true; }
        }));
        ClientPlayNetworking.registerGlobalReceiver(ReplayControlPayload.TYPE,(p,c)->c.client().execute(()->{
            if(recording==null||!recording.sessionId().equals(p.sessionId())) return;
            if(p.mode()==0) { boolean wasPlaying=isPlaying();stop(false);
                if(wasPlaying && c.client().player!=null && c.client().player.isAlive()) c.client().gui.setScreen(new SignalLostScreen());
            } else if(p.mode()==1||p.mode()==2) start(p.mode());
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->{stop(false);recording=null;});
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(autoPending && client.gui.screen() instanceof SignalLostScreen) {
                autoPending=false;
                if(FpvController.settings().replayAuto && available()) request(FpvController.settings().replaySlow);
            }
            if(!isPlaying()) return;
            if(client.level!=level||client.player==null||!client.player.isAlive()) {stop(true);recording=null;return;}
            cursor+=mode==2?.25:1;
            if(ReplayTimeline.finished(cursor,recording.frames().size(),mode)) {stop(true);client.gui.setScreen(new SignalLostScreen());return;}
            if(isAftermath()) {
                var impact=recording.impact();var p=impact.position();
                if(!impactShown) {
                    impactShown=true;level.removeEntity(ghost.getId(),Entity.RemovalReason.DISCARDED);
                    if(impact.explosion() && impact.particles()) level.addParticle(ParticleTypes.EXPLOSION_EMITTER,p.x,p.y,p.z,0,0,0);
                    if(impact.explosion() && impact.sound()) level.playLocalSound(p.x,p.y,p.z,
                            io.github.riveyder.shahed.registry.ModSounds.BLAST,SoundSource.NEUTRAL,1.5F,1,false);
                }
                if(impact.explosion() && impact.particles() && ReplayTimeline.aftermath(cursor,recording.frames().size(),mode)<45)
                    for(int i=0;i<2;i++) level.addParticle(ParticleTypes.LARGE_SMOKE,p.x+(level.getRandom().nextDouble()-.5)*2,
                            p.y+.2,p.z+(level.getRandom().nextDouble()-.5)*2,0,.03,0);
                camera.setOldPosAndRot();camera.setPos(p.x,p.y,p.z);return;
            }
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
    public static boolean available() {return FpvController.settings().replayEnabled && recording!=null && (System.nanoTime()-received)<60_000_000_000L;}
    public static boolean isPlaying() {return camera!=null;}
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
        level=client.level;mode=speed;cursor=0;impactShown=false;
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
        var f=frame(partial);double after=ReplayTimeline.aftermath(cursor+partial*ReplayTimeline.rate(mode),recording.frames().size(),mode)/60;
        int preset=FpvController.settings().replayCamera;
        double angle=f.yaw()+(preset==1?90:preset==2?135:0)+(isAftermath()?after*25:
                FpvController.settings().directorCamera?(cursor+partial)*.7:0);
        double yaw=Math.toRadians(angle),distance=7+Math.min(1,after)*3;
        return cameraTarget(partial).add(Math.sin(yaw)*distance,3+Math.min(1,after)*2,-Math.cos(yaw)*distance);
    }
    private static Vec3 cameraTarget(float partial) {return isAftermath()?recording.impact().position():frame(partial).position();}
    public static float cameraYaw(float partial) {
        Vec3 look=cameraTarget(partial).subtract(cameraPosition(partial));
        return (float)Math.toDegrees(Math.atan2(-look.x,look.z));
    }
    public static float cameraPitch(float partial) {Vec3 look=cameraTarget(partial).subtract(cameraPosition(partial));
        return (float)-Math.toDegrees(Math.atan2(look.y,look.horizontalDistance()));}
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
