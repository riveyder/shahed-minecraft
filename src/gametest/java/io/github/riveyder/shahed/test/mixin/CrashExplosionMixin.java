package io.github.riveyder.shahed.test.mixin;
import io.github.riveyder.shahed.test.CrashEffectsProbe;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerExplosion.class)
public abstract class CrashExplosionMixin {
    @Inject(method="explode",at=@At("RETURN"))
    private void explosion(CallbackInfoReturnable<Integer> cir) {
        var probe=CrashEffectsProbe.active();if(probe!=null) probe.explosions++;
    }
}
