package io.github.riveyder.shahed.client.renderer;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** A copied render snapshot; the renderer never retains or mutates the live entity. */
public final class DroneRenderState extends EntityRenderState {
    public float yaw;
    public float pitch;
    public float roll;
}
