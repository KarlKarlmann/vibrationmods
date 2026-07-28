package net.stealth.manhunt;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.stealth.manhunt.config.ManhuntConfig;
import net.stealth.manhunt.init.ManhuntParticles;
import net.stealth.manhunt.network.ManhuntNetwork;
import org.slf4j.Logger;

// The ultimate Hardcore Hide & Seek experience.
@Mod(StealthManhunt.MODID)
public class StealthManhunt {
    
    // WICHTIG: Die MODID muss exakt "manhunt" sein (so wie in der mods.toml und build.gradle definiert)
    public static final String MODID = "manhunt";
    public static final Logger LOGGER = LogUtils.getLogger();

    public StealthManhunt() {
        // 1. Configs registrieren (damit sie im /config Ordner generiert werden)
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ManhuntConfig.SERVER_SPEC);
        
        // Die Client-Config wurde komplett entfernt, da das Radar-System durch Serverseitige Echos ersetzt wurde.

        // 2. Lifecycle Events auf dem Mod-Bus abonnieren (für die Setup-Methode)
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

        // 3. Generellen Forge Event Bus registrieren
        // (Das meiste läuft bei uns mittlerweile über die @Mod.EventBusSubscriber Annotationen,
        // aber das hier hält die Tür offen, falls wir Instanzen registrieren müssen).
        MinecraftForge.EVENT_BUS.register(this);
        
        // 4. Registriert die Manhunt-Partikel
        ManhuntParticles.REGISTRY.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    private void setup(final FMLCommonSetupEvent event) {
        // EnqueueWork stellt sicher, dass das Netzwerk Thread-Safe während des Mod-Ladens registriert wird
        event.enqueueWork(() -> {
            ManhuntNetwork.register();
        });

        LOGGER.info("[Stealth: Manhunt] Initialized. The server is now hiding the truth.");
        LOGGER.info("[Stealth: Manhunt] Prop-Hunt is for cowards. Welcome to the shadows.");
    }
}