package io.github.riveyder.shahed.client;

import io.github.riveyder.shahed.network.ExplosionEffectPayload;
import io.github.riveyder.shahed.registry.*;
import io.github.riveyder.shahed.replay.ReplayImpact;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** One local cinematic timeline per event. Hard budgets apply even to simultaneous CHAOS crashes. */
public final class ExplosionEffects {
    public static final int MAX_BURSTS=8,MAX_PARTICLES=2048,MAX_NEW_PER_TICK=160;
    private static final SpriteSet[] SPRITES=new SpriteSet[4];
    private static final List<Burst> BURSTS=new ArrayList<>();
    private record Owned(CinematicParticle particle,boolean replay) {}
    private static final List<Owned> PARTICLES=new ArrayList<>();
    private static final LinkedHashMap<UUID,Long> SEEN=new LinkedHashMap<>();
    private static ClientLevel level;
    private static long ticks;
    private static int emitted;
    public static void initialize() {
        SimpleParticleType[] types={ModParticles.FIREBALL,ModParticles.SMOKE,ModParticles.RING,ModParticles.DEBRIS};
        for(int i=0;i<types.length;i++) {int kind=i;
            ParticleProviderRegistry.getInstance().register(types[i],sprites->{SPRITES[kind]=sprites;
                return (options,world,x,y,z,dx,dy,dz,random)->new CinematicParticle(world,x,y,z,sprites,kind,(float)Math.clamp(dx,.1,3),(int)Math.clamp(dy,2,400));});
        }
        ClientPlayNetworking.registerGlobalReceiver(ExplosionEffectPayload.TYPE,(p,c)->c.client().execute(()->receive(p)));
        ClientTickEvents.END_CLIENT_TICK.register(ExplosionEffects::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->clear());
    }
    public static void receive(ExplosionEffectPayload event) {accept(event,false);}
    public static void replay(ReplayImpact impact) {
        if(impact.explosion()) accept(new ExplosionEffectPayload(UUID.randomUUID(),impact.position(),impact.position().hashCode(),impact.particles(),impact.sound()),true);
    }
    private static void accept(ExplosionEffectPayload event,boolean replay) {
        var client=Minecraft.getInstance();if(client.level==null||!event.valid()) return;
        if(level!=client.level) {clear();level=client.level;}
        if(SEEN.putIfAbsent(event.id(),ticks)!=null) return;
        while(SEEN.size()>512) SEEN.remove(SEEN.keySet().iterator().next());
        if(camera().distanceToSqr(event.position())>Math.max(768*768,event.audioRange()*event.audioRange())) return;
        var b=new Burst(event,replay);if(BURSTS.size()>=MAX_BURSTS) BURSTS.removeFirst();BURSTS.add(b);
        if(event.sound()&&camera().distanceTo(event.position())<event.audioRange()) ExplosionAudio.play(event,false);
    }
    private static Vec3 camera() {var c=Minecraft.getInstance();return c.gameRenderer.mainCamera().position();}
    private static void tick(Minecraft client) {
        if(level!=client.level) {clear();level=client.level;}if(level==null)return;
        ticks++;emitted=0;PARTICLES.removeIf(p->!p.particle.isAlive());
        while(PARTICLES.size()>FpvController.settings().particleBudget) PARTICLES.removeFirst().particle.remove();
        SEEN.entrySet().removeIf(e->ticks-e.getValue()>1400);
        for(var b:BURSTS) {
            if(b.age==0 && b.visual()) {
                particle(b,2,b.event.position().add(0,.15,0),Vec3.ZERO,b.scale,22);
                particle(b,0,b.event.position().add(0,.6*b.scale,0),new Vec3(0,.04,0),b.scale*1.5F,34);
                for(int i=0;i<b.fireCount;i++) {
                    var offset=randomVector(b).scale(b.scale*1.4);
                    particle(b,0,b.event.position().add(offset),offset.scale(.025).add(0,.025,0),b.scale*.85F,26+b.random.nextInt(10));
                }
                for(int i=0;i<b.debris;i++) particle(b,3,b.event.position(),randomVector(b).scale(.5*b.scale).add(0,.25,0),.09F*b.scale,45+b.random.nextInt(20));
            }
            if(b.visual()&&b.age<45&&b.age%3==0) for(int i=0;i<b.quality+2;i++) {
                double a=b.random.nextDouble()*Math.PI*2,r=b.scale*(1+b.age*.35);
                particle(b,4,b.event.position().add(Math.cos(a)*r,.12,Math.sin(a)*r),new Vec3(Math.cos(a)*.08,.025,Math.sin(a)*.08),b.scale*.55F,45);
            }
            if(b.visual()&&b.age<b.smokeTicks&&b.age%Math.max(2,8-b.quality*2)==0) {
                for(int i=0;i<(int)Math.ceil((b.quality+1)*FpvController.settings().smokeDensity);i++) particle(b,1,b.event.position().add(randomVector(b).scale(b.scale*.7)).add(0,.5,0),
                        new Vec3(.025*b.scale,.14*b.scale,0),b.scale*(.6F+b.random.nextFloat()*.35F),Math.min(180,b.smokeTicks-b.age+80));
            }
            if(b.age==12&&b.event.sound()&&FpvController.settings().explosionEcho) {
                if(camera().distanceTo(b.event.position())<b.event.audioRange()) ExplosionAudio.play(b.event,true);
            }
            if(b.visual()&&b.event.visualScale()>=3.5F&&(b.age==7||b.age==14))
                particle(b,2,b.event.position().add(0,.3,0),Vec3.ZERO,b.scale*1.2F,32);
            if(b.visual()&&b.age<b.smokeTicks&&b.age%10==0) {
                // Burning wreckage and embers are decorative particles even in SAFE.
                int count=(int)(FpvController.settings().debrisDensity*(b.quality+2));
                for(int i=0;i<count;i++) {
                    var v=randomVector(b);
                    particle(b,5,b.event.position().add(v.scale(b.scale)),v.scale(.16).add(0,.22,0),.07F*b.scale,50+b.random.nextInt(50));
                }
                if(b.age%20==0 && FpvController.settings().debrisDensity>0)
                    particle(b,6,b.event.position().add(randomVector(b).scale(b.scale)),new Vec3(0,.01,0),.25F*b.scale,45);
            }
            b.age++;
        }
        BURSTS.removeIf(b->b.age>b.smokeTicks+185);
    }
    private static Vec3 randomVector(Burst b) {return new Vec3(b.random.nextDouble()*2-1,b.random.nextDouble(),b.random.nextDouble()*2-1);}
    private static void particle(Burst b,int kind,Vec3 pos,Vec3 velocity,float size,int life) {
        var sprites=SPRITES[kind>=5?0:kind==4?1:kind];if(sprites==null||emitted>=MAX_NEW_PER_TICK)return;
        if(PARTICLES.size()>=Math.min(MAX_PARTICLES,FpvController.settings().particleBudget)) PARTICLES.removeFirst().particle.remove();
        var p=new CinematicParticle(level,pos.x,pos.y,pos.z,sprites,kind,size,life);p.setParticleSpeed(velocity.x,velocity.y,velocity.z);
        Minecraft.getInstance().particleEngine.add(p);PARTICLES.add(new Owned(p,b.replay));emitted++;
    }
    public static float shake() {
        double sum=0;
        for(var b:BURSTS) if(b.event.particles()&&b.age<40) sum+=Math.max(0,1-camera().distanceTo(b.event.position())/160)*Math.exp(-b.age/12.0)*b.event.visualScale()/2;
        return (float)(Math.min(1,sum)*FpvController.settings().cameraShake);
    }
    public static void flash(GuiGraphicsExtractor g) {
        float strength=0;
        for(var b:BURSTS) if(b.event.particles()&&b.age<4) strength=Math.max(strength,(float)(Math.max(0,1-camera().distanceTo(b.event.position())/160)*(1-b.age/4F)));
        if(strength>0)g.fill(0,0,g.guiWidth(),g.guiHeight(),((int)(strength*65)<<24)|0xFFF1C7);
    }
    public static int activeBursts() {return BURSTS.size();}
    public static int liveParticles() {return (int)PARTICLES.stream().filter(p->p.particle.isAlive()).count();}
    public static void clearReplay() {BURSTS.removeIf(b->b.replay);PARTICLES.removeIf(p->{if(p.replay)p.particle.remove();return p.replay;});}
    public static void clear() {for(var p:PARTICLES)p.particle.remove();PARTICLES.clear();BURSTS.clear();SEEN.clear();ExplosionAudio.clear();level=null;ticks=0;}
    private static final class Burst {
        final ExplosionEffectPayload event;final boolean replay;final RandomSource random;
        final float scale;final int smokeTicks,quality,fireCount,debris;int age;
        Burst(ExplosionEffectPayload event,boolean replay) {
            this.event=event;this.replay=replay;random=RandomSource.create(event.seed());var s=FpvController.settings();
            scale=(float)s.explosionVfxScale*event.visualScale();smokeTicks=(int)(s.smokeSeconds*20);quality=s.explosionQuality;
            fireCount=6+quality*6;debris=(int)(s.debrisDensity*(12+quality*18));
        }
        boolean visual() {return event.particles()&&camera().distanceToSqr(event.position())<768*768;}
    }
    private ExplosionEffects() {}
}
