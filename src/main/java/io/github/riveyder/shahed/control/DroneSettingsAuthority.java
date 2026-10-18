package io.github.riveyder.shahed.control;

import io.github.riveyder.shahed.network.*;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.phys.Vec3;

/** All entry points run on the server thread. No packet can choose another player's identity or test coordinates. */
public final class DroneSettingsAuthority {
    private record Area(ServerLevel level,BlockPos pos) {}
    private record Challenge(UUID nonce,long revision,long expiry,Area area) {}
    private record PolicyChallenge(UUID nonce,long revision,long expiry,DroneConfig.Settings candidate) {}
    private static final Map<UUID,PolicyChallenge> POLICY_CHALLENGES=new HashMap<>();
    private static final Map<UUID,Area> AREAS=new HashMap<>();
    private static final Map<UUID,Challenge> CHALLENGES=new HashMap<>();
    private static final Map<UUID,Long> REQUESTS=new HashMap<>();
    private static long revision;
    public static boolean canEdit(ServerPlayer p) {return p.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);}
    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((h,s,server)->send(h.player,snapshot(h.player,"Настройки сервера получены",SettingsRequestPayload.NONE)));
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->{AREAS.remove(h.player.getUUID());CHALLENGES.remove(h.player.getUUID());POLICY_CHALLENGES.remove(h.player.getUUID());REQUESTS.remove(h.player.getUUID());});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{AREAS.clear();CHALLENGES.clear();POLICY_CHALLENGES.clear();REQUESTS.clear();revision=0;});
    }
    public static SettingsStatePayload snapshot(ServerPlayer p,String status,UUID nonce) {
        var area=AREAS.get(p.getUUID());
        return new SettingsStatePayload(revision,DroneConfig.get(),canEdit(p),status,nonce,
                area==null?"Не назначена":area.level.dimension().identifier()+" "+area.pos.toShortString());
    }
    public static void receive(ServerPlayer p,SettingsRequestPayload request) {send(p,handle(p,request));}
    public static SettingsStatePayload handle(ServerPlayer p,SettingsRequestPayload request) {
        long now=p.level().getServer().getTickCount();UUID id=p.getUUID();
        String status="Запрос отклонён";UUID nonce=SettingsRequestPayload.NONE;
        boolean confirming=request.action()==4 && CHALLENGES.containsKey(id) && CHALLENGES.get(id).nonce.equals(request.nonce())
                ||request.action()==6 && POLICY_CHALLENGES.containsKey(id) && POLICY_CHALLENGES.get(id).nonce.equals(request.nonce());
        if(!confirming && now-REQUESTS.getOrDefault(id,now-10)<10) return snapshot(p,"Слишком часто; повторите через секунду",nonce);
        REQUESTS.put(id,now);
        if(request.action()==0) return snapshot(p,"Настройки сервера получены",nonce);
        if(!canEdit(p)) return snapshot(p,"Требуются права администратора",nonce);
        if(request.revision()!=revision) return snapshot(p,"Настройки изменились; перечитайте и повторите",nonce);
        switch(request.action()) {
            case 1 -> {
                var candidate=request.settings().sanitized();
                if(DroneConfig.needsConfirmation(candidate) && !candidate.equals(DroneConfig.get())) {
                    nonce=UUID.randomUUID();POLICY_CHALLENGES.put(id,new PolicyChallenge(nonce,revision,now+200,candidate));
                    var current=snapshot(p,"CHAOS: мощность "+candidate.explosionPower()+", до "+candidate.maxBlocks()+" блоков за взрыв. Подтвердите в течение 10 секунд.",nonce);
                    return new SettingsStatePayload(current.revision(),current.settings(),current.admin(),current.status(),nonce,current.area(),6);
                }
                status=apply(p,candidate);
            }
            case 6 -> {
                var challenge=POLICY_CHALLENGES.remove(id);
                if(challenge!=null && challenge.nonce.equals(request.nonce()) && challenge.revision==revision && now<=challenge.expiry)
                    status=apply(p,challenge.candidate);
                else status="Подтверждение CHAOS недействительно или истекло";
            }
            case 2 -> {
                var area=new Area(p.level(),p.blockPosition().immutable());
                if(permitted(p,area)) {AREAS.put(id,area);CHALLENGES.remove(id);status="Точка теста назначена; отойдите на безопасное расстояние";}
                else status="Точка запрещена защитой сервера";
            }
            case 3 -> {
                var area=AREAS.get(id);
                if(area!=null && permitted(p,area)) {
                    nonce=UUID.randomUUID();CHALLENGES.put(id,new Challenge(nonce,revision,now+200,area));
                    status="Подтвердите тест в течение 10 секунд";
                } else status="Сначала назначьте разрешённую точку теста";
            }
            case 4 -> {
                var challenge=CHALLENGES.remove(id); // consume before effects: retries cannot explode twice
                if(challenge!=null && challenge.nonce.equals(request.nonce()) && challenge.revision==revision
                        && now<=challenge.expiry && permitted(p,challenge.area)) {
                    int blocks=DroneExplosions.explode(challenge.area.level,null,p,Vec3.atCenterOf(challenge.area.pos),DroneConfig.get());
                    status="Тест выполнен. Затронуто блоков: "+blocks;
                } else status="Подтверждение недействительно или истекло";
            }
            default -> { }
        }
        return snapshot(p,status,nonce);
    }
    private static boolean permitted(ServerPlayer p,Area a) {
        return p.level()==a.level && a.level.hasChunkAt(a.pos) && a.level.mayInteract(p,a.pos)
                && DroneExplosionProtection.ALLOW.invoker().allow(a.level,p,a.pos);
    }
    private static String apply(ServerPlayer p,DroneConfig.Settings candidate) {
        if(!DroneConfig.update(candidate)) return "Ошибка записи: изменения не применены";
        revision++;CHALLENGES.clear();POLICY_CHALLENGES.clear();
        for(var player:p.level().getServer().getPlayerList().getPlayers())
            if(player!=p) send(player,snapshot(player,"Администратор обновил настройки",SettingsRequestPayload.NONE));
        return "Настройки сохранены и применены";
    }
    private static void send(ServerPlayer p,SettingsStatePayload state) {
        if(p.connection!=null && ServerPlayNetworking.canSend(p,SettingsStatePayload.TYPE)) ServerPlayNetworking.send(p,state);
    }
    private DroneSettingsAuthority() {}
}
