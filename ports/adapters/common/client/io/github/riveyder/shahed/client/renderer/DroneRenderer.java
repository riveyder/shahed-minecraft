package io.github.riveyder.shahed.client.renderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.riveyder.shahed.ShahedMod;
import io.github.riveyder.shahed.client.FpvController;
import io.github.riveyder.shahed.client.model.DroneModel;
import io.github.riveyder.shahed.entity.DroneEntity;
import io.github.riveyder.shahed.compat.client.LegacyModel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
public final class DroneRenderer extends EntityRenderer<DroneEntity> {
 private final ResourceLocation texture;private final LegacyModel<DroneRenderState> model;
    public DroneRenderer(EntityRendererProvider.Context context){this(context,io.github.riveyder.shahed.entity.AircraftKind.SHAHED);}
    public DroneRenderer(EntityRendererProvider.Context context,io.github.riveyder.shahed.entity.AircraftKind kind){
        super(context);
        texture=ShahedMod.id("textures/entity/"+(kind==io.github.riveyder.shahed.entity.AircraftKind.SHAHED?"drone":kind.id)+".png");
        model=switch(kind){
            case SHAHED->new DroneModel(context.bakeLayer(DroneModel.LAYER));
            case QUAD->new io.github.riveyder.shahed.client.model.FleetModel(context.bakeLayer(io.github.riveyder.shahed.client.model.FleetModel.QUAD),false,true);
            case CONTACT->new io.github.riveyder.shahed.client.model.FleetModel(context.bakeLayer(io.github.riveyder.shahed.client.model.FleetModel.CONTACT),false,false);
            case JET->new io.github.riveyder.shahed.client.model.FleetModel(context.bakeLayer(io.github.riveyder.shahed.client.model.FleetModel.JET),true,false);
        };
        shadowRadius=kind==io.github.riveyder.shahed.entity.AircraftKind.SHAHED?1.65F:kind.hover?.65F:1;
    }


 @Override public ResourceLocation getTextureLocation(DroneEntity entity){return texture;}
 @Override public boolean shouldRender(DroneEntity e,Frustum f,double x,double y,double z){return !FpvController.isCameraDrone(e)&&super.shouldRender(e,f,x,y,z);}
 @Override public void render(DroneEntity e,float yaw,float t,PoseStack poses,MultiBufferSource buffers,int light){
  var state=new DroneRenderState();state.tint=e.kind()==io.github.riveyder.shahed.entity.AircraftKind.SHAHED?e.getTint():-1;
  state.loaded=e.hasPayload();state.propellerAngle=e.getPropellerAngle(t);state.yaw=e.getYRot(t);state.pitch=e.getXRot(t);state.roll=e.getRoll(t);
  model.setupAnim(state);poses.pushPose();poses.mulPose(Axis.YP.rotationDegrees(180-state.yaw));poses.mulPose(Axis.XP.rotationDegrees(-state.pitch));poses.mulPose(Axis.ZP.rotationDegrees(state.roll));poses.scale(-1,-1,1);poses.translate(0,-1.5,0);
  model.renderToBuffer(poses,buffers.getBuffer(RenderType.entityCutout(texture)),light,OverlayTexture.NO_OVERLAY,((state.tint>>16)&255)/255F,((state.tint>>8)&255)/255F,(state.tint&255)/255F,1);
  poses.popPose();super.render(e,yaw,t,poses,buffers,light);
 }
}
