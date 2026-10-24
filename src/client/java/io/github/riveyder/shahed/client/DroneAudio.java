package io.github.riveyder.shahed.client;

import io.github.riveyder.shahed.entity.DroneEntity;
import io.github.riveyder.shahed.registry.ModSounds;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** Entity data is server-authoritative. No client audio state changes gameplay. */
public final class DroneAudio {
    private static final Map<DroneEntity, Loop[]> LOOPS=new HashMap<>();
    private static final java.util.List<Transition> TRANSITIONS=new java.util.ArrayList<>();
    private static void clear(Minecraft client){
        for(var loops:LOOPS.values())for(var sound:loops)client.getSoundManager().stop(sound);
        for(var sound:TRANSITIONS)client.getSoundManager().stop(sound);
        LOOPS.clear();TRANSITIONS.clear();
    }
    private static float distanceGain(DroneEntity drone){
        double d=Minecraft.getInstance().gameRenderer.mainCamera().position().distanceTo(drone.position());
        double t=Math.clamp(d/144,0,1);return (float)((1-t*t*(3-2*t))/(1+d/80));
    }
    private static void transition(DroneEntity drone,boolean starting,Minecraft client){
        while(TRANSITIONS.size()>=32)client.getSoundManager().stop(TRANSITIONS.remove(0));
        var sound=new Transition(drone,starting);TRANSITIONS.add(sound);client.getSoundManager().play(sound);
    }
    public static void initialize() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->clear(client));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if(client.level==null){clear(client);return;}
            TRANSITIONS.removeIf(sound->{if(sound.drone.isRemoved()||sound.drone.level()!=client.level){client.getSoundManager().stop(sound);return true;}return sound.isStopped()||!client.getSoundManager().isActive(sound);});
            LOOPS.entrySet().removeIf(entry -> {
                if (entry.getKey().isRemoved() || entry.getKey().level()!=client.level
                        || client.getCameraEntity()!=null && entry.getKey().distanceToSqr(client.getCameraEntity())>20736) {
                    for (Loop loop:entry.getValue()) client.getSoundManager().stop(loop);
                    return true;
                }
                // Resource reload can stop channels without unloading the entity.
                for(Loop loop:entry.getValue()) {
                    if(client.level!=null && client.level.getGameTime()%20==0 && !client.getSoundManager().isActive(loop)) {
                        for(Loop old:entry.getValue()) client.getSoundManager().stop(old);
                        return true;
                    }
                }
                return false;
            });
            if(client.level==null) return;
            int exhaust=0;
            for(var entity:client.level.entitiesForRendering()) {
                if(entity instanceof DroneEntity jet && jet.kind()==io.github.riveyder.shahed.entity.AircraftKind.JET
                        &&jet.getThrottle()>.08F&&!ReplayController.isGhost(jet)&&exhaust<8&&client.level.getGameTime()%3==0
                        &&client.gameRenderer.mainCamera().position().distanceToSqr(jet.position())<128*128){
                    var back=net.minecraft.world.phys.Vec3.directionFromRotation(jet.getXRot(),jet.getYRot()).scale(-1.1);
                    var at=jet.position().add(back).add(0,.35,0);
                    client.level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,at.x,at.y,at.z,back.x*.03,back.y*.03,back.z*.03);
                    client.level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,at.x,at.y,at.z,back.x*.06,.015,back.z*.06);exhaust+=2;
                }
                if(entity instanceof DroneEntity drone && !ReplayController.isGhost(drone) && !LOOPS.containsKey(drone)
                        && client.getCameraEntity()!=null && drone.distanceToSqr(client.getCameraEntity())<16384
                        && (LOOPS.size()<32 || FpvController.isCameraDrone(drone))) {
                    Loop[] loops={new Loop(drone,false),new Loop(drone,true)};
                    LOOPS.put(drone,loops);
                    for(Loop loop:loops) client.getSoundManager().play(loop);
                }
            }
        });
    }
    private static final class Loop extends AbstractTickableSoundInstance {
        private final DroneEntity drone;
        private final boolean wind;
        private boolean powered;
        private double previousDistance=Double.NaN;
        Loop(DroneEntity drone,boolean wind) {
            super(wind?ModSounds.WIND:ModSounds.aircraft(drone.kind(),0),SoundSource.NEUTRAL,RandomSource.create());
            this.drone=drone; this.wind=wind;
            powered=drone.getThrottle()>.03F;
            looping=true; delay=0; volume=0;
            relative=false; attenuation=SoundInstance.Attenuation.NONE;
            x=drone.getX(); y=drone.getY(); z=drone.getZ();
        }
        @Override public boolean canStartSilent() { return true; }
        @Override public void tick() {
            var client=Minecraft.getInstance();
            if(drone.isRemoved() || drone.level()!=client.level) { stop(); return; }
            var settings=FpvController.settings();
            float rpm=drone.getThrottle();
            if(!wind) {
                x=drone.getX(); y=drone.getY(); z=drone.getZ();
                boolean now=powered?rpm>.015F:rpm>.04F;
                if(now!=powered) {
                    transition(drone,now,client);
                    powered=now;
                }
                float gain=rpm>.005F?(.08F+.92F*(float)Math.sqrt(rpm))*(float)settings.aircraftVolume(drone.kind())*distanceGain(drone):0;
                volume+=(gain-volume)*.15F;
                double distance=client.gameRenderer.mainCamera().position().distanceTo(drone.position());
                // Radial motion gives a restrained flyby without relying on unavailable OpenAL source velocities.
                double radial=Double.isFinite(previousDistance)?Math.clamp((previousDistance-distance)*20,-40,40):0;
                previousDistance=distance;
                float load=(float)Math.sqrt(Math.clamp(rpm,0,1));
                float base=switch(drone.kind()){case SHAHED->.82F+.34F*load;case QUAD,CONTACT->.80F+.50F*load;case JET->.85F+.28F*load;};
                float desired=base*(float)(1+radial*.0025);
                pitch+=(desired-pitch)*(drone.kind().hover?.28F:.14F);
            } else {
                x=drone.getX();y=drone.getY();z=drone.getZ();
                float target=(float)Math.min(1,drone.getDeltaMovement().length()/drone.maxSpeed())*(float)settings.windVolume
                        *(FpvController.isCameraDrone(drone)?1:.18F)*distanceGain(drone);
                volume+=(target-volume)*.15F;
                pitch=.8F+Math.min(1,(float)drone.getDeltaMovement().length());
            }
        }
    }
    /** A one-shot still tracks its source until it finishes; it never sticks at the startup location. */
    private static final class Transition extends AbstractTickableSoundInstance {
        private final DroneEntity drone;private int age;
        Transition(DroneEntity drone,boolean starting){
            super(ModSounds.aircraft(drone.kind(),starting?1:2),SoundSource.NEUTRAL,RandomSource.create());
            this.drone=drone;relative=false;looping=false;attenuation=SoundInstance.Attenuation.NONE;update();
        }
        private void update(){x=drone.getX();y=drone.getY();z=drone.getZ();volume=.55F*(float)FpvController.settings().aircraftVolume(drone.kind())*distanceGain(drone);}
        @Override public boolean canStartSilent(){return true;}
        @Override public void tick(){if(drone.isRemoved()||drone.level()!=Minecraft.getInstance().level||++age>100){stop();return;}update();}
    }
    private DroneAudio() { }
}
