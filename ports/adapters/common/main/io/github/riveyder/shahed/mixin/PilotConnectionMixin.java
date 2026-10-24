package io.github.riveyder.shahed.mixin;
import io.github.riveyder.shahed.compat.PilotConnection;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.*;
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PilotConnectionMixin implements PilotConnection {
    @Shadow private int aboveGroundTickCount;
    @Shadow private int aboveGroundVehicleTickCount;
    @Shadow private boolean clientIsFloating;
    @Shadow private boolean clientVehicleIsFloating;
    public void shahed$resetFlyingTicks(){aboveGroundTickCount=aboveGroundVehicleTickCount=0;clientIsFloating=clientVehicleIsFloating=false;}
}
