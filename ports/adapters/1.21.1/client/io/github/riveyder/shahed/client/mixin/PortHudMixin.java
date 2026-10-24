package io.github.riveyder.shahed.client.mixin;
import net.minecraft.client.gui.Gui;
import io.github.riveyder.shahed.client.FpvController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Gui.class)
abstract class PortHudMixin {
 @Inject(method={"renderCrosshair","renderHotbarAndDecorations","renderExperienceLevel","renderPlayerHealth","renderVehicleHealth","renderExperienceBar","renderJumpMeter","renderSelectedItemName","renderTextureOverlay","renderVignette","renderPortalOverlay","renderSpyglassOverlay"},at=@At("HEAD"),cancellable=true)
 private void shahed$hideBodyHud(CallbackInfo callback){if(FpvController.isActive())callback.cancel();}
}
