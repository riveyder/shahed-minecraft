package io.github.riveyder.shahed.client.mixin;

import io.github.riveyder.shahed.client.FpvController;
import io.github.riveyder.shahed.entity.DroneEntity;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adjust before Camera.update prepares projection/culling, so every render pass shares the pose. */
@Mixin(Camera.class)
abstract class CameraMixin {
    @Shadow private Entity entity;
    @Shadow @Final private Quaternionf rotation;
    @Shadow @Final private Vector3f forwards;
    @Shadow @Final private Vector3f up;
    @Shadow @Final private Vector3f left;
    @Shadow private int matrixPropertiesDirty;
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow protected abstract void setPosition(Vec3 position);

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void shahed$alignFpv(float partialTick, CallbackInfo callback) {
        if (!(entity instanceof DroneEntity drone) || !FpvController.isCameraDrone(entity)) return;
        FpvController.CameraPose pose = FpvController.cameraPose(partialTick);
        setRotation(pose.yaw(), pose.pitch());
        rotation.rotateZ(pose.roll() * Mth.DEG_TO_RAD);
        // Vanilla derives these in setRotation before bank is applied; refresh for frustum/fog/rays.
        forwards.set(0, 0, -1).rotate(rotation);
        up.set(0, 1, 0).rotate(rotation);
        left.set(-1, 0, 0).rotate(rotation);
        matrixPropertiesDirty |= 3;
        setPosition(new Vec3(Mth.lerp(partialTick, drone.xo, drone.getX()),
                Mth.lerp(partialTick, drone.yo, drone.getY()) + 0.55,
                Mth.lerp(partialTick, drone.zo, drone.getZ())));
    }
}
