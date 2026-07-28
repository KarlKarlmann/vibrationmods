package net.targetoutlines;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.targetoutlines.network.TargetNetwork;
import net.targetoutlines.registry.ModParticles;

@Mod(TargetOutlinesMod.MODID)
public class TargetOutlinesMod {
    public static final String MODID = "targetoutlines";

    public TargetOutlinesMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModParticles.register(modEventBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, TargetConfig.CLIENT_SPEC);

        modEventBus.addListener(this::setup);

        MinecraftForge.EVENT_BUS.register(new TargetEventHandler());

        if (ModList.get().isLoaded("stealth")) {
            net.targetoutlines.compat.StealthCompatHandler.register();
        }
    }

    private void setup(final FMLCommonSetupEvent event) {
        TargetNetwork.register();
    }
}