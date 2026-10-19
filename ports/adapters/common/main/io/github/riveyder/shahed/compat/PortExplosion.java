package io.github.riveyder.shahed.compat;
import net.minecraft.world.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
public final class PortExplosion extends Explosion {
 public PortExplosion(Level level,Entity source,DamageSource damage,ExplosionDamageCalculator calculator,double x,double y,double z,float power,BlockInteraction interaction){super(level,source,damage,calculator,x,y,z,power,false,interaction);}
}
