package io.github.riveyder.shahed.client;

import io.github.riveyder.shahed.network.ExplosionEffectPayload;
import io.github.riveyder.shahed.registry.ModSounds;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** Positioned mono sources with explicit distance gain: no vanilla 16/128-block broadcast cutoff. */
final class ExplosionAudio {
    private static final List<Layer> ACTIVE=new ArrayList<>();
    private static final int MAX_CHANNELS=24;
    static void play(ExplosionEffectPayload event,boolean echo) {
        var client=Minecraft.getInstance();
        ACTIVE.removeIf(Layer::isStopped);
        if(echo) add(new Layer(event,2),client);
        else {add(new Layer(event,0),client);add(new Layer(event,1),client);}
    }
    private static void add(Layer layer,Minecraft client) {
        while(ACTIVE.size()>=MAX_CHANNELS)client.getSoundManager().stop(ACTIVE.removeFirst());
        ACTIVE.add(layer);client.getSoundManager().play(layer);
    }
    static void clear() {var c=Minecraft.getInstance();for(var s:ACTIVE)c.getSoundManager().stop(s);ACTIVE.clear();}
    private static final class Layer extends AbstractTickableSoundInstance {
        private final Vec3 origin;
        private final ClientLevel world;
        private final int range,kind;
        private int age;
        Layer(ExplosionEffectPayload event,int kind) {
            super(kind==0?ModSounds.BLAST:kind==1?ModSounds.BLAST_DISTANT:ModSounds.BLAST_ECHO,SoundSource.NEUTRAL,RandomSource.create(event.seed()));
            this.kind=kind;origin=event.position();range=event.audioRange();world=Minecraft.getInstance().level;
            x=origin.x;y=origin.y;z=origin.z;relative=false;looping=false;
            attenuation=SoundInstance.Attenuation.NONE;pitch=1;updateGain();
        }
        @Override public boolean canStartSilent() {return true;}
        private void updateGain() {
            var s=FpvController.settings();double d=Minecraft.getInstance().gameRenderer.mainCamera().position().distanceTo(origin);
            double blend=Math.clamp((d-32)/160,0,1);blend=blend*blend*(3-2*blend);
            double falloff=Math.pow(Math.max(0,1-d/range),.7)/(1+d/220);
            volume=(float)(falloff*(kind==0?.85*(1-blend):kind==1?.8*blend*s.distantRumbleVolume:.24*s.distantRumbleVolume));
        }
        @Override public void tick() {
            if(Minecraft.getInstance().level!=world||++age>200){stop();return;}
            updateGain();
        }
    }
    private ExplosionAudio() {}
}
