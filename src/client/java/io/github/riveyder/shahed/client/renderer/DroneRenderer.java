package io.github.riveyder.shahed.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.riveyder.shahed.ShahedMod;
import io.github.riveyder.shahed.client.FpvController;
import io.github.riveyder.shahed.client.model.DroneModel;
import io.github.riveyder.shahed.entity.DroneEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Drone model adapter; renders the same pitch and bank as the FPV camera. */
public final class DroneRenderer extends EntityRenderer<DroneEntity, DroneRenderState> {
    private final Identifier texture;
    private final net.minecraft.client.model.EntityModel<DroneRenderState> model;

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

    @Override
    public DroneRenderState createRenderState() {
        return new DroneRenderState();
    }

    @Override
    public boolean shouldRender(DroneEntity entity, Frustum frustum, double x, double y, double z) {
        return !FpvController.isCameraDrone(entity) && super.shouldRender(entity, frustum, x, y, z);
    }

    @Override
    public void extractRenderState(DroneEntity entity, DroneRenderState state, float tickProgress) {
        super.extractRenderState(entity, state, tickProgress);
        state.tint=entity.kind()==io.github.riveyder.shahed.entity.AircraftKind.SHAHED?entity.getTint():-1;
        state.loaded=entity.hasPayload();
        state.propellerAngle=entity.getPropellerAngle(tickProgress);
        state.yaw = entity.getYRot(tickProgress);
        state.pitch = entity.getXRot(tickProgress);
        state.roll = entity.getRoll(tickProgress);
    }

    @Override
    public void submit(DroneRenderState state, PoseStack poses, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        poses.pushPose();
        // Model nose is -Z; Minecraft yaw 0 faces +Z, yaw 90 faces -X.
        poses.mulPose(Axis.YP.rotationDegrees(180.0F - state.yaw));
        poses.mulPose(Axis.XP.rotationDegrees(-state.pitch));
        poses.mulPose(Axis.ZP.rotationDegrees(state.roll));
        poses.scale(-1.0F, -1.0F, 1.0F);
        poses.translate(0.0F, -1.5F, 0.0F);
        collector.order(0).submitModel(model, state, poses,
                RenderTypes.entityCutout(texture), state.lightCoords,
                OverlayTexture.NO_OVERLAY, state.tint, null, state.outlineColor, null);
        poses.popPose();
        super.submit(state, poses, collector, camera);
    }
}
