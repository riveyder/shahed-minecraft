package io.github.riveyder.shahed.client.model;
import io.github.riveyder.shahed.ShahedMod;
import io.github.riveyder.shahed.client.renderer.DroneRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
public final class PayloadModel extends EntityModel<DroneRenderState>{
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(ShahedMod.id("arcade_payload"),"main");
    public PayloadModel(ModelPart root){super(root);}
    public static LayerDefinition layer(){
        var mesh=new MeshDefinition();var r=mesh.getRoot();
        r.addOrReplaceChild("casing",CubeListBuilder.create().texOffs(0,0).addBox(-2.5F,20,-3,5,3,6),PartPose.ZERO);
        r.addOrReplaceChild("band",CubeListBuilder.create().texOffs(0,12).addBox(-2.6F,21,-3.1F,5.2F,1,6.2F),PartPose.ZERO);
        r.addOrReplaceChild("latch",CubeListBuilder.create().texOffs(24,0).addBox(-1,19,-1,2,1,2),PartPose.ZERO);
        return LayerDefinition.create(mesh,32,32);
    }
}
