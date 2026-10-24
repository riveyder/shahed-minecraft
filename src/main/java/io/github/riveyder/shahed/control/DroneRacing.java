package io.github.riveyder.shahed.control;

import io.github.riveyder.shahed.network.*;
import io.github.riveyder.shahed.race.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.Mth;

/** World-scoped courses and independent, safe, server-authoritative runs. */
public final class DroneRacing {
    private static final Map<MinecraftServer,State> STATES=new WeakHashMap<>();
    private static final class State {
        final RaceStorage storage;
        Map<String,RaceCourse> courses;
        final Map<UUID,Entry> runs=new HashMap<>();
        final Map<UUID,Draft> drafts=new HashMap<>();
        final Map<UUID,Long> requests=new HashMap<>();
        State(MinecraftServer server){storage=new RaceStorage(server);courses=storage.load();}
    }
    private static final class Draft {
        final String id,dimension;final int kind;final RaceCourse base;
        final List<RaceCourse.Gate> gates=new ArrayList<>();boolean finish,enabled=true;
        Draft(String id,String dimension,int kind,RaceCourse base){this.id=id;this.dimension=dimension;this.kind=kind;this.base=base;
            if(base!=null){gates.addAll(base.gates());enabled=base.enabled();finish=true;}}
    }
    private record Entry(ServerPlayer player,RaceAircraft aircraft,CourseRun run,long joined) {}
    private static State state(MinecraftServer server){return STATES.computeIfAbsent(server,State::new);}
    private static State state(ServerPlayer p){return state(p.level().getServer());}
    public static String time(long millis){return millis<=0?"—":String.format(Locale.ROOT,"%.3f s",millis/1000.0);}
    private static void send(ServerPlayer p,CustomPacketPayload payload){if(p.connection!=null&&ServerPlayNetworking.canSend(p,payload.type()))ServerPlayNetworking.send(p,payload);}
    private static int message(ServerPlayer p,String key,Object... args){p.sendSystemMessage(Component.translatable("race."+key,args));return 1;}
    private static int fail(ServerPlayer p,String reason){message(p,reason);send(p,new RaceStatePayload("",0,4,0,0,0,0,0,reason,List.of()));return 0;}
    private static boolean admin(ServerPlayer p){if(DroneSettingsAuthority.canEdit(p))return true;fail(p,"admin_required");return false;}
    private static String dimension(ServerPlayer p){return p.level().dimension().identifier().toString();}

