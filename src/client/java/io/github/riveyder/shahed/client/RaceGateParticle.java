package io.github.riveyder.shahed.client;

import io.github.riveyder.shahed.race.RaceCourse.Gate;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.joml.Quaternionf;

/** Static oriented gate, refreshed by the client; no entity, blocks, physics or gameplay effect. */
final class RaceGateParticle extends SingleQuadParticle {
    private final float yaw;
    RaceGateParticle(ClientLevel world,SpriteSet sprites,Gate gate,boolean next){
        super(world,gate.x(),gate.y(),gate.z(),sprites.first());
        yaw=gate.yaw();quadSize=gate.radius();lifetime=12;hasPhysics=false;gravity=0;
        rCol=next?.2F:.7F;gCol=next?1:.8F;bCol=next?.4F:1;alpha=next?.95F:.28F;
    }
    @Override protected Layer getLayer(){return Layer.TRANSLUCENT;}
    @Override protected int getLightCoords(float partial){return 15728880;}
    @Override public void extract(QuadParticleRenderState state,Camera camera,float partial){
        extractRotatedQuad(state,camera,new Quaternionf().rotationY((float)Math.toRadians(-yaw)),partial);
    }
}
