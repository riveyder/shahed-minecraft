package io.github.riveyder.shahed.client;

import io.github.riveyder.shahed.network.TelemetryPayload;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

/** Transparent instruments in a responsive 480x270 safe area. All samples originate on the server. */
public final class FpvHud {
    private static TelemetryPayload target;
    private static long received, previous;
    private static double speed, altitude, climb, yaw, pitch, roll, throttle;
    public static void reset() { target=null;previous=0; }
    public static void receive(TelemetryPayload sample) {
        if(target==null) {speed=sample.speed();altitude=sample.altitude();climb=sample.verticalSpeed();
            yaw=sample.heading();pitch=sample.pitch();roll=sample.roll();throttle=sample.throttle();}
        target=sample;received=System.nanoTime();
    }
    public static void render(GuiGraphicsExtractor g, boolean spectator) {
        var settings=FpvController.settings();int c=settings.hudArgb();
        long now=System.nanoTime();
        double blend=previous==0?1:1-Math.exp(-Math.clamp((now-previous)/1e9,0,.25)/.12);previous=now;
        if(target!=null) {speed+=(target.speed()-speed)*blend;altitude+=(target.altitude()-altitude)*blend;
            climb+=(target.verticalSpeed()-climb)*blend;yaw+=Mth.wrapDegrees(target.heading()-yaw)*blend;
            pitch+=(target.pitch()-pitch)*blend;roll+=(target.roll()-roll)*blend;throttle+=(target.throttle()-throttle)*blend;}
        float scale=Math.min(g.guiWidth()/480F,g.guiHeight()/270F);
        g.pose().pushMatrix();g.pose().translate((g.guiWidth()-480*scale)/2,(g.guiHeight()-270*scale)/2);g.pose().scale(scale);
        line(g,225,135,235,135,c);line(g,245,135,255,135,c);line(g,240,124,240,131,c);line(g,240,139,240,146,c);
        String connection=target==null?"ACQUIRING":now-received>2_000_000_000L?"TELEMETRY HOLD":spectator?"OBSERVER / LINK":"LINK ACTIVE";
        text(g,connection,settings.minimalHud?16:334,20,c);
        if(target==null) {g.pose().popMatrix();return;}
        centered(g,FpvController.aircraftLabel(),240,8,c);
        if(settings.minimalHud) {
            text(g,String.format(Locale.ROOT,"SPD %.1f m/s",speed),16,239,c);
            text(g,String.format(Locale.ROOT,"ALT %.1f m",altitude),350,239,c);
            g.pose().popMatrix();return;
        }
        text(g,"KRIVYE RUKI",16,16,c);text(g,"AEROSPACE / FPV",16,28,c);
        // Heading tape: Minecraft yaw 0 is south. N/E/S/W remain geographically correct.
        double compass=(yaw+180+360)%360;
        for(int offset=-60;offset<=60;offset+=15) {
            double value=Math.floor(compass/15)*15+offset;int x=240+(int)((value-compass)*1.5);
            line(g,x,42,x,offset%30==0?49:46,c);
            int bearing=Math.floorMod((int)value,360);
            if(bearing%30==0) centered(g,switch(bearing){case 0->"N";case 90->"E";case 180->"S";case 270->"W";default->String.valueOf(bearing);},x,31,c);
        }
        centered(g,String.format(Locale.ROOT,"HDG %03.0f",compass),240,56,c);
        line(g,240,40,240,51,c);
        tape(g,70,135,speed,2,"m/s",c);tape(g,405,135,altitude,10,"ALT m",c);
        // Pitch ladder rotates around the optical centre and is clipped away from other instruments.
        g.enableScissor(112,73,365,201);
        g.pose().pushMatrix();g.pose().translate(240,135);g.pose().rotate((float)Math.toRadians(-roll));
        for(int degrees=-60;degrees<=60;degrees+=10) {
            int y=(int)((degrees-pitch)*2);int span=degrees==0?65:34;
            line(g,-span,y,-10,y,c);line(g,10,y,span,y,c);
            if(degrees!=0) {text(g,Integer.toString(-degrees),-span-21,y-4,c);text(g,Integer.toString(-degrees),span+4,y-4,c);}
        }
        g.pose().popMatrix();g.disableScissor();
        text(g,String.format(Locale.ROOT,"PITCH %+05.1f",pitch),130,207,c);
        text(g,String.format(Locale.ROOT,"ROLL %+05.1f",roll),260,207,c);
        line(g,441,92,441,178,c);line(g,437,135,446,135,c);
        int vy=135-(int)(Math.clamp(climb,-10,10)*4);g.fill(438,Math.min(vy,135),445,Math.max(vy,135)+1,c);
        text(g,"V/S",433,78,c);text(g,String.format(Locale.ROOT,"%+.1f",climb),427,185,c);
        text(g,String.format(Locale.ROOT,"THR %3.0f%%",throttle*100),16,220,c);
        g.outline(16,233,88,5,c);g.fill(18,235,18+(int)(84*throttle),236,c);
        int seconds=target.flightTicks()/20;
        text(g,String.format(Locale.ROOT,"FLIGHT %02d:%02d",seconds/60,seconds%60),350,220,c);
        text(g,"ID "+target.droneUuid().toString().substring(0,8).toUpperCase(Locale.ROOT),350,234,c);
        centered(g,"FLIGHT SYSTEM / FULL",240,246,c);
        g.pose().popMatrix();
    }
    private static void tape(GuiGraphicsExtractor g,int x,int y,double value,int step,String unit,int c) {
        text(g,unit,x-18,y-61,c);line(g,x,y-45,x,y+45,c);
        for(int i=-3;i<=3;i++) {double mark=Math.floor(value/step)*step+i*step;
            int py=y-(int)((mark-value)*12/step);if(Math.abs(py-y)>43)continue;
            line(g,x-5,py,x+5,py,c);text(g,String.format(Locale.ROOT,"%.0f",mark),x+9,py-4,c);}
        g.fill(x-29,y-6,x-1,y+6,0x55000805);text(g,String.format(Locale.ROOT,"%.1f",value),x-35,y-4,c);
    }
    private static void line(GuiGraphicsExtractor g,int x1,int y1,int x2,int y2,int c) {g.fill(x1,y1,Math.max(x1+1,x2),Math.max(y1+1,y2),c);}
    private static void text(GuiGraphicsExtractor g,String s,int x,int y,int c) {g.text(Minecraft.getInstance().font,s,x,y,c);}
    private static void centered(GuiGraphicsExtractor g,String s,int x,int y,int c) {g.centeredText(Minecraft.getInstance().font,s,x,y,c);}
    private FpvHud() { }
}
