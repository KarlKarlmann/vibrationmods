package net.assassinscreedstealthbridge.config;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.List;

public class SyndicateConfig {
    public static final ForgeConfigSpec SERVER_SPEC;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> UNIQUE_SYNDICATE_MEMBERS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SYNDICATE_FIRST_NAMES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SYNDICATE_LAST_NAMES;
    
    public static final ForgeConfigSpec.IntValue THREAT_THRESHOLD;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SNITCH_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SYNDICATE_DIVISIONS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Syndicate Stealth Settings").push("syndicate");

        THREAT_THRESHOLD = builder
                .comment("How many points are needed to trigger an ambush?")
                .defineInRange("threatThreshold", 100, 10, 1000);

        builder.comment("Named Syndicate members. Format: 'id|DisplayName|MobEffectId'").push("uniqueMembers");
        UNIQUE_SYNDICATE_MEMBERS = builder.defineList("members", 
                        List.of(
                            "berg|Juhani Otso Berg|minecraft:resistance", 
                            "cross|Daniel Cross|minecraft:speed", 
                            "lucy|Lucy Stillman|minecraft:invisibility", 
                            "vidic|Warren Vidic|minecraft:regeneration", 
                            "sable|Robert de Sable|minecraft:strength", 
                            "borgia|Cesare Borgia|minecraft:fire_resistance", 
                            "haytham|Haytham Kenway|minecraft:absorption"
                        ),
                        obj -> obj instanceof String);
        builder.pop();
        
        builder.comment("First and last names for randomly generated Syndicate grunts.").push("randomNames");
        SYNDICATE_FIRST_NAMES = builder.defineList("firstNames", 
                        List.of("Alex", "Sam", "Victor", "Ezio", "Altair", "Edward", "Arno", "Jacob", "Evie", "Kassandra"),
                        obj -> obj instanceof String);
        SYNDICATE_LAST_NAMES = builder.defineList("lastNames", 
                        List.of("Smith", "Auditore", "Kenway", "Dorian", "Frye", "Cormac", "Ibn-La'Ahad", "Cross", "Miles"),
                        obj -> obj instanceof String);
        builder.pop();

        // AMBUSH_WAVES wurde komplett entfernt!

        builder.comment("List of snitches").push("points");
        SNITCH_ENTITIES = builder.defineList("snitchEntities",
                List.of("minecraft:villager|6", "minecraft:piglin|10", "minecraft:wandering_trader|25", "assassinscreedstealthbridge:wandering_wanderer|40"),
                obj -> obj instanceof String);
        builder.pop();

        builder.comment("Divisions. Format: 'DivisionId|DisplayName|RewardBoxId'").push("board");
        SYNDICATE_DIVISIONS = builder.defineList("syndicateDivisions",
                List.of(
                        "transportation|Transportation|reward_box:transportation",
                        "fortification|Fortification|reward_box:fortification",
                        "research|Research|reward_box:research",
                        "intervention|Intervention|reward_box:intervention",
                        "shadow_cell|Shadow Cell|reward_box:shadow_cell",
                        "enforcers|Enforcers|reward_box:enforcers",
                        "financiers|Financiers|reward_box:financiers",
                        "zealots|Zealots|reward_box:zealots"
                ),
                obj -> obj instanceof String);
        builder.pop();

        builder.pop();
        SERVER_SPEC = builder.build();
    }
}
