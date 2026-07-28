package net.stealth.manhunt.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ManhuntConfig {
    public static final ForgeConfigSpec SERVER_SPEC;

    // --- ENTITY CULLING (Gamma-Killer) ---
    public static final ForgeConfigSpec.DoubleValue PARANOIA_THRESHOLD;
    public static final ForgeConfigSpec.BooleanValue ALLIES_VISIBLE;

    // --- VISUAL AUDIO PERMISSIONS ---
    public static final ForgeConfigSpec.BooleanValue ALLOW_HUD_RADAR;
    public static final ForgeConfigSpec.BooleanValue ALLOW_ECHO_PARTICLES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Stealth: Manhunt - Core Server Configuration").push("culling");
        
        PARANOIA_THRESHOLD = builder
                .comment("How hidden an entity must be to be removed from the client (0.0 to 1.0).",
                         "Lower value = entities disappear easier in shadows. Default: 0.15")
                .defineInRange("paranoiaThreshold", 0.15D, 0.0D, 1.0D);

        ALLIES_VISIBLE = builder
                .comment("If true, players in the same scoreboard team will always see each other.")
                .define("alliesAlwaysVisible", true);
                
        builder.pop();
        
        builder.comment("Visual Audio Settings", 
                       "Determines what visual aids the server allows clients to use for deaf/stealth gameplay.")
               .push("visual_audio");
        
        ALLOW_HUD_RADAR = builder
                .comment("Allows clients to see the directional HUD radar (like Fortnite) for nearby sounds.",
                         "Great for tactical PvP.")
                .define("allowHudRadar", true);

        ALLOW_ECHO_PARTICLES = builder
                .comment("Allows clients to see 3D echo particles through walls at the exact sound source.",
                         "Great for PvE or specific 'Bat-Vision' items.")
                .define("allowEchoParticles", true);
        
        builder.pop();

        SERVER_SPEC = builder.build();
    }
}