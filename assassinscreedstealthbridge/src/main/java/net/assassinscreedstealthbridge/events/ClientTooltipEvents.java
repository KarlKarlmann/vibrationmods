package net.assassinscreedstealthbridge.events;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.assassinscreedstealthbridge.registry.BridgeTags;

@Mod.EventBusSubscriber(modid = "assassinscreedstealthbridge", value = Dist.CLIENT)
public class ClientTooltipEvents {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        // --- WEAPONS (Tiers 1-3) ---
        if (event.getItemStack().is(BridgeTags.STEALTH_WEAPON_T3)) {
            event.getToolTip().add(Component.empty());
            event.getToolTip().add(Component.translatable("tooltip.assassinscreedstealthbridge.weapon_t3").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            event.getToolTip().add(Component.translatable("tooltip.assassinscreedstealthbridge.weapon_t3.backstab").withStyle(ChatFormatting.RED));
            event.getToolTip().add(Component.translatable("tooltip.assassinscreedstealthbridge.weapon_t3.air_assassination").withStyle(ChatFormatting.RED));
        } else if (event.getItemStack().is(BridgeTags.STEALTH_WEAPON_T2)) {
            event.getToolTip().add(Component.empty());
            event.getToolTip().add(Component.translatable("tooltip.assassinscreedstealthbridge.weapon_t2").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            event.getToolTip().add(Component.translatable("tooltip.assassinscreedstealthbridge.weapon_t2.backstab").withStyle(ChatFormatting.RED));
        } else if (event.getItemStack().is(BridgeTags.STEALTH_WEAPON_T1)) {
            event.getToolTip().add(Component.empty());
            event.getToolTip().add(Component.translatable("tooltip.assassinscreedstealthbridge.weapon_t1").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            event.getToolTip().add(Component.translatable("tooltip.assassinscreedstealthbridge.weapon_t1.backstab").withStyle(ChatFormatting.RED));
        }

        // --- ARMOR (Tiers 1-3) ---
        if (event.getItemStack().is(BridgeTags.ASSASSIN_ARMOR_T3)) {
            event.getToolTip().add(Component.empty());
            event.getToolTip().add(Component.translatable("tooltip.assassinscreedstealthbridge.armor_t3").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD));
            event.getToolTip().add(Component.translatable("tooltip.assassinscreedstealthbridge.armor_t3.camo").withStyle(ChatFormatting.GREEN));
            event.getToolTip().add(Component.translatable("tooltip.assassinscreedstealthbridge.armor_t3.muffle").withStyle(ChatFormatting.GREEN));
        } else if (event.getItemStack().is(BridgeTags.ASSASSIN_ARMOR_T2)) {
            event.getToolTip().add(Component.empty());
            event.getToolTip().add(Component.translatable("tooltip.assassinscreedstealthbridge.armor_t2").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD));
            event.getToolTip().add(Component.translatable("tooltip.assassinscreedstealthbridge.armor_t2.camo").withStyle(ChatFormatting.GREEN));
            event.getToolTip().add(Component.translatable("tooltip.assassinscreedstealthbridge.armor_t2.muffle").withStyle(ChatFormatting.GREEN));
        } else if (event.getItemStack().is(BridgeTags.ASSASSIN_ARMOR_T1)) {
            event.getToolTip().add(Component.empty());
            event.getToolTip().add(Component.translatable("tooltip.assassinscreedstealthbridge.armor_t1").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD));
            event.getToolTip().add(Component.translatable("tooltip.assassinscreedstealthbridge.armor_t1.camo").withStyle(ChatFormatting.GREEN));
            event.getToolTip().add(Component.translatable("tooltip.assassinscreedstealthbridge.armor_t1.muffle").withStyle(ChatFormatting.GREEN));
        }
    }
}