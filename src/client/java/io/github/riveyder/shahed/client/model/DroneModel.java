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

/** Original, editable fixed-wing drone. Geometry/UV source: docs/model/drone-model.json. */
public final class DroneModel extends EntityModel<DroneRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ShahedMod.id("drone"), "main");

    public DroneModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot().addOrReplaceChild("drone",
                CubeListBuilder.create(), PartPose.offset(0, 16, 0));
        box(root, "fuselage_core", 0, 0, -3, -2, -17, 6, 5, 34);
        box(root, "fuselage_upper_bevel", 82, 0, -2, -3, -16, 4, 1, 32);
        box(root, "fuselage_lower_bevel", 156, 0, -2, 3, -15, 4, 1, 31);
        box(root, "left_fuselage_chine", 0, 41, -4, -1, -14, 1, 3, 29);
        box(root, "right_fuselage_chine", 0, 41, 3, -1, -14, 1, 3, 29);
        box(root, "forward_taper", 62, 41, -3, -2, -23, 6, 4, 6);
        box(root, "nose_taper", 88, 41, -2, -2, -27, 4, 4, 4);
        box(root, "camera_bezel", 106, 41, -2, -2, -29, 4, 3, 2);
        box(root, "camera_lens", 120, 41, -1, -1, -30, 2, 2, 1);
        box(root, "left_wing_01", 128, 41, -6, 0, -13, 2, 1, 30);
        box(root, "left_wing_02", 194, 41, -8, 0, -11, 2, 1, 28);
        box(root, "left_wing_03", 0, 75, -10, 0, -9, 2, 1, 26);
        box(root, "left_wing_04", 58, 75, -12, 0, -7, 2, 1, 24);
        box(root, "left_wing_05", 112, 75, -14, 0, -5, 2, 1, 22);
        box(root, "left_wing_06", 162, 75, -16, 0, -3, 2, 1, 20);
        box(root, "left_wing_07", 208, 75, -18, 0, -1, 2, 1, 18);
        box(root, "left_wing_08", 0, 104, -20, 0, 1, 2, 1, 16);
        box(root, "left_wing_09", 38, 104, -22, 0, 3, 2, 1, 14);
        box(root, "left_wing_10", 72, 104, -24, 0, 5, 2, 1, 12);
        box(root, "left_wing_11", 102, 104, -26, 0, 7, 2, 1, 10);
        box(root, "left_wing_service_panel", 128, 104, -10, -1, 2, 5, 1, 10);
        box(root, "left_aileron", 160, 104, -24, -1, 14, 10, 1, 2);
        box(root, "left_wing_root_fairing", 186, 104, -5, -1, -10, 2, 1, 25);
        box(root, "right_wing_01", 128, 41, 4, 0, -13, 2, 1, 30);
        box(root, "right_wing_02", 194, 41, 6, 0, -11, 2, 1, 28);
        box(root, "right_wing_03", 0, 75, 8, 0, -9, 2, 1, 26);
        box(root, "right_wing_04", 58, 75, 10, 0, -7, 2, 1, 24);
        box(root, "right_wing_05", 112, 75, 12, 0, -5, 2, 1, 22);
        box(root, "right_wing_06", 162, 75, 14, 0, -3, 2, 1, 20);
        box(root, "right_wing_07", 208, 75, 16, 0, -1, 2, 1, 18);
        box(root, "right_wing_08", 0, 104, 18, 0, 1, 2, 1, 16);
        box(root, "right_wing_09", 38, 104, 20, 0, 3, 2, 1, 14);
        box(root, "right_wing_10", 72, 104, 22, 0, 5, 2, 1, 12);
        box(root, "right_wing_11", 102, 104, 24, 0, 7, 2, 1, 10);
        box(root, "right_wing_service_panel", 128, 104, 5, -1, 2, 5, 1, 10);
        box(root, "right_aileron", 160, 104, 14, -1, 14, 10, 1, 2);
        box(root, "right_wing_root_fairing", 186, 104, 3, -1, -10, 2, 1, 25);
        box(root, "rear_collar", 0, 132, -3, -2, 17, 6, 5, 3);
        box(root, "engine_motor", 20, 132, -2, -2, 20, 4, 4, 5);
        box(root, "left_motor_mount", 40, 132, -3, -1, 20, 1, 2, 7);
        box(root, "right_motor_mount", 40, 132, 2, -1, 20, 1, 2, 7);
        box(root, "motor_top_mount", 58, 132, -1, -3, 20, 2, 1, 6);
        box(root, "tail_fin_base", 76, 132, -1, -6, 10, 2, 5, 10);
        box(root, "tail_fin_top", 102, 132, -1, -10, 14, 2, 4, 6);
        box(root, "tail_rudder", 120, 132, -1, -9, 20, 2, 8, 2);
        box(root, "propeller_hub", 130, 132, -1, -1, 25, 2, 2, 4);
        box(root, "propeller_left", 144, 132, -10, -1, 28, 9, 1, 1);
        box(root, "propeller_right", 144, 132, 1, 0, 28, 9, 1, 1);
        box(root, "propeller_upper", 166, 132, 0, -8, 28, 1, 7, 1);
        box(root, "propeller_lower", 166, 132, -1, 1, 28, 1, 7, 1);
        box(root, "propeller_left_tip", 172, 132, -10, -1, 28, 2, 1, 1);
        box(root, "propeller_right_tip", 172, 132, 8, 0, 28, 2, 1, 1);
        box(root, "battery_hatch", 180, 132, -2, -4, -10, 4, 1, 11);
        box(root, "avionics_hatch", 212, 132, -2, -4, 3, 4, 1, 6);
        box(root, "status_light", 234, 132, 0, -5, 5, 1, 1, 2);
        box(root, "antenna_mount", 242, 132, -1, -5, -1, 2, 2, 3);
        box(root, "antenna_mast", 0, 149, 0, -11, 0, 1, 6, 1);
        box(root, "antenna_cap", 6, 149, -1, -11, -1, 3, 1, 3);
        box(root, "left_skid_front_mount", 20, 149, -5, 3, -10, 1, 4, 2);
        box(root, "left_skid_rear_mount", 20, 149, -5, 3, 9, 1, 4, 2);
        box(root, "left_landing_skid", 28, 149, -5, 7, -12, 1, 1, 25);
        box(root, "right_skid_front_mount", 20, 149, 4, 3, -10, 1, 4, 2);
        box(root, "right_skid_rear_mount", 20, 149, 4, 3, 9, 1, 4, 2);
        box(root, "right_landing_skid", 28, 149, 4, 7, -12, 1, 1, 25);
        return LayerDefinition.create(mesh, 256, 256);
    }

    private static void box(PartDefinition root, String name, int u, int v,
                            float x, float y, float z, float width, float height, float depth) {
        root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v)
                .addBox(x, y, z, width, height, depth), PartPose.ZERO);
    }
}
