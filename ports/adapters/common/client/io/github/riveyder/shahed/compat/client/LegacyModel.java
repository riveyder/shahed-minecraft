package io.github.riveyder.shahed.compat.client;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.*;
public class LegacyModel<S> extends Model {
 protected final ModelPart root;
 public LegacyModel(ModelPart root){super(RenderType::entityCutoutNoCull);this.root=root;}
 public ModelPart root(){return root;}
 public void setupAnim(S state){root.getAllParts().forEach(ModelPart::resetPose);}
 @Override public void renderToBuffer(PoseStack poses,VertexConsumer vertices,int light,int overlay,float r,float g,float b,float a){root.render(poses,vertices,light,overlay,r,g,b,a);}
}
