package net.targetoutlines.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.targetoutlines.TargetOutlinesMod;
import net.targetoutlines.client.particle.IndicatorParticle;
import net.targetoutlines.registry.ModParticles;

@Mod.EventBusSubscriber(modid = TargetOutlinesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.ALERT_MARK.get(), IndicatorParticle.Provider::new);
        event.registerSpriteSet(ModParticles.SUSPICIOUS_MARK.get(), IndicatorParticle.Provider::new);
    }
}