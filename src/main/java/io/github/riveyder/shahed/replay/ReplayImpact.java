package io.github.riveyder.shahed.replay;

import net.minecraft.world.phys.Vec3;

/** Immutable visual event, never a command to damage the world. */
public record ReplayImpact(Vec3 position, boolean explosion, boolean particles, boolean sound, float power) {
    public ReplayImpact {
        if(position==null||!Double.isFinite(position.x)||!Double.isFinite(position.y)||!Double.isFinite(position.z)
                ||!Float.isFinite(power)||power<0||power>12) throw new IllegalArgumentException("Replay impact");
    }
    public static ReplayImpact none(Vec3 position) {return new ReplayImpact(position,false,false,false,0);}
}
