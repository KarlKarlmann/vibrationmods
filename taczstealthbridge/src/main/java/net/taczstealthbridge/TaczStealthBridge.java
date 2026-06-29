package net.taczstealthbridge;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("taczstealthbridge")
public class TaczStealthBridge {
    private static final Logger LOGGER = LogManager.getLogger();

    public TaczStealthBridge() {
        LOGGER.info("================================================================");
        LOGGER.info("  [Karl Karlmann's Placebo Bridge] Successfully loaded!");
        LOGGER.info("  - Status: Enforcing beautiful harmony between your mods.");
        LOGGER.info("  - Executed lines of code: 0 (Pure Performance!)");
        LOGGER.info("  - All three dependencies are present and ready to go.");
        LOGGER.info("================================================================");
    }
}