package io.github.riveyder.shahed.registry;

import io.github.riveyder.shahed.ShahedMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
    public static final SoundEvent ENGINE = sound("engine");
    public static final SoundEvent STARTUP = sound("startup");
    public static final SoundEvent SHUTDOWN = sound("shutdown");
    public static final SoundEvent WIND = sound("wind");
    public static final SoundEvent PAYLOAD_RELEASE = sound("payload_release");
    public static final SoundEvent IMPACT = sound("impact");
    public static final SoundEvent BLAST = sound("blast");
    public static final SoundEvent BLAST_ECHO = sound("blast_echo");
    public static final SoundEvent BLAST_DISTANT = sound("blast_distant");
    public static final SoundEvent QUAD_ENGINE = sound("quad_engine");
    public static final SoundEvent QUAD_STARTUP = sound("quad_startup");
    public static final SoundEvent QUAD_SHUTDOWN = sound("quad_shutdown");
    public static final SoundEvent CONTACT_QUAD_ENGINE = sound("contact_quad_engine");
    public static final SoundEvent CONTACT_QUAD_STARTUP = sound("contact_quad_startup");
    public static final SoundEvent CONTACT_QUAD_SHUTDOWN = sound("contact_quad_shutdown");
    public static final SoundEvent JET_ENGINE = sound("jet_engine");
    public static final SoundEvent JET_STARTUP = sound("jet_startup");
    public static final SoundEvent JET_SHUTDOWN = sound("jet_shutdown");
    public static SoundEvent aircraft(io.github.riveyder.shahed.entity.AircraftKind kind,int stage){
        return switch(kind){
            case SHAHED->stage==1?STARTUP:stage==2?SHUTDOWN:ENGINE;
            case QUAD->stage==1?QUAD_STARTUP:stage==2?QUAD_SHUTDOWN:QUAD_ENGINE;
            case CONTACT->stage==1?CONTACT_QUAD_STARTUP:stage==2?CONTACT_QUAD_SHUTDOWN:CONTACT_QUAD_ENGINE;
            case JET->stage==1?JET_STARTUP:stage==2?JET_SHUTDOWN:JET_ENGINE;
        };
    }
    private static SoundEvent sound(String name) {
        var id=ShahedMod.id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT,id,SoundEvent.createVariableRangeEvent(id));
    }
    public static void initialize() { }
    private ModSounds() { }
}
