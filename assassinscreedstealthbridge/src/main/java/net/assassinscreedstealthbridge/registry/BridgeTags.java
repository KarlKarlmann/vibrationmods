package net.assassinscreedstealthbridge.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class BridgeTags {
    // Rüstungen (3 Tiers)
    public static final TagKey<Item> ASSASSIN_ARMOR_T1 = TagKey.create(Registries.ITEM, new ResourceLocation("assassinscreedstealthbridge", "assassin_armor_t1"));
    public static final TagKey<Item> ASSASSIN_ARMOR_T2 = TagKey.create(Registries.ITEM, new ResourceLocation("assassinscreedstealthbridge", "assassin_armor_t2"));
    public static final TagKey<Item> ASSASSIN_ARMOR_T3 = TagKey.create(Registries.ITEM, new ResourceLocation("assassinscreedstealthbridge", "assassin_armor_t3"));

    // Waffen (3 Tiers)
    public static final TagKey<Item> STEALTH_WEAPON_T1 = TagKey.create(Registries.ITEM, new ResourceLocation("assassinscreedstealthbridge", "stealth_weapon_t1"));
    public static final TagKey<Item> STEALTH_WEAPON_T2 = TagKey.create(Registries.ITEM, new ResourceLocation("assassinscreedstealthbridge", "stealth_weapon_t2"));
    public static final TagKey<Item> STEALTH_WEAPON_T3 = TagKey.create(Registries.ITEM, new ResourceLocation("assassinscreedstealthbridge", "stealth_weapon_t3"));
}