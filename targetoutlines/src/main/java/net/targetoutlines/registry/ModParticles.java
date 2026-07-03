package net.targetoutlines.registry;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.targetoutlines.TargetOutlinesMod;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = 
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, TargetOutlinesMod.MODID);

    // Das Ausrufezeichen für den harten Alarm (!)
    public static final RegistryObject<SimpleParticleType> ALERT_MARK = 
            PARTICLES.register("alert_mark", () -> new SimpleParticleType(false));

    // Das Fragezeichen für den Verdacht (?)
    public static final RegistryObject<SimpleParticleType> SUSPICIOUS_MARK = 
            PARTICLES.register("suspicious_mark", () -> new SimpleParticleType(false));

    public static void register(IEventBus eventBus) {
        PARTICLES.register(eventBus);
    }
}