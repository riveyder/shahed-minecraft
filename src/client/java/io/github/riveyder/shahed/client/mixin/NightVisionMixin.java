package io.github.riveyder.shahed.client.mixin;

import io.github.riveyder.shahed.client.FpvController;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Camera-only lightmap effect, never a potion or a server-side player modification. */
@Mixin(LightmapRenderStateExtractor.class)
abstract class NightVisionMixin {
    @Unique private boolean shahed$wasEnabled;
    @Inject(method="extract",at=@At("TAIL"))
    private void shahed$nightVision(LightmapRenderState state,float partial,CallbackInfo callback) {
        boolean enabled=FpvController.isActive() && FpvController.settings().nightVision;
        if(enabled) {
            state.nightVisionEffectIntensity=1;
            state.nightVisionColor=new Vector3f(.35F,1,.5F);
        }
        if(enabled||shahed$wasEnabled) state.needsUpdate=true;
        shahed$wasEnabled=enabled;
    }
}
