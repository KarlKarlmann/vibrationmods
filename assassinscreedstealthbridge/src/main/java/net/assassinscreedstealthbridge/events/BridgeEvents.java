package net.assassinscreedstealthbridge.events;

import net.assassinscreedstealthbridge.registry.BridgeTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.stealth.registry.StealthAttributes;
import net.stealth.registry.StealthSounds;

import java.util.UUID;

public class BridgeEvents {

    private UUID getUUIDForSlot(EquipmentSlot slot, String base) {
        return UUID.nameUUIDFromBytes((base + slot.getName()).getBytes());
    }

    @SubscribeEvent
    public void onAttributeModification(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        EquipmentSlot slot = event.getSlotType();

        double backstabBonus = 0.0;
        if (stack.is(BridgeTags.STEALTH_WEAPON_T3)) backstabBonus = 5.0D;
        else if (stack.is(BridgeTags.STEALTH_WEAPON_T2)) backstabBonus = 3.0D;
        else if (stack.is(BridgeTags.STEALTH_WEAPON_T1)) backstabBonus = 1.5D;

        if (backstabBonus > 0.0 && slot == EquipmentSlot.MAINHAND) {
            event.addModifier(StealthAttributes.BACKSTAB_MULTIPLIER.get(),
                    new AttributeModifier(getUUIDForSlot(slot, "AC_WeaponBackstab"), "AC Backstab", backstabBonus, AttributeModifier.Operation.ADDITION));
        }

        double camoBonus = 0.0;
        double muffleBonus = 0.0;
        if (stack.is(BridgeTags.ASSASSIN_ARMOR_T3)) { camoBonus = 0.15D; muffleBonus = 0.20D; }
        else if (stack.is(BridgeTags.ASSASSIN_ARMOR_T2)) { camoBonus = 0.10D; muffleBonus = 0.15D; }
        else if (stack.is(BridgeTags.ASSASSIN_ARMOR_T1)) { camoBonus = 0.05D; muffleBonus = 0.10D; }

        if (camoBonus > 0.0 && stack.getItem() instanceof ArmorItem armor) {
            if (armor.getType().getSlot() == slot) {
                event.addModifier(StealthAttributes.CAMOUFLAGE.get(),
                        new AttributeModifier(getUUIDForSlot(slot, "AC_ArmorCamo"), "AC Camo", camoBonus, AttributeModifier.Operation.ADDITION));
                event.addModifier(StealthAttributes.MUFFLING.get(),
                        new AttributeModifier(getUUIDForSlot(slot, "AC_ArmorMuffle"), "AC Muffle", muffleBonus, AttributeModifier.Operation.ADDITION));
            }
        }
    }

    @SubscribeEvent
    public void onAirAssassination(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            ItemStack weapon = player.getMainHandItem();

            if (weapon.is(BridgeTags.STEALTH_WEAPON_T3)) {
                if (player.getDeltaMovement().y < -0.1) {
                    if (player.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.CRIT,
                                event.getEntity().getX(), event.getEntity().getEyeY(), event.getEntity().getZ(),
                                40, 0.5, 0.5, 0.5, 0.3);
                        sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                                event.getEntity().getX(), event.getEntity().getEyeY(), event.getEntity().getZ(),
                                3, 0.2, 0.2, 0.2, 0.0);
                    }

                    player.level().playSound(null, player.blockPosition(), StealthSounds.BACKSTAB.get(), SoundSource.PLAYERS, 2.0f, 0.8f);
                    event.setAmount(event.getAmount() * 10.0f);
                    player.fallDistance = 0.0f;
                }
            }
        }
    }
}