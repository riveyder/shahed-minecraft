package io.github.riveyder.shahed.compat.client;
import net.minecraft.client.particle.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.Camera;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.*;
import net.minecraft.util.Mth;
public abstract class SingleQuadParticle extends TextureSheetParticle {
 protected enum Layer {TRANSLUCENT}
 protected SingleQuadParticle(ClientLevel l,double x,double y,double z,TextureAtlasSprite sprite){super(l,x,y,z);setSprite(sprite);}
 protected abstract Layer getLayer();
 @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
 protected int getLightCoords(float t){return super.getLightColor(t);}
 @Override public void render(VertexConsumer v,Camera c,float t){extract(new QuadParticleRenderState(v),c,t);}
 public void extract(QuadParticleRenderState s,Camera c,float t){var q=new Quaternionf(c.rotation());q.rotateZ(Mth.lerp(t,oRoll,roll));extractRotatedQuad(s,c,q,t);}
 protected void extractRotatedQuad(QuadParticleRenderState s,Camera c,Quaternionf q,float t){
  var cp=c.getPosition();float px=(float)(Mth.lerp(t,xo,x)-cp.x),py=(float)(Mth.lerp(t,yo,y)-cp.y),pz=(float)(Mth.lerp(t,zo,z)-cp.z);
  float size=getQuadSize(t);int light=getLightCoords(t);
  vertex(s.vertices(),q,-1,-1,px,py,pz,size,getU1(),getV1(),light);
  vertex(s.vertices(),q,-1,1,px,py,pz,size,getU1(),getV0(),light);
  vertex(s.vertices(),q,1,1,px,py,pz,size,getU0(),getV0(),light);
  vertex(s.vertices(),q,1,-1,px,py,pz,size,getU0(),getV1(),light);
 }
 private void vertex(VertexConsumer v,Quaternionf q,float x,float y,float px,float py,float pz,float size,float u,float w,int light){
  var p=new Vector3f(x,y,0).rotate(q).mul(size).add(px,py,pz);
  v.vertex(p.x,p.y,p.z).uv(u,w).color(rCol,gCol,bCol,alpha).uv2(light).endVertex();
 }
}
