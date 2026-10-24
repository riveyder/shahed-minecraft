package io.github.riveyder.shahed.client.model;
import io.github.riveyder.shahed.ShahedMod;
import io.github.riveyder.shahed.client.renderer.DroneRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
/** Original geometry exported with docs/model/generate_fleet.py; editable sources in assets-work. */
public final class FleetModel extends EntityModel<DroneRenderState> {
    public static final ModelLayerLocation QUAD=new ModelLayerLocation(ShahedMod.id("quad"),"main");
    public static final ModelLayerLocation CONTACT=new ModelLayerLocation(ShahedMod.id("contact_quad"),"main");
    public static final ModelLayerLocation JET=new ModelLayerLocation(ShahedMod.id("jet"),"main");
    private final ModelPart[] rotors;
    private final ModelPart payload;
    public FleetModel(ModelPart root,boolean jet,boolean reusable){super(root);
        rotors=jet?new ModelPart[0]:new ModelPart[]{root.getChild("rotor_LF"),root.getChild("rotor_RF"),root.getChild("rotor_LB"),root.getChild("rotor_RB")};
        payload=reusable?root.getChild("payload"):null;
    }
    @Override public void setupAnim(DroneRenderState state){super.setupAnim(state);
        for(int i=0;i<rotors.length;i++)rotors[i].yRot=state.propellerAngle*((i==0||i==3)?1:-1);
        if(payload!=null)payload.visible=state.loaded;
    }
    public static LayerDefinition quadLayer() {
        MeshDefinition mesh=new MeshDefinition();PartDefinition root=mesh.getRoot();
        PartDefinition rotor_LF=root.addOrReplaceChild("rotor_LF",CubeListBuilder.create(),PartPose.offset(-7.5F,14F,-7.5F));
        PartDefinition rotor_LB=root.addOrReplaceChild("rotor_LB",CubeListBuilder.create(),PartPose.offset(-7.5F,14F,7.5F));
        PartDefinition rotor_RF=root.addOrReplaceChild("rotor_RF",CubeListBuilder.create(),PartPose.offset(7.5F,14F,-7.5F));
        PartDefinition rotor_RB=root.addOrReplaceChild("rotor_RB",CubeListBuilder.create(),PartPose.offset(7.5F,14F,7.5F));
        PartDefinition payload=root.addOrReplaceChild("payload",CubeListBuilder.create(),PartPose.offset(0F,0F,0F));
        box(root,"part_0",2,2,-3F,18F,-5F,6F,1F,10F); // carbon deck
        box(root,"part_1",36,2,-2.5F,16F,-3F,5F,2F,6F); // electronics
        box(root,"part_2",60,2,-2F,14F,-1F,4F,2F,5F); // battery
        box(root,"part_3",80,2,-2.25F,13.75F,1F,4.5F,0.25F,1F); // battery strap
        box(root,"part_4",93,2,-1.5F,16F,-6.5F,3F,3F,2F); // camera mount
        box(root,"part_5",105,2,-1F,16.5F,-7F,2F,2F,0.5F); // FPV lens
        box(root,"part_6",112,2,-0.25F,10F,4F,0.5F,6F,0.5F); // antenna stalk
        box(root,"part_7",116,2,-1.5F,9F,3F,3F,1F,2F); // antenna head
        box(root,"part_8",128,2,-3.625F,18F,-3.625F,2F,1.2F,2F); // arm rotor_LF0
        box(root,"part_9",138,2,-5.35F,18F,-5.35F,2F,1.2F,2F); // arm rotor_LF1
        box(root,"part_10",148,2,-7.075F,18F,-7.075F,2F,1.2F,2F); // arm rotor_LF2
        box(root,"part_11",158,2,-8.9F,16F,-8.9F,2.8F,2F,2.8F); // motor base rotor_LF
        box(root,"part_12",172,2,-8.2F,14F,-8.2F,1.4F,2F,1.4F); // motor shaft rotor_LF
        box(rotor_LF,"part_13",180,2,-5F,-0.2F,-0.55F,10F,0.4F,1.1F); // blade A rotor_LF
        box(rotor_LF,"part_14",205,2,-0.55F,-0.2F,-5F,1.1F,0.4F,10F); // blade B rotor_LF
        box(rotor_LF,"part_15",230,2,-0.7F,-0.6F,-0.7F,1.4F,0.6F,1.4F); // hub rotor_LF
        box(root,"part_16",238,2,-3.625F,18F,1.625F,2F,1.2F,2F); // arm rotor_LB0
        box(root,"part_17",248,2,-5.35F,18F,3.3499999999999996F,2F,1.2F,2F); // arm rotor_LB1
        box(root,"part_18",258,2,-7.075F,18F,5.075F,2F,1.2F,2F); // arm rotor_LB2
        box(root,"part_19",268,2,-8.9F,16F,6.1F,2.8F,2F,2.8F); // motor base rotor_LB
        box(root,"part_20",282,2,-8.2F,14F,6.8F,1.4F,2F,1.4F); // motor shaft rotor_LB
        box(rotor_LB,"part_21",290,2,-5F,-0.2F,-0.55F,10F,0.4F,1.1F); // blade A rotor_LB
        box(rotor_LB,"part_22",315,2,-0.55F,-0.2F,-5F,1.1F,0.4F,10F); // blade B rotor_LB
        box(rotor_LB,"part_23",340,2,-0.7F,-0.6F,-0.7F,1.4F,0.6F,1.4F); // hub rotor_LB
        box(root,"part_24",348,2,1.625F,18F,-3.625F,2F,1.2F,2F); // arm rotor_RF0
        box(root,"part_25",358,2,3.3499999999999996F,18F,-5.35F,2F,1.2F,2F); // arm rotor_RF1
        box(root,"part_26",368,2,5.075F,18F,-7.075F,2F,1.2F,2F); // arm rotor_RF2
        box(root,"part_27",378,2,6.1F,16F,-8.9F,2.8F,2F,2.8F); // motor base rotor_RF
        box(root,"part_28",392,2,6.8F,14F,-8.2F,1.4F,2F,1.4F); // motor shaft rotor_RF
        box(rotor_RF,"part_29",400,2,-5F,-0.2F,-0.55F,10F,0.4F,1.1F); // blade A rotor_RF
        box(rotor_RF,"part_30",425,2,-0.55F,-0.2F,-5F,1.1F,0.4F,10F); // blade B rotor_RF
        box(rotor_RF,"part_31",450,2,-0.7F,-0.6F,-0.7F,1.4F,0.6F,1.4F); // hub rotor_RF
        box(root,"part_32",458,2,1.625F,18F,1.625F,2F,1.2F,2F); // arm rotor_RB0
        box(root,"part_33",468,2,3.3499999999999996F,18F,3.3499999999999996F,2F,1.2F,2F); // arm rotor_RB1
        box(root,"part_34",478,2,5.075F,18F,5.075F,2F,1.2F,2F); // arm rotor_RB2
        box(root,"part_35",488,2,6.1F,16F,6.1F,2.8F,2F,2.8F); // motor base rotor_RB
        box(root,"part_36",502,2,6.8F,14F,6.8F,1.4F,2F,1.4F); // motor shaft rotor_RB
        box(rotor_RB,"part_37",2,15,-5F,-0.2F,-0.55F,10F,0.4F,1.1F); // blade A rotor_RB
        box(rotor_RB,"part_38",27,15,-0.55F,-0.2F,-5F,1.1F,0.4F,10F); // blade B rotor_RB
        box(rotor_RB,"part_39",52,15,-0.7F,-0.6F,-0.7F,1.4F,0.6F,1.4F); // hub rotor_RB
        box(root,"part_40",60,15,-3.4F,19F,-3.9F,0.8F,4F,0.8F); // landing leg
        box(root,"part_41",66,15,-3.4F,19F,3.1F,0.8F,4F,0.8F); // landing leg
        box(root,"part_42",72,15,-3.5F,23F,-5F,1F,1F,10F); // skid
        box(root,"part_43",96,15,2.6F,19F,-3.9F,0.8F,4F,0.8F); // landing leg
        box(root,"part_44",102,15,2.6F,19F,3.1F,0.8F,4F,0.8F); // landing leg
        box(root,"part_45",108,15,2.5F,23F,-5F,1F,1F,10F); // skid
        box(root,"part_46",132,15,-1F,19F,-1F,2F,1F,2F); // payload socket
        box(payload,"part_47",142,15,-2F,20F,-2.5F,4F,2.5F,5F); // arcade payload casing
        box(payload,"part_48",162,15,-2.1F,20.8F,-2.6F,4.2F,0.7F,5.2F); // payload band
        box(payload,"part_49",183,15,-0.6F,20.5F,-2.8F,1.2F,1F,0.2F); // payload indicator
        return LayerDefinition.create(mesh,512,512);
    }
    public static LayerDefinition contactLayer() {
        MeshDefinition mesh=new MeshDefinition();PartDefinition root=mesh.getRoot();
        PartDefinition rotor_LF=root.addOrReplaceChild("rotor_LF",CubeListBuilder.create(),PartPose.offset(-7.5F,14F,-7.5F));
        PartDefinition rotor_LB=root.addOrReplaceChild("rotor_LB",CubeListBuilder.create(),PartPose.offset(-7.5F,14F,7.5F));
        PartDefinition rotor_RF=root.addOrReplaceChild("rotor_RF",CubeListBuilder.create(),PartPose.offset(7.5F,14F,-7.5F));
        PartDefinition rotor_RB=root.addOrReplaceChild("rotor_RB",CubeListBuilder.create(),PartPose.offset(7.5F,14F,7.5F));
        box(root,"part_0",2,2,-3F,18F,-5F,6F,1F,10F); // carbon deck
        box(root,"part_1",36,2,-2.5F,16F,-3F,5F,2F,6F); // electronics
        box(root,"part_2",60,2,-2F,14F,-1F,4F,2F,5F); // battery
        box(root,"part_3",80,2,-2.25F,13.75F,1F,4.5F,0.25F,1F); // battery strap
        box(root,"part_4",93,2,-1.5F,16F,-6.5F,3F,3F,2F); // camera mount
        box(root,"part_5",105,2,-1F,16.5F,-7F,2F,2F,0.5F); // FPV lens
        box(root,"part_6",112,2,-0.25F,10F,4F,0.5F,6F,0.5F); // antenna stalk
        box(root,"part_7",116,2,-1.5F,9F,3F,3F,1F,2F); // antenna head
        box(root,"part_8",128,2,-3.625F,18F,-3.625F,2F,1.2F,2F); // arm rotor_LF0
        box(root,"part_9",138,2,-5.35F,18F,-5.35F,2F,1.2F,2F); // arm rotor_LF1
        box(root,"part_10",148,2,-7.075F,18F,-7.075F,2F,1.2F,2F); // arm rotor_LF2
        box(root,"part_11",158,2,-8.9F,16F,-8.9F,2.8F,2F,2.8F); // motor base rotor_LF
        box(root,"part_12",172,2,-8.2F,14F,-8.2F,1.4F,2F,1.4F); // motor shaft rotor_LF
        box(rotor_LF,"part_13",180,2,-5F,-0.2F,-0.55F,10F,0.4F,1.1F); // blade A rotor_LF
        box(rotor_LF,"part_14",205,2,-0.55F,-0.2F,-5F,1.1F,0.4F,10F); // blade B rotor_LF
        box(rotor_LF,"part_15",230,2,-0.7F,-0.6F,-0.7F,1.4F,0.6F,1.4F); // hub rotor_LF
        box(root,"part_16",238,2,-3.625F,18F,1.625F,2F,1.2F,2F); // arm rotor_LB0
        box(root,"part_17",248,2,-5.35F,18F,3.3499999999999996F,2F,1.2F,2F); // arm rotor_LB1
        box(root,"part_18",258,2,-7.075F,18F,5.075F,2F,1.2F,2F); // arm rotor_LB2
        box(root,"part_19",268,2,-8.9F,16F,6.1F,2.8F,2F,2.8F); // motor base rotor_LB
        box(root,"part_20",282,2,-8.2F,14F,6.8F,1.4F,2F,1.4F); // motor shaft rotor_LB
        box(rotor_LB,"part_21",290,2,-5F,-0.2F,-0.55F,10F,0.4F,1.1F); // blade A rotor_LB
        box(rotor_LB,"part_22",315,2,-0.55F,-0.2F,-5F,1.1F,0.4F,10F); // blade B rotor_LB
        box(rotor_LB,"part_23",340,2,-0.7F,-0.6F,-0.7F,1.4F,0.6F,1.4F); // hub rotor_LB
        box(root,"part_24",348,2,1.625F,18F,-3.625F,2F,1.2F,2F); // arm rotor_RF0
        box(root,"part_25",358,2,3.3499999999999996F,18F,-5.35F,2F,1.2F,2F); // arm rotor_RF1
        box(root,"part_26",368,2,5.075F,18F,-7.075F,2F,1.2F,2F); // arm rotor_RF2
        box(root,"part_27",378,2,6.1F,16F,-8.9F,2.8F,2F,2.8F); // motor base rotor_RF
        box(root,"part_28",392,2,6.8F,14F,-8.2F,1.4F,2F,1.4F); // motor shaft rotor_RF
        box(rotor_RF,"part_29",400,2,-5F,-0.2F,-0.55F,10F,0.4F,1.1F); // blade A rotor_RF
        box(rotor_RF,"part_30",425,2,-0.55F,-0.2F,-5F,1.1F,0.4F,10F); // blade B rotor_RF
        box(rotor_RF,"part_31",450,2,-0.7F,-0.6F,-0.7F,1.4F,0.6F,1.4F); // hub rotor_RF
        box(root,"part_32",458,2,1.625F,18F,1.625F,2F,1.2F,2F); // arm rotor_RB0
        box(root,"part_33",468,2,3.3499999999999996F,18F,3.3499999999999996F,2F,1.2F,2F); // arm rotor_RB1
        box(root,"part_34",478,2,5.075F,18F,5.075F,2F,1.2F,2F); // arm rotor_RB2
        box(root,"part_35",488,2,6.1F,16F,6.1F,2.8F,2F,2.8F); // motor base rotor_RB
        box(root,"part_36",502,2,6.8F,14F,6.8F,1.4F,2F,1.4F); // motor shaft rotor_RB
        box(rotor_RB,"part_37",2,15,-5F,-0.2F,-0.55F,10F,0.4F,1.1F); // blade A rotor_RB
        box(rotor_RB,"part_38",27,15,-0.55F,-0.2F,-5F,1.1F,0.4F,10F); // blade B rotor_RB
        box(rotor_RB,"part_39",52,15,-0.7F,-0.6F,-0.7F,1.4F,0.6F,1.4F); // hub rotor_RB
        box(root,"part_40",60,15,-3.4F,19F,-3.9F,0.8F,4F,0.8F); // landing leg
        box(root,"part_41",66,15,-3.4F,19F,3.1F,0.8F,4F,0.8F); // landing leg
        box(root,"part_42",72,15,-3.5F,23F,-5F,1F,1F,10F); // skid
        box(root,"part_43",96,15,2.6F,19F,-3.9F,0.8F,4F,0.8F); // landing leg
        box(root,"part_44",102,15,2.6F,19F,3.1F,0.8F,4F,0.8F); // landing leg
        box(root,"part_45",108,15,2.5F,23F,-5F,1F,1F,10F); // skid
        box(root,"part_46",132,15,-2.5F,19F,-4F,5F,3F,7F); // integrated contact core
        box(root,"part_47",158,15,-3F,19F,-6F,6F,2F,2F); // contact bumper
        box(root,"part_48",176,15,-2.6F,20F,-4.1F,5.2F,0.7F,7.2F); // single use stripe
        return LayerDefinition.create(mesh,512,512);
    }
    public static LayerDefinition jetLayer() {
        MeshDefinition mesh=new MeshDefinition();PartDefinition root=mesh.getRoot();
        box(root,"part_0",2,2,-1F,17F,-21F,2F,2F,4F); // tapered fuselage 0
        box(root,"part_1",16,2,-2F,16F,-17F,4F,4F,5F); // tapered fuselage 1
        box(root,"part_2",36,2,-3F,15F,-12F,6F,6F,8F); // tapered fuselage 2
        box(root,"part_3",66,2,-3.5F,15F,-4F,7F,6F,12F); // tapered fuselage 3
        box(root,"part_4",106,2,-2.5F,16F,8F,5F,5F,7F); // tapered fuselage 4
        box(root,"part_5",132,2,-1.8F,13.5F,-11F,3.6F,1.5F,7F); // sensor canopy
        box(root,"part_6",156,2,-5.0F,18F,-6.0F,2.0F,1.2F,3.2F); // swept wing
        box(root,"part_7",169,2,-7.2F,18F,-3.7F,4.2F,1.2F,3.2F); // swept wing
        box(root,"part_8",186,2,-9.4F,18F,-1.4000000000000004F,6.4F,1.2F,3.2F); // swept wing
        box(root,"part_9",208,2,-11.600000000000001F,18F,0.8999999999999995F,8.600000000000001F,1.2F,3.2F); // swept wing
        box(root,"part_10",234,2,-13.8F,18F,3.1999999999999993F,10.8F,1.2F,3.2F); // swept wing
        box(root,"part_11",264,2,-13.8F,17.9F,4F,2F,1.4F,3F); // wing accent
        box(root,"part_12",276,2,-5F,16F,-3F,2F,3F,8F); // intake housing
        box(root,"part_13",298,2,-4.8F,16.4F,-3.2F,1.6F,2.2F,0.3F); // intake mouth
        box(root,"part_14",304,2,-7F,17F,10F,5F,1F,4F); // rear stabilizer
        box(root,"part_15",324,2,-2F,21F,-8F,1F,3F,18F); // landing skid
        box(root,"part_16",364,2,3F,18F,-6.0F,2.0F,1.2F,3.2F); // swept wing
        box(root,"part_17",377,2,3F,18F,-3.7F,4.2F,1.2F,3.2F); // swept wing
        box(root,"part_18",394,2,3F,18F,-1.4000000000000004F,6.4F,1.2F,3.2F); // swept wing
        box(root,"part_19",416,2,3F,18F,0.8999999999999995F,8.600000000000001F,1.2F,3.2F); // swept wing
        box(root,"part_20",442,2,3F,18F,3.1999999999999993F,10.8F,1.2F,3.2F); // swept wing
        box(root,"part_21",472,2,11.8F,17.9F,4F,2F,1.4F,3F); // wing accent
        box(root,"part_22",484,2,3F,16F,-3F,2F,3F,8F); // intake housing
        box(root,"part_23",2,25,3.2F,16.4F,-3.2F,1.6F,2.2F,0.3F); // intake mouth
        box(root,"part_24",8,25,2F,17F,10F,5F,1F,4F); // rear stabilizer
        box(root,"part_25",28,25,1F,21F,-8F,1F,3F,18F); // landing skid
        box(root,"part_26",68,25,-0.6F,9F,6F,1.2F,7F,8F); // tail fin root
        box(root,"part_27",89,25,-0.6F,7F,9F,1.2F,2F,5F); // tail fin tip
        box(root,"part_28",104,25,-2.8F,15.8F,14F,5.6F,5.6F,2F); // exhaust collar
        box(root,"part_29",122,25,-1.9F,16.7F,16F,3.8F,3.8F,0.5F); // exhaust nozzle
        box(root,"part_30",133,25,-1.25F,17.35F,16.55F,2.5F,2.5F,0.1F); // exhaust glow
        return LayerDefinition.create(mesh,512,512);
    }
    private static void box(PartDefinition parent,String name,int u,int v,float x,float y,float z,float dx,float dy,float dz){
        parent.addOrReplaceChild(name,CubeListBuilder.create().texOffs(u,v).addBox(x,y,z,dx,dy,dz),PartPose.ZERO);
    }
}
