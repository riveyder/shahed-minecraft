package io.github.riveyder.shahed.client;

import io.github.riveyder.shahed.control.DroneConfig;
import io.github.riveyder.shahed.network.*;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.*;

/** Client cache is presentation only; every mutation is checked again by the server. */
public final class ServerSettingsClient {
    private static SettingsStatePayload state;
    public static SettingsStatePayload state() {return state;}
    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(SettingsStatePayload.TYPE,(p,c)->c.client().execute(()->{
            state=p;
            if(c.client().gui.screen() instanceof FpvSettingsScreen screen) screen.receive(p);
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((h,c)->state=null);
    }
    public static void request(int action,DroneConfig.Settings settings,UUID nonce) {
        if(ClientPlayNetworking.canSend(SettingsRequestPayload.TYPE)) ClientPlayNetworking.send(new SettingsRequestPayload(
                action,state==null?0:state.revision(),settings,nonce));
    }
    public static void read() {request(0,DroneConfig.DEFAULTS,SettingsRequestPayload.NONE);}
    private ServerSettingsClient() {}
}