    public static void request(ServerPlayer p,RaceRequestPayload request){
        var s=state(p);long now=p.level().getServer().getTickCount();
        if(now-s.requests.getOrDefault(p.getUUID(),now-5)<5)return;
        s.requests.put(p.getUUID(),now);
        switch(request.action()){case 0->catalog(p);case 1->join(p,request.course());case 2->cancel(p,"cancelled");case 3->preview(p,request.course());default->fail(p,"invalid");}
    }
    private static String category(ServerPlayer p){var aircraft=RaceAircraft.controlled(p);return aircraft==null?"shahed":aircraft.category();}
    public static void catalog(ServerPlayer p){
        var rows=state(p).courses.values().stream().filter(c->c.enabled()&&c.dimension().equals(dimension(p)))
                .sorted(Comparator.comparing(RaceCourse::id)).map(c->new RaceCatalogPayload.Entry(c.id(),c.kind(),c.gates().size(),c.best(p.getUUID(),category(p)),c.recordTime(category(p)))).toList();
        send(p,new RaceCatalogPayload(rows));
    }
    public static int list(ServerPlayer p){
        var list=state(p).courses.values().stream().filter(c->c.enabled()||DroneSettingsAuthority.canEdit(p)).sorted(Comparator.comparing(RaceCourse::id)).toList();
        if(list.isEmpty())message(p,"empty");
        for(var c:list)message(p,"list_row",c.id(),Component.translatable("race.kind."+c.kind()),c.gates().size(),c.dimension(),Component.translatable(c.enabled()?"race.enabled":"race.disabled"));
        catalog(p);return list.size();
    }
    public static int records(ServerPlayer p,String id){return records(p,id,category(p));}
    public static int records(ServerPlayer p,String id,String category){
        var c=state(p).courses.get(id);if(c==null||!c.enabled()&&!DroneSettingsAuthority.canEdit(p))return fail(p,"not_found");
        message(p,"records",id+" / "+category,time(c.best(p.getUUID(),category)),time(c.recordTime(category)));
        int rank=0;for(var r:c.records().entrySet().stream().filter(e->RaceCourse.matchesCategory(e.getKey(),category)).sorted(Comparator.<Map.Entry<String,RaceCourse.Best>>comparingLong(e->e.getValue().millis()).thenComparing(Map.Entry::getKey)).limit(10).toList())
            message(p,"record_row",++rank,r.getValue().name(),time(r.getValue().millis()));
        return 1;
    }
    public static int join(ServerPlayer p,String id){
        var s=state(p);var c=s.courses.get(id);
        if(c==null||!c.enabled())return fail(p,"not_found");
        if(!c.dimension().equals(dimension(p)))return fail(p,"dimension");
        var aircraft=RaceAircraft.controlled(p);if(aircraft==null||!aircraft.valid(p))return fail(p,"training_required");
        if(p.connection!=null&&!ServerPlayNetworking.canSend(p,RaceStatePayload.TYPE))return fail(p,"incompatible");
        aircraft.beginRace();
        var e=new Entry(p,aircraft,new CourseRun(c,aircraft.position()),p.level().getServer().getTickCount());
        s.runs.put(p.getUUID(),e);update(s,e,0,"armed",true);message(p,"armed",id);return 1;
    }
    public static int preview(ServerPlayer p,String id){
        var s=state(p);if(s.runs.containsKey(p.getUUID()))return fail(p,"already_running");
        var c=s.courses.get(id);if(c==null||!c.enabled()&&!DroneSettingsAuthority.canEdit(p))return fail(p,"not_found");
        if(!c.dimension().equals(dimension(p)))return fail(p,"dimension");
        send(p,new RaceStatePayload(c.id(),c.kind(),5,0,c.gates().size(),0,c.best(p.getUUID(),category(p)),c.recordTime(category(p)),"preview",c.gates()));return 1;
    }
    public static int cancel(ServerPlayer p,String reason){
        var s=state(p);var e=s.runs.remove(p.getUUID());
        if(e!=null){update(s,e,3,reason,false);message(p,reason);}
        else send(p,new RaceStatePayload("",0,3,0,0,0,0,0,reason,List.of()));
        return 1;
    }
    private static void cancelCourse(State s,String id){
        for(var e:new ArrayList<>(s.runs.values()))if(e.run.course.id().equals(id))cancel(e.player,"course_changed");
    }
    private static void update(State s,Entry e,int status,String reason,boolean geometry){
        var c=s.courses.getOrDefault(e.run.course.id(),e.run.course);
        send(e.player,new RaceStatePayload(c.id(),c.kind(),status,e.run.next(),c.gates().size(),e.run.elapsed(),c.best(e.player.getUUID(),e.aircraft.category()),c.recordTime(e.aircraft.category()),reason,geometry?c.gates():List.of()));
    }
    private static void finish(State s,Entry e){
        var c=s.courses.get(e.run.course.id());long time=e.run.elapsed();
        if(c!=null&&(c.best(e.player.getUUID(),e.aircraft.category())==0||time<c.best(e.player.getUUID(),e.aircraft.category()))){
            var records=new HashMap<>(c.records());String key=RaceCourse.recordKey(e.player.getUUID(),e.aircraft.category());
            if(records.size()<RaceCourse.MAX_RECORDS||records.containsKey(key)){
                String name=e.player.getName().getString();records.put(key,new RaceCourse.Best(name.substring(0,Math.min(32,name.length())),time));
                var replacement=new RaceCourse(c.id(),c.dimension(),c.kind(),c.enabled(),c.gates(),records);
                var courses=new LinkedHashMap<>(s.courses);courses.put(c.id(),replacement);
                if(s.storage.save(courses))s.courses=courses;else message(e.player,"storage_error");
            }else message(e.player,"record_limit");
        }
        update(s,e,2,"finished",false);message(e.player,"finished",e.run.course.id(),time(time));s.runs.remove(e.player.getUUID());
    }
    public static int create(ServerPlayer p,String id,int kind){
        if(!admin(p))return 0;var s=state(p);
        if(!id.matches("[a-z0-9_-]{1,32}")||kind<0||kind>2)return fail(p,"invalid");
        if(s.courses.containsKey(id))return fail(p,"exists");
        if(s.courses.size()>=RaceCourse.MAX_COURSES)return fail(p,"course_limit");
        s.drafts.put(p.getUUID(),new Draft(id,dimension(p),kind,null));return message(p,"created",id);
    }
    public static int edit(ServerPlayer p,String id){
        if(!admin(p))return 0;var s=state(p);var c=s.courses.get(id);if(c==null)return fail(p,"not_found");
        if(!c.dimension().equals(dimension(p)))return fail(p,"dimension");
        s.drafts.put(p.getUUID(),new Draft(id,c.dimension(),c.kind(),c));return message(p,"editing",id);
    }
    public static int gate(ServerPlayer p,String operation,int index,float radius){
        if(!admin(p))return 0;var d=state(p).drafts.get(p.getUUID());if(d==null)return fail(p,"no_draft");
        if(!d.dimension.equals(dimension(p)))return fail(p,"dimension");
        if(operation.equals("remove")){
            if(index<1||index>d.gates.size())return fail(p,"invalid");
            if(index==d.gates.size())d.finish=false;d.gates.remove(index-1);return message(p,"gate_removed",index);
        }
        if(radius==0)radius=d.kind==1?1.5F:d.kind==2?3:4;
        if(!Float.isFinite(radius)||radius<1||radius>16||d.kind==1&&radius>2.5F)return fail(p,"invalid");
        var pos=p.getEyePosition();var g=new RaceCourse.Gate(pos.x,pos.y,pos.z,Mth.wrapDegrees(p.getYRot()),radius);
        if(operation.equals("set")){
            if(index<1||index>d.gates.size())return fail(p,"invalid");d.gates.set(index-1,g);
        }else if(operation.equals("start")){
            if(d.gates.isEmpty())d.gates.add(g);else d.gates.set(0,g);
        }else if(operation.equals("add")||operation.equals("finish")){
            if(d.gates.isEmpty())return fail(p,"start_first");
            if(operation.equals("finish")&&d.finish)d.gates.set(d.gates.size()-1,g);
            else {if(d.gates.size()>=RaceCourse.MAX_GATES)return fail(p,"gate_limit");
                d.gates.add(operation.equals("add")&&d.finish?d.gates.size()-1:d.gates.size(),g);}
            if(operation.equals("finish"))d.finish=true;
        }else return fail(p,"invalid");
        return message(p,"gate_added",d.gates.size());
    }
    public static int save(ServerPlayer p){
        if(!admin(p))return 0;var s=state(p);var d=s.drafts.get(p.getUUID());if(d==null)return fail(p,"no_draft");
        if(!d.finish||d.gates.size()<2)return fail(p,"finish_required");
        var old=s.courses.get(d.id);
        if(d.base==null&&old!=null||d.base!=null&&(old==null||!d.base.sameRoute(old)||d.base.enabled()!=old.enabled()))return fail(p,"stale");
        try{
            var c=new RaceCourse(d.id,d.dimension,d.kind,d.enabled,d.gates,Map.of());
            boolean changed=old==null||!old.sameRoute(c);
            if(!changed)c=new RaceCourse(c.id(),c.dimension(),c.kind(),c.enabled(),c.gates(),old.records());
            var courses=new LinkedHashMap<>(s.courses);courses.put(c.id(),c);
            if(!s.storage.save(courses))return fail(p,"storage_error");
            if(changed||!c.enabled())cancelCourse(s,c.id());s.courses=courses;s.drafts.remove(p.getUUID());
            message(p,changed?"saved_reset":"saved",c.id());catalog(p);return 1;
        }catch(IllegalArgumentException error){return fail(p,"invalid_route");}
    }
    public static int delete(ServerPlayer p,String id){
        if(!admin(p))return 0;var s=state(p);if(!s.courses.containsKey(id))return fail(p,"not_found");
        var courses=new LinkedHashMap<>(s.courses);courses.remove(id);if(!s.storage.save(courses))return fail(p,"storage_error");
        cancelCourse(s,id);s.courses=courses;catalog(p);return message(p,"deleted",id);
    }
    public static int enabled(ServerPlayer p,String id,boolean enabled){
        if(!admin(p))return 0;var s=state(p);var c=s.courses.get(id);if(c==null)return fail(p,"not_found");
        var courses=new LinkedHashMap<>(s.courses);courses.put(id,new RaceCourse(c.id(),c.dimension(),c.kind(),enabled,c.gates(),c.records()));
        if(!s.storage.save(courses))return fail(p,"storage_error");if(!enabled)cancelCourse(s,id);s.courses=courses;catalog(p);return message(p,"saved",id);
    }
    public static void register(){
        ServerLifecycleEvents.SERVER_STARTED.register(server->state(server));
        ServerTickEvents.END_SERVER_TICK.register(server->{
            var s=STATES.get(server);if(s==null||s.runs.isEmpty())return;
            long now=server.getTickCount();
            for(var e:new ArrayList<>(s.runs.values())){
                if(!e.aircraft.valid(e.player)||!e.player.isAlive()){cancel(e.player,"lost_control");continue;}
                if(now-e.joined>=72000){cancel(e.player,"timeout");continue;}
                boolean progress=e.run.tick(e.aircraft.position(),now);
                if(e.run.finished())finish(s,e);
                else if(progress||now%5==0)update(s,e,e.run.next()==0?0:1,e.run.next()==0?"armed":"running",progress);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((h,server)->{var s=STATES.get(server);if(s!=null){s.runs.remove(h.player.getUUID());s.drafts.remove(h.player.getUUID());s.requests.remove(h.player.getUUID());}});
        ServerLifecycleEvents.SERVER_STOPPED.register(STATES::remove);
    }
    private DroneRacing(){}
}
