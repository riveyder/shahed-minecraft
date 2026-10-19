package io.github.riveyder.shahed.mixin;
import io.github.riveyder.shahed.control.DroneSessions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Entity.class)
public abstract class ServerPilotGravityMixin{
    @Inject(method="isNoGravity",at=@At("HEAD"),cancellable=true)
    private void transientGravity(CallbackInfoReturnable<Boolean> ci){if((Object)this instanceof ServerPlayer p&&DroneSessions.isControlling(p))ci.setReturnValue(true);}
}
