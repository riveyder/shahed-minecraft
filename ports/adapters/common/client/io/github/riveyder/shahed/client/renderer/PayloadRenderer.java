package io.github.riveyder.shahed.client.renderer;
import io.github.riveyder.shahed.entity.PayloadEntity;
import io.github.riveyder.shahed.client.model.PayloadModel;
import io.github.riveyder.shahed.ShahedMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
public final class PayloadRenderer extends EntityRenderer<PayloadEntity>{
 private final PayloadModel model;
 public PayloadRenderer(EntityRendererProvider.Context c){super(c);model=new PayloadModel(c.bakeLayer(PayloadModel.LAYER));shadowRadius=.15F;}
 @Override public ResourceLocation getTextureLocation(PayloadEntity e){return ShahedMod.id("textures/entity/arcade_payload.png");}
 @Override public void render(PayloadEntity e,float yaw,float t,PoseStack p,MultiBufferSource b,int light){p.pushPose();p.scale(-1,-1,1);p.translate(0,-1.5,0);model.renderToBuffer(p,b.getBuffer(RenderType.entityCutout(getTextureLocation(e))),light,OverlayTexture.NO_OVERLAY,1,1,1,1);p.popPose();super.render(e,yaw,t,p,b,light);}
}
