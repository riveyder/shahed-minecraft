package io.github.riveyder.shahed.client;
import io.github.riveyder.shahed.network.MechanicalSoundPayload;
import io.github.riveyder.shahed.registry.ModSounds;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import java.util.*;
/** Bounded, deduplicated positional one-shots; no client gameplay or release prediction. */
public final class MechanicalAudio {
 private static final Set<UUID> SEEN=new LinkedHashSet<>();
 private static final List<Shot> ACTIVE=new ArrayList<>();
 public static void initialize(){
  ClientPlayNetworking.registerGlobalReceiver(MechanicalSoundPayload.TYPE,(event,context)->context.client().execute(()->play(event)));
  ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{for(var s:ACTIVE)client.getSoundManager().stop(s);ACTIVE.clear();SEEN.clear();});
 }
 private static void play(MechanicalSoundPayload event){
  var c=Minecraft.getInstance();if(c.level==null||!event.valid()||!SEEN.add(event.id()))return;
  while(SEEN.size()>256)SEEN.remove(SEEN.iterator().next());
  ACTIVE.removeIf(Shot::isStopped);
  if(c.gameRenderer.mainCamera().position().distanceTo(event.position())>=64)return;
  while(ACTIVE.size()>=16)c.getSoundManager().stop(ACTIVE.remove(0));
  var sound=new Shot(event);ACTIVE.add(sound);c.getSoundManager().play(sound);
 }
 private static final class Shot extends AbstractTickableSoundInstance {
  private final MechanicalSoundPayload event;private final net.minecraft.client.multiplayer.ClientLevel world;private int age;
  Shot(MechanicalSoundPayload event){super(event.impact()?ModSounds.IMPACT:ModSounds.PAYLOAD_RELEASE,SoundSource.NEUTRAL,RandomSource.create());this.event=event;world=Minecraft.getInstance().level;
   x=event.position().x;y=event.position().y;z=event.position().z;relative=false;looping=false;attenuation=SoundInstance.Attenuation.NONE;gain();}
  private void gain(){double t=Math.clamp(Minecraft.getInstance().gameRenderer.mainCamera().position().distanceTo(event.position())/64,0,1);
   volume=(float)((1-t*t*(3-2*t))*(event.impact()?.45*FpvController.settings().explosionVolume:.65*FpvController.settings().quadVolume));}
  @Override public boolean canStartSilent(){return true;}
  @Override public void tick(){if(Minecraft.getInstance().level!=world||++age>80){stop();return;}gain();}
 }
 private MechanicalAudio(){}
}
