package io.github.riveyder.shahed.mixin;
import io.github.riveyder.shahed.compat.PortExplosion;
import java.util.List;
import net.minecraft.world.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(Explosion.class)
public abstract class PortExplosionMixin {
 // Old Explosion has no per-entity damage policy. Our adapter applies that policy separately.
 @Redirect(method="explode",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
 private List<Entity> shahed$policyEntities(Level level,Entity source,AABB box){return (Object)this instanceof PortExplosion?List.of():level.getEntities(source,box);}
}
