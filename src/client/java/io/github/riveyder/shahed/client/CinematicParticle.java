package io.github.riveyder.shahed.client;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.joml.Quaternionf;

/** Animated original sprites; bounded lifetime and no world/entity side effects. */
public final class CinematicParticle extends SingleQuadParticle {
    private final SpriteSet sprites;
    private final int kind;
    private final float size;
    public CinematicParticle(ClientLevel level,double x,double y,double z,SpriteSet sprites,int kind,float size,int ticks) {
        super(level,x,y,z,sprites.first());this.sprites=sprites;this.kind=kind;this.size=size;
        lifetime=Math.clamp(ticks,2,400);hasPhysics=kind==3;friction=kind==3?.98F:.985F;
        gravity=kind==3?.65F:0;quadSize=size;
        if(kind==3) {rCol=.35F;gCol=.29F;bCol=.21F;roll=random.nextFloat()*6.28F;}
        else if(kind==2) {rCol=1;gCol=.82F;bCol=.55F;}
    }
    @Override public void tick() {
        super.tick();oRoll=roll;float t=(float)age/lifetime;
        if(kind==0||kind==6) {
            quadSize=size*(.5F+2.8F*(float)Math.sqrt(t));
            rCol=t<.65F?1:.3F;gCol=Math.max(.12F,.95F-t*1.3F);bCol=Math.max(.025F,.35F-t*.6F);
            alpha=Math.min(1,(1-t)*2.5F);setSpriteFromAge(sprites);roll+=.015F;
        } else if(kind==5) {quadSize=size*(1-t*.5F);rCol=1;gCol=.3F+.35F*(1-t);bCol=.025F;alpha=(1-t);setSpriteFromAge(sprites);
        } else if(kind==2) {quadSize=size*(1+13*t);alpha=(1-t)*.65F;}
        else if(kind==3) {alpha=Math.min(1,(1-t)*4);roll+=.16F;}
        else {
            quadSize=size*(1+2*t);alpha=Math.min(1,age/6F)*Math.min(1,(1-t)*3)*.72F;
            rCol=kind==4?.57F:.18F+t*.18F;gCol=kind==4?.47F:rCol;bCol=kind==4?.35F:rCol;
            setSpriteFromAge(sprites);roll+=.004F;
        }
    }
    @Override protected Layer getLayer() {return Layer.TRANSLUCENT;}
    @Override protected int getLightCoords(float partial) {return kind==0||kind==2||kind==5||kind==6?15728880:super.getLightCoords(partial);}
    @Override public void extract(QuadParticleRenderState state,Camera camera,float partial) {
        if(kind==2) extractRotatedQuad(state,camera,new Quaternionf().rotationX((float)-Math.PI/2),partial);
        else super.extract(state,camera,partial);
    }
}
