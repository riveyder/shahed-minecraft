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
    public static final SoundEvent IMPACT = sound("impact");
    public static final SoundEvent BLAST = sound("blast");
    public static final SoundEvent BLAST_ECHO = sound("blast_echo");
    public static final SoundEvent BLAST_DISTANT = sound("blast_distant");
    private static SoundEvent sound(String name) {
        var id=ShahedMod.id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT,id,SoundEvent.createVariableRangeEvent(id));
    }
    public static void initialize() { }
    private ModSounds() { }
}
