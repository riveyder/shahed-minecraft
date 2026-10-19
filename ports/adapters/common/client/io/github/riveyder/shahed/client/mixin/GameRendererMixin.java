package io.github.riveyder.shahed.client.mixin;

import io.github.riveyder.shahed.client.FpvController;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    @Inject(method = {"renderItemInHand", "bobHurt", "bobView"}, at = @At("HEAD"), cancellable = true)
    private void shahed$hideBodyEffects(CallbackInfo callback) {
        if (FpvController.isActive()) callback.cancel();
    }
    @Inject(method="getFov",at=@At("RETURN"),cancellable=true)
    private void shahed$fov(net.minecraft.client.Camera camera,float partial,boolean changing,org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Double> callback){
        if(FpvController.isActive()||io.github.riveyder.shahed.client.ReplayController.isPlaying())callback.setReturnValue((double)FpvController.settings().cameraFov);
    }
    @Inject(method="getNightVisionScale",at=@At("HEAD"),cancellable=true)
    private static void shahed$nightScale(net.minecraft.world.entity.LivingEntity entity,float partial,org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> callback){
        if(FpvController.isActive()&&FpvController.settings().nightVision)callback.setReturnValue(1F);
    }
}
