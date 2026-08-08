package net.assassinscreedstealthbridge.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class SyndicateConfig {
    public static final ForgeConfigSpec SERVER_SPEC;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SYNDICATE_NAMES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> AMBUSH_WAVES;
    public static final ForgeConfigSpec.IntValue THREAT_THRESHOLD;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Syndicate Stealth Settings").push("syndicate");

        THREAT_THRESHOLD = builder
                .comment("How many points are needed to trigger an ambush?")
                .defineInRange("threatThreshold", 100, 10, 1000);

        SYNDICATE_NAMES = builder
                .comment("List of names for named Syndicate members.")
                .defineList("syndicateNames", 
                        List.of("Juhani Otso Berg", "Daniel Cross", "Lucy Stillman", "Warren Vidic", "Robert de Sable", "Cesare Borgia", "Haytham Kenway"),
                        obj -> obj instanceof String);

        AMBUSH_WAVES = builder
                .comment("Configure the ambush waves. Format: 'Level|MobID|Count|IsNamed|WeaponID|HealthBonus|DamageBonus|SpeedBonus'",
                         "Level: The player's ambush level (starts at 1).",
                         "MobID: The registry name of the entity (e.g., assassins_creed:templar).",
                         "Count: How many of these mobs should spawn.",
                         "IsNamed: true or false. If true, takes a random name from syndicateNames.",
                         "WeaponID: Registry name of the weapon to equip (e.g., minecraft:iron_sword) or 'none'.",
                         "HealthBonus: Extra max health (e.g., 10.0 = 5 extra hearts).",
                         "DamageBonus: Extra attack damage (e.g., 2.0 = 1 extra heart of damage).",
                         "SpeedBonus: Extra movement speed (e.g., 0.05 for a slight boost).")
                .defineList("ambushWaves",
                        List.of(
                                "1|assassins_creed:templar|1|true|minecraft:iron_sword|0.0|0.0|0.0",
                                "2|assassins_creed:templar_2|2|true|minecraft:iron_sword|0.0|0.0|0.0",
                                "3|assassins_creed:templar_3|2|true|minecraft:iron_sword|5.0|1.0|0.0",
                                "4|assassins_creed:templar_4|3|true|minecraft:diamond_sword|10.0|2.0|0.02",
                                "5|assassins_creed:templar_5|3|true|minecraft:diamond_sword|15.0|3.0|0.03",
                                "6|assassins_creed:templar_6|4|true|minecraft:netherite_sword|25.0|5.0|0.05"
                        ),
                        obj -> obj instanceof String);

        builder.pop();
        SERVER_SPEC = builder.build();
    }
}