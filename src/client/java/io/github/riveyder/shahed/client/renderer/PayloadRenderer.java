package io.github.riveyder.shahed.client.renderer;
import io.github.riveyder.shahed.entity.PayloadEntity;
import io.github.riveyder.shahed.client.model.PayloadModel;
import io.github.riveyder.shahed.ShahedMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
public final class PayloadRenderer extends EntityRenderer<PayloadEntity,DroneRenderState>{
    private final PayloadModel model;
    public PayloadRenderer(EntityRendererProvider.Context c){super(c);model=new PayloadModel(c.bakeLayer(PayloadModel.LAYER));shadowRadius=.15F;}
    @Override public DroneRenderState createRenderState(){return new DroneRenderState();}
    @Override public void submit(DroneRenderState s,PoseStack p,SubmitNodeCollector c,CameraRenderState camera){
        p.pushPose();p.scale(-1,-1,1);p.translate(0,-1.5,0);
        c.order(0).submitModel(model,s,p,RenderTypes.entityCutout(ShahedMod.id("textures/entity/arcade_payload.png")),s.lightCoords,OverlayTexture.NO_OVERLAY,-1,null,s.outlineColor,null);
        p.popPose();super.submit(s,p,c,camera);
    }
}
