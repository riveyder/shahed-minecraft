package io.github.riveyder.shahed.client;

import io.github.riveyder.shahed.ShahedMod;
import io.github.riveyder.shahed.network.*;
import io.github.riveyder.shahed.race.RaceCourse.Gate;
import io.github.riveyder.shahed.registry.ModParticles;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.network.chat.Component;

/** Presentation cache only. The client never advances checkpoint progress or supplies race times. */
public final class RaceClient {
    private static List<RaceCatalogPayload.Entry> catalogue=List.of();
    private static List<Gate> gates=List.of();
    private static final List<RaceGateParticle> particles=new ArrayList<>();
    private static SpriteSet sprites;
    private static ClientLevel level;
    private static RaceStatePayload state;
    private static Component notice=Component.empty();
    private static int age,refresh;
    public static List<RaceCatalogPayload.Entry> courses(){return catalogue;}
    public static Component notice(){return notice;}
    public static String time(long millis){return millis<=0?"—":String.format(Locale.ROOT,"%.3f s",millis/1000.0);}
    public static void initialize(){
        ParticleProviderRegistry.getInstance().register(ModParticles.RACE_GATE,s->{sprites=s;
            return (o,w,x,y,z,dx,dy,dz,r)->new RaceGateParticle(w,s,new Gate(x,y,z,0,1),true);});
        ClientPlayNetworking.registerGlobalReceiver(RaceCatalogPayload.TYPE,(p,c)->c.client().execute(()->{
            catalogue=p.courses();if(c.client().gui.screen() instanceof RaceScreen screen)screen.refresh();
        }));
        ClientPlayNetworking.registerGlobalReceiver(RaceStatePayload.TYPE,(p,c)->c.client().execute(()->receive(p)));
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->clear());
        ClientTickEvents.END_CLIENT_TICK.register(RaceClient::tick);
        HudElementRegistry.addLast(ShahedMod.id("race"),(g,d)->hud(g));
    }
    public static void request(int action,String course){
        if(ClientPlayNetworking.canSend(RaceRequestPayload.TYPE))ClientPlayNetworking.send(new RaceRequestPayload(action,course));
        else notice=Component.translatable("race.incompatible");
    }
    private static void receive(RaceStatePayload p){
        var client=Minecraft.getInstance();
        if(level!=client.level){clear();level=client.level;}
        if(p.status()==4){notice=Component.translatable("race."+p.reason());return;}
        notice=Component.translatable(p.status()==3?"race."+p.reason():"race.status."+p.status());
        if(state==null||!state.course().equals(p.course()))gates=List.of();
        if(!p.gates().isEmpty())gates=p.gates();
        if(state==null||p.next()!=state.next()||p.status()!=state.status()) {clearParticles();refresh=0;}
        state=p;age=0;
        if((p.status()==0||p.status()==5)&&client.gui.screen() instanceof RaceScreen)client.gui.setScreen(null);
    }
    private static void tick(Minecraft c){
        if(c.level!=level){clear();level=c.level;}if(state==null||level==null)return;
        age++;particles.removeIf(p->!p.isAlive());
        if((state.status()==2||state.status()==3)&&age>200||state.status()==5&&age>600){state=null;gates=List.of();clearParticles();return;}
        if(state.status()==2||state.status()==3)return;
        if(sprites==null||refresh++%10!=0)return;
        var camera=c.gameRenderer.mainCamera().position();
        for(int i=state.next();i<gates.size();i++){
            var gate=gates.get(i);if(camera.distanceToSqr(gate.center())>512*512)continue;
            while(particles.size()>=128)particles.removeFirst().remove();
            var p=new RaceGateParticle(level,sprites,gate,i==state.next());particles.add(p);c.particleEngine.add(p);
        }
    }
    private static void hud(GuiGraphicsExtractor g){
        var c=Minecraft.getInstance();if(state==null||c.gui.hud.isHidden()||c.gui.screen()!=null)return;
        float scale=Math.min(g.guiWidth()/480F,g.guiHeight()/270F);
        g.pose().pushMatrix();g.pose().translate((g.guiWidth()-480*scale)/2,(g.guiHeight()-270*scale)/2);g.pose().scale(scale);
        String title=state.course()+" · "+Component.translatable("race.status."+state.status()).getString();
        g.centeredText(c.font,c.font.plainSubstrByWidth(title,220),240,216,0xFF9DFFC0);
        String elapsed=String.format(Locale.ROOT,"%.2f",state.millis()/1000.0);
        g.centeredText(c.font,Component.translatable("race.hud_progress",elapsed,Math.min(state.next()+1,state.total()),state.total()),240,226,0xFFF0F5F3);
        g.centeredText(c.font,Component.translatable("race.hud_best",time(state.best()),time(state.record())),240,236,0xFFB8DCC8);
        g.pose().popMatrix();
    }
    private static void clearParticles(){for(var p:particles)p.remove();particles.clear();}
    private static void clear(){clearParticles();catalogue=List.of();gates=List.of();state=null;notice=Component.empty();level=null;age=refresh=0;}
    private RaceClient(){}
}
