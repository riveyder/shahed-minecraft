package io.github.riveyder.shahed.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.riveyder.shahed.ShahedMod;
import io.github.riveyder.shahed.client.model.DroneModel;
import io.github.riveyder.shahed.entity.DroneEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Minecraft 26.2 rendering adapter. Flight and FPV do not belong in this class. */
public final class DroneRenderer extends EntityRenderer<DroneEntity, DroneRenderState> {
    private static final Identifier TEXTURE = ShahedMod.id("textures/entity/drone.png");
    private final DroneModel model;

    public DroneRenderer(EntityRendererProvider.Context context) {
        super(context);
        model = new DroneModel(context.bakeLayer(DroneModel.LAYER));
        shadowRadius = 0.7F;
    }

    @Override
    public DroneRenderState createRenderState() {
        return new DroneRenderState();
    }

    @Override
    public void extractRenderState(DroneEntity entity, DroneRenderState state, float tickProgress) {
        super.extractRenderState(entity, state, tickProgress);
        state.yaw = entity.getYRot(tickProgress);
    }

    @Override
    public void submit(DroneRenderState state, PoseStack poses, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        poses.pushPose();
        // Model nose is -Z; Minecraft yaw 0 faces +Z, yaw 90 faces -X.
        poses.mulPose(Axis.YP.rotationDegrees(180.0F - state.yaw));
        poses.scale(-1.0F, -1.0F, 1.0F);
        poses.translate(0.0F, -1.5F, 0.0F);
        collector.order(0).submitModel(model, state, poses,
                RenderTypes.entityCutout(TEXTURE), state.lightCoords,
                OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor, null);
        poses.popPose();
        super.submit(state, poses, collector, camera);
    }
}
