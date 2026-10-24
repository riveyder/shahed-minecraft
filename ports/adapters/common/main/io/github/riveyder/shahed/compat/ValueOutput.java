package io.github.riveyder.shahed.compat;
import net.minecraft.nbt.CompoundTag;
public record ValueOutput(CompoundTag tag){
    public void putString(String k,String v){tag.putString(k,v);}
    public void putBoolean(String k,boolean v){tag.putBoolean(k,v);}
    public void putInt(String k,int v){tag.putInt(k,v);}
    public void putFloat(String k,float v){tag.putFloat(k,v);}
}
