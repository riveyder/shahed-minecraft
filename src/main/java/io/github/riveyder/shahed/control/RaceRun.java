package io.github.riveyder.shahed.control;

import java.util.List;
import net.minecraft.world.phys.Vec3;

/** Server-tick stopwatch and ordered swept checkpoint detection. */
public final class RaceRun {
    private final List<Vec3> gates;
    private Vec3 previous;
    private int next;
    private int ticks;
    public RaceRun(List<Vec3> gates,Vec3 start) {
        if(gates.size()<2||gates.size()>32) throw new IllegalArgumentException("Race requires 2..32 checkpoints");
        this.gates=List.copyOf(gates);previous=start;
    }
    public void tick(Vec3 position) {
        if(finished()) return;
        ticks++;
        Vec3 segment=position.subtract(previous),target=gates.get(next);
        double t=segment.lengthSqr()<1e-9?0:Math.clamp(target.subtract(previous).dot(segment)/segment.lengthSqr(),0,1);
        if(previous.add(segment.scale(t)).distanceToSqr(target)<=9) next++;
        previous=position;
    }
    public boolean finished() { return next==gates.size(); }
    public int ticks() {return ticks;}
    public int next() {return next;}
    public String hud() {
        if(finished()) return String.format(java.util.Locale.ROOT,"FINISH | %.2f s",ticks/20.0);
        Vec3 target=gates.get(next);
        return String.format(java.util.Locale.ROOT,"RACE %d/%d | %.1f s | GATE %.0f %.0f %.0f",
                next+1,gates.size(),ticks/20.0,target.x,target.y,target.z);
    }
}
