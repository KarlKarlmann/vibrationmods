package net.vibratingsteps.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * ARCHITECTURE: VIBRATINGSTEPS CONFIGURATION SPECIFICATION
 * Defines the server-side configuration for all ParCool vibration events.
 * Generates a well-documented "vibratingsteps-server.toml" inside the Minecraft config directory.
 */
public class VibratingStepsConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // --- TICK-BASED ACTIONS (CONTINUOUS) ---
    public static final ForgeConfigSpec.BooleanValue FASTRUN_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> FASTRUN_EVENT;
    public static final ForgeConfigSpec.IntValue FASTRUN_INTERVAL;

    public static final ForgeConfigSpec.BooleanValue FASTSWIM_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> FASTSWIM_EVENT;
    public static final ForgeConfigSpec.IntValue FASTSWIM_INTERVAL;

    public static final ForgeConfigSpec.BooleanValue WALLRUN_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> WALLRUN_EVENT;
    public static final ForgeConfigSpec.IntValue WALLRUN_INTERVAL;

    public static final ForgeConfigSpec.BooleanValue SLIDE_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> SLIDE_EVENT;
    public static final ForgeConfigSpec.IntValue SLIDE_INTERVAL;

    // --- INSTANT ACTIONS (SINGLE SOUNDS) ---
    public static final ForgeConfigSpec.BooleanValue WALLJUMP_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> WALLJUMP_EVENT;

    public static final ForgeConfigSpec.BooleanValue VERTICAL_WALLRUN_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> VERTICAL_WALLRUN_EVENT;

    public static final ForgeConfigSpec.BooleanValue CATLEAP_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> CATLEAP_EVENT;

    public static final ForgeConfigSpec.BooleanValue CHARGEJUMP_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> CHARGEJUMP_EVENT;

    public static final ForgeConfigSpec.BooleanValue ROLL_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> ROLL_EVENT;

    public static final ForgeConfigSpec.BooleanValue DODGE_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> DODGE_EVENT;

    public static final ForgeConfigSpec.BooleanValue VAULT_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> VAULT_EVENT;

    public static final ForgeConfigSpec.BooleanValue CLIMBUP_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> CLIMBUP_EVENT;

    public static final ForgeConfigSpec.BooleanValue JUMPFROMBAR_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> JUMPFROMBAR_EVENT;

    static {
        BUILDER.push("VibratingSteps-Settings");

        BUILDER.comment("=========================================================================",
                        "CONTINUOUS TICK-BASED MOVEMENTS",
                        "Defines how often and what kind of vibrations are caused during running/sliding.",
                        "=========================================================================");
        
        FASTRUN_ENABLED = BUILDER.comment("Enable vibrations for fast running (FastRun)?").define("fastRun.enabled", true);
        FASTRUN_EVENT = BUILDER.comment("Which GameEvent should FastRun trigger? (Default: minecraft:step)").define("fastRun.gameEvent", "minecraft:step");
        FASTRUN_INTERVAL = BUILDER.comment("At what tick interval should FastRun vibrate? (Lower = More frequent/louder, Default: 5)").defineInRange("fastRun.interval", 5, 1, 100);

        FASTSWIM_ENABLED = BUILDER.comment("Enable vibrations for fast swimming (FastSwim)?").define("fastSwim.enabled", true);
        FASTSWIM_EVENT = BUILDER.comment("Which GameEvent should FastSwim trigger? (Default: minecraft:swim)").define("fastSwim.gameEvent", "minecraft:swim");
        FASTSWIM_INTERVAL = BUILDER.comment("At what tick interval should FastSwim vibrate? (Default: 5)").defineInRange("fastSwim.interval", 5, 1, 100);

        WALLRUN_ENABLED = BUILDER.comment("Enable vibrations for horizontal wall running (HorizontalWallRun)?").define("horizontalWallRun.enabled", true);
        WALLRUN_EVENT = BUILDER.comment("Which GameEvent should wall running trigger? (Default: minecraft:step)").define("horizontalWallRun.gameEvent", "minecraft:step");
        WALLRUN_INTERVAL = BUILDER.comment("At what tick interval should wall running vibrate? (Default: 4)").defineInRange("horizontalWallRun.interval", 4, 1, 100);

        SLIDE_ENABLED = BUILDER.comment("Enable vibrations for sliding (Slide)?").define("slide.enabled", true);
        SLIDE_EVENT = BUILDER.comment("Which GameEvent should sliding trigger? (Default: minecraft:step)").define("slide.gameEvent", "minecraft:step");
        SLIDE_INTERVAL = BUILDER.comment("At what tick interval should sliding vibrate? (Default: 3)").defineInRange("slide.interval", 3, 1, 100);

        BUILDER.comment("=========================================================================",
                        "EXPLOSIVE INSTANT MOVEMENTS",
                        "Vibrations that are triggered instantly and uniquely upon executing a parkour action.",
                        "=========================================================================");

        WALLJUMP_ENABLED = BUILDER.comment("Enable wall jumping (WallJump) vibrations?").define("wallJump.enabled", true);
        WALLJUMP_EVENT = BUILDER.comment("GameEvent for WallJump").define("wallJump.gameEvent", "minecraft:hit_ground");

        VERTICAL_WALLRUN_ENABLED = BUILDER.comment("Enable vertical wall running start vibrations?").define("verticalWallRun.enabled", true);
        VERTICAL_WALLRUN_EVENT = BUILDER.comment("GameEvent for vertical WallRun start").define("verticalWallRun.gameEvent", "minecraft:hit_ground");

        CATLEAP_ENABLED = BUILDER.comment("Enable cat leaping (CatLeap) vibrations?").define("catLeap.enabled", true);
        CATLEAP_EVENT = BUILDER.comment("GameEvent for CatLeap").define("catLeap.gameEvent", "minecraft:hit_ground");

        CHARGEJUMP_ENABLED = BUILDER.comment("Enable charge jumping (ChargeJump) vibrations?").define("chargeJump.enabled", true);
        CHARGEJUMP_EVENT = BUILDER.comment("GameEvent for ChargeJump").define("chargeJump.gameEvent", "minecraft:hit_ground");

        ROLL_ENABLED = BUILDER.comment("Enable rolling (Roll) vibrations?").define("roll.enabled", true);
        ROLL_EVENT = BUILDER.comment("GameEvent for Roll").define("roll.gameEvent", "minecraft:step");

        DODGE_ENABLED = BUILDER.comment("Enable dodging (Dodge) vibrations?").define("dodge.enabled", true);
        DODGE_EVENT = BUILDER.comment("GameEvent for Dodge").define("dodge.gameEvent", "minecraft:step");

        VAULT_ENABLED = BUILDER.comment("Enable vaulting (Vault) vibrations?").define("vault.enabled", true);
        VAULT_EVENT = BUILDER.comment("GameEvent for Vault").define("vault.gameEvent", "minecraft:step");

        CLIMBUP_ENABLED = BUILDER.comment("Enable climbing up (ClimbUp) vibrations?").define("climbUp.enabled", true);
        CLIMBUP_EVENT = BUILDER.comment("GameEvent for ClimbUp").define("climbUp.gameEvent", "minecraft:step");

        JUMPFROMBAR_ENABLED = BUILDER.comment("Enable jumping from bars (JumpFromBar) vibrations?").define("jumpFromBar.enabled", true);
        JUMPFROMBAR_EVENT = BUILDER.comment("GameEvent for JumpFromBar").define("jumpFromBar.gameEvent", "minecraft:step");

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}