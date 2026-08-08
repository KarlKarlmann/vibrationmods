package net.assassinscreedstealthbridge.syndicate;

import net.assassinscreedstealthbridge.AssassinsCreedStealthBridge;
import net.assassinscreedstealthbridge.config.SyndicateConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.stealth.registry.StealthSounds;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = AssassinsCreedStealthBridge.MODID)
public class SyndicateEvents {

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.side.isServer() && event.phase == TickEvent.Phase.END) {
            ServerLevel level = (ServerLevel) event.level;
            
            if (level.dimension() == Level.OVERWORLD) {
                long currentDay = level.getDayTime() / 24000;
                SyndicateManager manager = SyndicateManager.get(level);
                
                if (currentDay > manager.lastReckoningDay) {
                    manager.executeDailyReckoning(level);
                    manager.lastReckoningDay = currentDay;
                    manager.setDirty();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side.isClient() || event.phase == TickEvent.Phase.START || !event.player.isAlive()) return;

        Player player = event.player;
        if (player.level() instanceof ServerLevel level) {
            
            SyndicateManager manager = SyndicateManager.get(level);
            int threat = manager.getThreatLevel(player);
            int pending = manager.getPendingPoints(player);
            int threshold = SyndicateConfig.THREAT_THRESHOLD.get();

            // ==========================================
            // EAGLE VISION (Live Daten)
            // ==========================================
            if (player.isCrouching()) {
                if (player.tickCount % 20 == 0) {
                    int currentAmbushLevel = manager.getAmbushLevel(player);
                    player.displayClientMessage(Component.literal("§8[Eagle Vision] §cThreat: " + threat + "/" + threshold + " §7| §ePending: +" + pending + " §7| §4Wave: " + currentAmbushLevel), true);
                }
                
                if (player.tickCount % 10 == 0) {
                    AABB searchBox = player.getBoundingBox().inflate(32.0);
                    for (Mob mob : level.getEntitiesOfClass(Mob.class, searchBox)) {
                        SyndicateManager.Sighting sighting = manager.getSighting(mob);
                        if (sighting != null && sighting.playerUuid.equals(player.getUUID())) {
                            level.sendParticles((net.minecraft.server.level.ServerPlayer) player, ParticleTypes.DAMAGE_INDICATOR, false, 
                                    mob.getX(), mob.getEyeY() + 0.8, mob.getZ(), 
                                    1, 0.1, 0.1, 0.1, 0.0);
                        }
                    }
                }
            }

            // ==========================================
            // AMBUSH TRIGGER
            // ==========================================
            if (threat >= threshold) {
                if (!isSafeToAmbush(player, level)) return;
                
                if (level.random.nextInt(300) == 0) {
                    //AssassinsCreedStealthBridge.LOGGER.info("[Syndicate Debug] AMBUSH AUSGELÖST für Spieler " + player.getName().getString());
                    player.displayClientMessage(Component.literal("§4The Syndicate has found you!").withStyle(net.minecraft.ChatFormatting.BOLD), true);
                    
                    triggerSyndicateAmbush(player, level, manager);
                    
                    manager.resetThreatLevel(player);
                    manager.incrementAmbushLevel(player);
                    
                    //AssassinsCreedStealthBridge.LOGGER.info("[Syndicate Debug] Threat-Level für " + player.getName().getString() + " wurde auf 0 resettet (Ambush ausgeführt!).");
                } else if (level.random.nextInt(400) == 0 && !player.isCrouching()) { 
                    level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.5f, 0.6f);
                    player.displayClientMessage(Component.literal("§cYou feel like you are being watched...").withStyle(net.minecraft.ChatFormatting.ITALIC), true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onMobDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            ServerLevel level = (ServerLevel) event.getEntity().level();
            SyndicateManager.get(level).onMobDeath(event.getEntity());
        }
    }

    private static boolean isSafeToAmbush(Player player, ServerLevel level) {
        if (player.isFallFlying() || player.getDeltaMovement().y < -0.5) return false;
        if (player.isInWater() || player.isInLava()) return false;
        if (player.isSleeping()) return false;
        
        AABB bossArea = player.getBoundingBox().inflate(64.0);
        boolean bossNearby = !level.getEntitiesOfClass(EnderDragon.class, bossArea).isEmpty() ||
                             !level.getEntitiesOfClass(WitherBoss.class, bossArea).isEmpty();
        return !bossNearby;
    }

    private static void triggerSyndicateAmbush(Player player, ServerLevel level, SyndicateManager manager) {
        int playerWaveLevel = manager.getAmbushLevel(player);
        List<? extends String> configWaves = SyndicateConfig.AMBUSH_WAVES.get();
        List<? extends String> syndicateNames = SyndicateConfig.SYNDICATE_NAMES.get();
        
        List<String> currentWaveDefs = new ArrayList<>();
        
        // 1. Sammle ALLE Spawns, die für dieses Level konfiguriert sind
        for (String waveDef : configWaves) {
            String[] parts = waveDef.split("\\|");
            if (parts.length >= 4) {
                try {
                    int waveLevel = Integer.parseInt(parts[0]);
                    if (waveLevel == playerWaveLevel) {
                        currentWaveDefs.add(waveDef);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        // 2. Fallback: Höchste Welle suchen, wenn Level überschritten ist
        if (currentWaveDefs.isEmpty() && !configWaves.isEmpty()) {
            int maxLevel = 1;
            for (String waveDef : configWaves) {
                String[] parts = waveDef.split("\\|");
                try { maxLevel = Math.max(maxLevel, Integer.parseInt(parts[0])); } catch (Exception ignored) {}
            }
            for (String waveDef : configWaves) {
                String[] parts = waveDef.split("\\|");
                try { 
                    if (Integer.parseInt(parts[0]) == maxLevel) {
                        currentWaveDefs.add(waveDef);
                    }
                } catch (Exception ignored) {}
            }
        }

        // 3. Spawne alle konfigurierten Mobs
        for (String waveDef : currentWaveDefs) {
            String[] parts = waveDef.split("\\|");
            String mobId = parts[1];
            int count = Integer.parseInt(parts[2]);
            boolean isNamed = Boolean.parseBoolean(parts[3]);
            String weaponId = parts.length > 4 ? parts[4] : "none"; 
            
            double healthBonus = 0.0, damageBonus = 0.0, speedBonus = 0.0;
            try {
                if (parts.length > 5) healthBonus = Double.parseDouble(parts[5]);
                if (parts.length > 6) damageBonus = Double.parseDouble(parts[6]);
                if (parts.length > 7) speedBonus = Double.parseDouble(parts[7]);
            } catch (NumberFormatException ignored) {}

            EntityType<?> chosenType = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(mobId));
            
            if (chosenType != null) {
                for (int i = 0; i < count; i++) {
                    Entity entity = chosenType.create(level);
                    
                    if (entity instanceof Mob ambusher) {
                        // Position berechnen
                        double angle = level.random.nextDouble() * 2 * Math.PI;
                        double distance = 8.0 + level.random.nextDouble() * 7.0; 
                        
                        double spawnX = player.getX() + distance * Math.cos(angle);
                        double spawnZ = player.getZ() + distance * Math.sin(angle);
                        double spawnY = player.getY(); 
                        
                        ambusher.moveTo(spawnX, spawnY, spawnZ, level.random.nextFloat() * 360F, 0.0F);

                        // Name setzen
                        if (isNamed && !syndicateNames.isEmpty()) {
                            String randomName = syndicateNames.get(level.random.nextInt(syndicateNames.size()));
                            ambusher.setCustomName(Component.literal("§c" + randomName));
                            ambusher.setCustomNameVisible(true);
                        }

                        // Waffe ausrüsten
                        if (!weaponId.equals("none")) {
                            Item weaponItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(weaponId));
                            if (weaponItem != null && weaponItem != Items.AIR) {
                                ambusher.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weaponItem));
                                ambusher.setDropChance(EquipmentSlot.MAINHAND, 0.05f); 
                            }
                        }

                        // Attribute Buffs
                        if (healthBonus > 0 && ambusher.getAttribute(Attributes.MAX_HEALTH) != null) {
                            double newMaxHealth = ambusher.getAttribute(Attributes.MAX_HEALTH).getBaseValue() + healthBonus;
                            ambusher.getAttribute(Attributes.MAX_HEALTH).setBaseValue(newMaxHealth);
                            ambusher.setHealth((float) newMaxHealth); 
                        }
                        
                        if (damageBonus > 0 && ambusher.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
                            double newDamage = ambusher.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue() + damageBonus;
                            ambusher.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(newDamage);
                        }
                        
                        if (speedBonus > 0 && ambusher.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
                            double newSpeed = ambusher.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue() + speedBonus;
                            ambusher.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(newSpeed);
                        }

                        // Spawnen und Effekte
                        ambusher.setTarget(player);
                        level.addFreshEntity(ambusher);
                        
                        BlockPos spawnPos = BlockPos.containing(spawnX, spawnY, spawnZ);
                        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, spawnX, spawnY + 1.0, spawnZ, 30, 0.5, 1.0, 0.5, 0.05);
                        level.sendParticles(ParticleTypes.SQUID_INK, spawnX, spawnY + 1.0, spawnZ, 20, 0.5, 1.0, 0.5, 0.1);
                        level.playSound(null, spawnPos, StealthSounds.BACKSTAB.get(), SoundSource.HOSTILE, 1.5f, 0.5f);
                    }
                }
            }
        }
    }
}