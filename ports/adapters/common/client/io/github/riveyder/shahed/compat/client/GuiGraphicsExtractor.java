package io.github.riveyder.shahed.compat.client;
import net.minecraft.client.gui.*;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
public final class GuiGraphicsExtractor {
 private final GuiGraphics g;
 public GuiGraphicsExtractor(GuiGraphics g){this.g=g;}
 public int guiWidth(){return g.guiWidth();} public int guiHeight(){return g.guiHeight();}
 public void text(Font f,String t,int x,int y,int c){g.drawString(f,t,x,y,c);}
 public void text(Font f,Component t,int x,int y,int c){g.drawString(f,t,x,y,c);}
 public void text(Font f,FormattedCharSequence t,int x,int y,int c){g.drawString(f,t,x,y,c);}
 public void centeredText(Font f,String t,int x,int y,int c){g.drawCenteredString(f,t,x,y,c);}
 public void centeredText(Font f,Component t,int x,int y,int c){g.drawCenteredString(f,t,x,y,c);}
 public void fill(int x,int y,int x2,int y2,int c){g.fill(x,y,x2,y2,c);}
 public void outline(int x,int y,int w,int h,int c){g.renderOutline(x,y,w,h,c);}
 public void enableScissor(int x,int y,int x2,int y2){var m=g.pose().last().pose();var a=m.transformPosition(new org.joml.Vector3f(x,y,0));var b=m.transformPosition(new org.joml.Vector3f(x2,y2,0));g.enableScissor((int)a.x,(int)a.y,(int)b.x,(int)b.y);}
 public void disableScissor(){g.disableScissor();}
 public Pose pose(){return new Pose(g.pose());}
 public record Pose(PoseStack stack){
  public void pushMatrix(){stack.pushPose();} public void popMatrix(){stack.popPose();}
  public void translate(float x,float y){stack.translate(x,y,0);} public void scale(float s){stack.scale(s,s,1);}
  public void rotate(float r){stack.mulPose(Axis.ZP.rotation(r));}
 }
}
