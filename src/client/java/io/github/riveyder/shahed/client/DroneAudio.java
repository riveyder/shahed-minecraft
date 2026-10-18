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
    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
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
            for(var entity:client.level.entitiesForRendering()) {
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
            super(wind?ModSounds.WIND:ModSounds.ENGINE,SoundSource.NEUTRAL,RandomSource.create());
            this.drone=drone; this.wind=wind;
            powered=drone.getThrottle()>.03F;
            looping=true; delay=0; volume=0;
            relative=wind; attenuation=wind?SoundInstance.Attenuation.NONE:SoundInstance.Attenuation.LINEAR;
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
                    client.level.playLocalSound(x,y,z,now?ModSounds.STARTUP:ModSounds.SHUTDOWN,
                            SoundSource.NEUTRAL,(float)settings.engineVolume,1,false);
                    powered=now;
                }
                float gain=rpm>.005F?(.08F+.92F*(float)Math.sqrt(rpm))*(float)settings.engineVolume:0;
                volume+=(gain-volume)*.15F;
                double distance=client.gameRenderer.mainCamera().position().distanceTo(drone.position());
                // Radial motion gives a restrained flyby without relying on unavailable OpenAL source velocities.
                double radial=Double.isFinite(previousDistance)?Math.clamp((previousDistance-distance)*20,-40,40):0;
                previousDistance=distance;
                float desired=(.65F+rpm*.85F)*(float)(1+radial*.004);
                pitch+=(desired-pitch)*.12F;
            } else {
                x=y=z=0;
                float target=FpvController.isCameraDrone(drone)
                        ? (float)Math.min(1,drone.getDeltaMovement().length()/DroneEntity.MAX_SPEED)*(float)settings.windVolume : 0;
                volume+=(target-volume)*.15F;
                pitch=.8F+Math.min(1,(float)drone.getDeltaMovement().length());
            }
        }
    }
    private DroneAudio() { }
}
