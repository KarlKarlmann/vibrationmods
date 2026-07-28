package net.stealth.manhunt.init;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.stealth.manhunt.StealthManhunt;

public class ManhuntParticles {
    public static final DeferredRegister<ParticleType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, StealthManhunt.MODID);

    // alwaysShow = true: Das Echo wird auch über weite Entfernungen vom Client nicht wegoptimiert!
    public static final RegistryObject<SimpleParticleType> ECHO_WAVE = REGISTRY.register("echo_wave", 
            () -> new SimpleParticleType(true));
}