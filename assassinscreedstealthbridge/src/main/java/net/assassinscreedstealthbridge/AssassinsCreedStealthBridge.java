package net.assassinscreedstealthbridge;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.assassinscreedstealthbridge.events.BridgeEvents;
import org.slf4j.Logger;
import net.assassinscreedstealthbridge.config.SyndicateConfig;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.ModLoadingContext;

@Mod(AssassinsCreedStealthBridge.MODID)
public class AssassinsCreedStealthBridge {
    public static final String MODID = "assassinscreedstealthbridge";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AssassinsCreedStealthBridge() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

        // Registriert unsere extrem mächtigen Bridge-Events!
        MinecraftForge.EVENT_BUS.register(new BridgeEvents());
		ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SyndicateConfig.SERVER_SPEC);
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("[AC-Stealth-Bridge] Loaded! The Brotherhood is now fully integrated with the Stealth System.");
    }
}