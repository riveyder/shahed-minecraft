package io.github.riveyder.shahed.compat;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
public record ValueInput(CompoundTag tag){
    public Optional<String> getString(String key){return tag.contains(key,8)?Optional.of(tag.getString(key)):Optional.empty();}
    public boolean getBooleanOr(String k,boolean d){return tag.contains(k)?tag.getBoolean(k):d;}
    public int getIntOr(String k,int d){return tag.contains(k)?tag.getInt(k):d;}
    public float getFloatOr(String k,float d){return tag.contains(k)?tag.getFloat(k):d;}
}
