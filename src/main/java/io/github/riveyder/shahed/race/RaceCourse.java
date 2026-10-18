package io.github.riveyder.shahed.race;

import java.util.*;
import net.minecraft.world.phys.Vec3;

/** World-scoped immutable route. Gate coordinates do not depend on an aircraft entity type. */
public record RaceCourse(String id,String dimension,int kind,boolean enabled,List<Gate> gates,Map<String,Best> records) {
    public static final int MAX_COURSES=64,MAX_GATES=64,MAX_RECORDS=1024;
    public RaceCourse {
        if(id==null||!id.matches("[a-z0-9_-]{1,32}")||dimension==null||!dimension.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")
                ||kind<0||kind>2||gates==null||gates.size()<2||gates.size()>MAX_GATES||records==null||records.size()>MAX_RECORDS)
            throw new IllegalArgumentException("Invalid course");
        gates=List.copyOf(gates);records=Map.copyOf(records);
        for(int i=0;i<gates.size();i++) {
            if(kind==1&&gates.get(i).radius()>2.5F)throw new IllegalArgumentException("Precision radius");
            if(i>0&&gates.get(i).center().distanceToSqr(gates.get(i-1).center())<4)throw new IllegalArgumentException("Adjacent gates too close");
        }
        for(var key:records.keySet())UUID.fromString(key);
    }
    public long best(UUID player) {var r=records.get(player.toString());return r==null?0:r.millis;}
    public long recordTime() {return records.values().stream().mapToLong(Best::millis).min().orElse(0);}
    public boolean sameRoute(RaceCourse other) {return dimension.equals(other.dimension)&&kind==other.kind&&gates.equals(other.gates);}
    public record Best(String name,long millis) {
        public Best {if(name==null||name.length()>32||millis<1||millis>3_600_000)throw new IllegalArgumentException("Invalid record");}
    }
    public record Gate(double x,double y,double z,float yaw,float radius) {
        public Gate {
            if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||Math.abs(x)>30_000_000||Math.abs(z)>30_000_000
                    ||Math.abs(y)>20_000_000||!Float.isFinite(yaw)||Math.abs(yaw)>360||!Float.isFinite(radius)||radius<1||radius>16)
                throw new IllegalArgumentException("Invalid gate");
        }
        public Vec3 center() {return new Vec3(x,y,z);}
        public Vec3 normal() {double a=Math.toRadians(yaw);return new Vec3(-Math.sin(a),0,Math.cos(a));}
        /** Intersection fraction, or -1. Standing inside the plane cannot trigger a checkpoint. */
        public double crossing(Vec3 from,Vec3 to) {
            Vec3 n=normal(),c=center();double a=from.subtract(c).dot(n),b=to.subtract(c).dot(n);
            if(Math.abs(a-b)<1e-8||a*b>0)return -1;
            double t=a/(a-b);if(t<0||t>1)return -1;
            return from.lerp(to,t).distanceToSqr(c)<=radius*radius?t:-1;
        }
    }
}
