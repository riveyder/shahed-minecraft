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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adjust before Camera.update prepares projection/culling, so every render pass shares the pose. */
@Mixin(Camera.class)
abstract class CameraMixin {
    @Inject(method="calculateFov",at=@At("RETURN"),cancellable=true)
    private void shahed$fov(float partialTick,CallbackInfoReturnable<Float> callback) {
        if(FpvController.isActive() || io.github.riveyder.shahed.client.ReplayController.isPlaying())
            callback.setReturnValue((float)FpvController.settings().cameraFov);
    }
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
        if(io.github.riveyder.shahed.client.ReplayController.isCamera(entity)) {
            var frame=io.github.riveyder.shahed.client.ReplayController.frame(partialTick);
            setRotation(io.github.riveyder.shahed.client.ReplayController.cameraYaw(partialTick),
                    io.github.riveyder.shahed.client.ReplayController.cameraPitch(partialTick));
            setPosition(io.github.riveyder.shahed.client.ReplayController.cameraPosition(partialTick));
            return;
        }
        if (!(entity instanceof DroneEntity drone) || !FpvController.isCameraDrone(entity)) return;
        FpvController.CameraPose pose = FpvController.cameraPose(partialTick);
        setRotation(pose.yaw(), pose.pitch());
        if(FpvController.settings().cameraEffects) rotation.rotateZ(pose.roll() * Mth.DEG_TO_RAD);
        // Vanilla derives these in setRotation before bank is applied; refresh for frustum/fog/rays.
        forwards.set(0, 0, -1).rotate(rotation);
        up.set(0, 1, 0).rotate(rotation);
        left.set(-1, 0, 0).rotate(rotation);
        matrixPropertiesDirty |= 3;
        // The visible lens is on the nose at local -Z = 33.7 px, rather than at the
        // entity centre. Rotate this fixed mount with the drone's real attitude;
        // the smoothed viewing direction must not slide the lens around its body.
        Quaternionf mount = new Quaternionf().rotationYXZ(
                Mth.PI - drone.getYRot(partialTick) * Mth.DEG_TO_RAD,
                -drone.getXRot(partialTick) * Mth.DEG_TO_RAD,
                drone.getRoll(partialTick) * Mth.DEG_TO_RAD);
        Vector3f offset = new Vector3f(0.0F, 0.49F, -2.15F).rotate(mount);
        setPosition(new Vec3(Mth.lerp(partialTick, drone.xo, drone.getX()) + offset.x(),
                Mth.lerp(partialTick, drone.yo, drone.getY()) + offset.y(),
                Mth.lerp(partialTick, drone.zo, drone.getZ()) + offset.z()));
    }
}
