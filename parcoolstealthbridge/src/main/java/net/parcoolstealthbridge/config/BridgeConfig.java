package net.parcoolstealthbridge.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * SYSTEM: CONFIG ENGINE FOR PARCOOL-STEALTH BRIDGE
 * Manages all adjustable modifiers for Camouflage,
 * Muffling (noise dampening), and client-side HUD settings.
 */
public class BridgeConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final CommonConfig COMMON;

    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ClientConfig CLIENT;

    static {
        final Pair<CommonConfig, ForgeConfigSpec> commonSpecPair = new ForgeConfigSpec.Builder().configure(CommonConfig::new);
        COMMON_SPEC = commonSpecPair.getRight();
        COMMON = commonSpecPair.getLeft();

        final Pair<ClientConfig, ForgeConfigSpec> clientSpecPair = new ForgeConfigSpec.Builder().configure(ClientConfig::new);
        CLIENT_SPEC = clientSpecPair.getRight();
        CLIENT = clientSpecPair.getLeft();
    }

    public static class CommonConfig {
        // Crawl Modifiers
        public final ForgeConfigSpec.DoubleValue crawlCamouflage;
        public final ForgeConfigSpec.DoubleValue crawlMuffling;

        // Slide Modifiers
        public final ForgeConfigSpec.DoubleValue slideCamouflage;

        // FastRun / FastSwim Modifiers (negative values represent a visibility penalty)
        public final ForgeConfigSpec.DoubleValue fastRunCamouflage;
        public final ForgeConfigSpec.DoubleValue fastRunMuffling;

        // Cling Modifiers
        public final ForgeConfigSpec.DoubleValue clingCamouflage;

        // HideInBlock Modifiers
        public final ForgeConfigSpec.DoubleValue hideInBlockCamouflage;

        CommonConfig(ForgeConfigSpec.Builder builder) {
            builder.push("movement_modifiers");

            crawlCamouflage = builder
                    .comment("Additional camouflage bonus when crawling (Crawl). 0.0 = Normal, 1.0 = Invisible. Default: 0.4 (40%)")
                    .defineInRange("crawl_camouflage", 0.4, -10.0, 1.0);

            crawlMuffling = builder
                    .comment("Muffling (noise reduction) bonus when crawling (Crawl). 0.0 = Loud, 1.0 = Silent. Default: 0.5 (50%)")
                    .defineInRange("crawl_muffling", 0.5, -10.0, 1.0);

            slideCamouflage = builder
                    .comment("Camouflage bonus while sliding (Slide). Default: 0.3 (30%)")
                    .defineInRange("slide_camouflage", 0.3, -10.0, 1.0);

            fastRunCamouflage = builder
                    .comment("Camouflage penalty (malus) during fast running/swimming (FastRun / FastSwim). Default: -0.3 (-30%)")
                    .defineInRange("fastrun_camouflage_penalty", -0.3, -10.0, 1.0);

            fastRunMuffling = builder
                    .comment("Muffling penalty (malus) during fast running/swimming (FastRun / FastSwim). Default: -0.4 (-40%)")
                    .defineInRange("fastrun_muffling_penalty", -0.4, -10.0, 1.0);

            clingCamouflage = builder
                    .comment("Camouflage bonus when clinging to edges (Cling / HangDown). Default: 0.2 (20%)")
                    .defineInRange("cling_camouflage", 0.2, -10.0, 1.0);

            hideInBlockCamouflage = builder
                    .comment("Camouflage bonus inside cardboard box / block hiding (HideInBlock). Default: 0.9 (90%)")
                    .defineInRange("hide_in_block_camouflage", 0.9, -10.0, 1.0);

            builder.pop();
        }
    }

    public static class ClientConfig {
        public final ForgeConfigSpec.BooleanValue showHudOnCrawl;

        ClientConfig(ForgeConfigSpec.Builder builder) {
            builder.push("client");

            showHudOnCrawl = builder
                    .comment("Specifies whether the standard Stealth HUD (eye, dagger, soundwaves) is automatically displayed",
                             "as soon as the player crawls (Crawl), without needing to crouch simultaneously. Default: true")
                    .define("show_hud_on_crawl", true);

            builder.pop();
        }
    }
}