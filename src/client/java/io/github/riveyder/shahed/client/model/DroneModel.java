package io.github.riveyder.shahed.client.model;

import io.github.riveyder.shahed.ShahedMod;
import io.github.riveyder.shahed.client.renderer.DroneRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Original, editable 17-part delta-wing prototype. All dimensions are model pixels. */
public final class DroneModel extends EntityModel<DroneRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ShahedMod.id("drone"), "main");

    public DroneModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot().addOrReplaceChild("drone",
                CubeListBuilder.create(), PartPose.offset(0, 16, 0));
        box(root, "fuselage", 0, 0, -2, -2, -8, 4, 4, 17);
        box(root, "nose", 44, 0, -1.5F, -1.5F, -12, 3, 3, 4);
        box(root, "nose_tip", 60, 0, -1, -1, -15, 2, 2, 3);
        box(root, "left_wing_root", 0, 24, -7, -0.5F, -3, 5, 1, 10);
        box(root, "left_wing_mid", 32, 24, -11, -0.5F, 1, 4, 1, 8);
        box(root, "left_wing_tip", 58, 24, -15, -0.5F, 5, 4, 1, 4);
        box(root, "right_wing_root", 0, 24, 2, -0.5F, -3, 5, 1, 10);
        box(root, "right_wing_mid", 32, 24, 7, -0.5F, 1, 4, 1, 8);
        box(root, "right_wing_tip", 58, 24, 11, -0.5F, 5, 4, 1, 4);
        box(root, "tail_fin", 76, 24, -0.5F, -6, 5, 1, 5, 4);
        box(root, "engine", 0, 38, -1.5F, -1.5F, 9, 3, 3, 4);
        box(root, "propeller_hub", 16, 38, -1, -1, 13, 2, 2, 2);
        box(root, "propeller_horizontal", 0, 48, -7, -0.5F, 14.5F, 14, 1, 1);
        box(root, "propeller_vertical", 32, 48, -0.5F, -7, 14.5F, 1, 14, 1);
        box(root, "left_skid", 44, 38, -3, 2, -2, 1, 6, 8);
        box(root, "right_skid", 44, 38, 2, 2, -2, 1, 6, 8);
        box(root, "antenna", 66, 38, -0.5F, -6, 0, 1, 4, 1);
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static void box(PartDefinition root, String name, int u, int v,
                            float x, float y, float z, float width, float height, float depth) {
        root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v)
                .addBox(x, y, z, width, height, depth), PartPose.ZERO);
    }
}
