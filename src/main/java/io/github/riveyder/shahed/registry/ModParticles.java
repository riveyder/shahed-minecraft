package io.github.riveyder.shahed.registry;

import io.github.riveyder.shahed.ShahedMod;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.SimpleParticleType;

public final class ModParticles {
    public static final SimpleParticleType FIREBALL=register("fireball");
    public static final SimpleParticleType SMOKE=register("cinematic_smoke");
    public static final SimpleParticleType RING=register("shockwave");
    public static final SimpleParticleType DEBRIS=register("debris");
    public static final SimpleParticleType RACE_GATE=register("race_gate");
    private static SimpleParticleType register(String name) {return Registry.register(BuiltInRegistries.PARTICLE_TYPE,ShahedMod.id(name),FabricParticleTypes.simple());}
    public static void initialize() {}
    private ModParticles() {}
}
