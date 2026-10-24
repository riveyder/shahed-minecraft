package io.github.riveyder.shahed.control;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Protection adapters may veto a blast location, affected block or entity position. Owner may be null. */
public final class DroneExplosionProtection {
    public interface Allow {boolean allow(ServerLevel level,ServerPlayer owner,BlockPos position);}
    public static final Event<Allow> ALLOW=EventFactory.createArrayBacked(Allow.class,listeners->(level,owner,pos)->{
        for(var listener:listeners) if(!listener.allow(level,owner,pos)) return false;return true;
    });
    private DroneExplosionProtection() { }
}
