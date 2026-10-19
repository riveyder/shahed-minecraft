package io.github.riveyder.shahed.compat;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
public final class Compat {
    public static int clamp(int v,int a,int b){return Math.min(b,Math.max(a,v));}
    public static float clamp(float v,float a,float b){return Math.min(b,Math.max(a,v));}
    public static double clamp(double v,double a,double b){return Math.min(b,Math.max(a,v));}
    public static <T>T last(List<T> list){return list.get(list.size()-1);}
    public static ResourceLocation id(String namespace,String path){return new ResourceLocation(namespace,path);}
    public static Iterable<VoxelShape> colliders(Entity entity,Level level,AABB bounds){
        var shapes=new ArrayList<VoxelShape>();level.getBlockCollisions(entity,bounds).forEach(shapes::add);
        shapes.addAll(level.getEntityCollisions(entity,bounds));return shapes;
    }
}
