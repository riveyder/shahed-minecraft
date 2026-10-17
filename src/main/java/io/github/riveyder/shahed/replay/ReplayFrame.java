package io.github.riveyder.shahed.replay;

import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

public record ReplayFrame(double x,double y,double z,float yaw,float pitch,float roll) {
    public ReplayFrame {
        if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)
                ||!Float.isFinite(yaw)||!Float.isFinite(pitch)||!Float.isFinite(roll))
            throw new IllegalArgumentException("Non-finite replay frame");
    }
    public Vec3 position() { return new Vec3(x,y,z); }
    public ReplayFrame blend(ReplayFrame next,float alpha) {
        return new ReplayFrame(Mth.lerp(alpha,x,next.x),Mth.lerp(alpha,y,next.y),Mth.lerp(alpha,z,next.z),
                yaw+Mth.wrapDegrees(next.yaw-yaw)*alpha,Mth.lerp(alpha,pitch,next.pitch),Mth.lerp(alpha,roll,next.roll));
    }
}
