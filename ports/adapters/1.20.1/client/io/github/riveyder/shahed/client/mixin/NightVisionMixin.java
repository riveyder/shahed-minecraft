package io.github.riveyder.shahed.client.mixin;
import io.github.riveyder.shahed.client.FpvController;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(LightTexture.class)
abstract class NightVisionMixin {
 @Redirect(method="updateLightTexture",at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;hasEffect(Lnet/minecraft/world/effect/MobEffect;)Z"))
 private boolean shahed$nightVision(LocalPlayer p,MobEffect effect){return effect==MobEffects.NIGHT_VISION&&FpvController.isActive()&&FpvController.settings().nightVision||p.hasEffect(effect);}
}
