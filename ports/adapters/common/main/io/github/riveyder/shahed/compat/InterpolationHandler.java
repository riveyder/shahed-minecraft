package io.github.riveyder.shahed.compat;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.Mth;
public final class InterpolationHandler{
    private final Entity entity;private int remaining;private double x,y,z;private float yaw,pitch;
    public InterpolationHandler(Entity entity,int ignored){this.entity=entity;}
    public void set(double x,double y,double z,float yaw,float pitch,int steps){this.x=x;this.y=y;this.z=z;this.yaw=yaw;this.pitch=pitch;remaining=Math.max(1,steps);}
    public void interpolate(){if(remaining>0){entity.setPos(entity.getX()+(x-entity.getX())/remaining,entity.getY()+(y-entity.getY())/remaining,entity.getZ()+(z-entity.getZ())/remaining);
        entity.setYRot(entity.getYRot()+Mth.wrapDegrees(yaw-entity.getYRot())/remaining);entity.setXRot(entity.getXRot()+(pitch-entity.getXRot())/remaining);remaining--;}}
}
