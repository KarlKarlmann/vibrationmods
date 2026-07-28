package net.stealth.manhunt.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.stealth.manhunt.init.ManhuntParticles;
import net.stealth.manhunt.client.particle.EchoWaveParticle;

@Mod.EventBusSubscriber(modid = "manhunt", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ManhuntParticleRender {

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        // Verbindet die Registry mit unserem EchoWave-Particle
        event.registerSpriteSet(ManhuntParticles.ECHO_WAVE.get(), EchoWaveParticle.Provider::new);
    }
}