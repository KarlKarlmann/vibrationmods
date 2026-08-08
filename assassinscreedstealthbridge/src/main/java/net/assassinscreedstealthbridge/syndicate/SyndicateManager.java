package net.assassinscreedstealthbridge.syndicate;

import net.assassinscreedstealthbridge.AssassinsCreedStealthBridge;
import net.assassinscreedstealthbridge.config.SyndicateConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SyndicateManager extends SavedData {

    // Speichert das permanente Fahndungslevel der Spieler (UUID -> Punkte)
    private final Map<UUID, Integer> playerThreatLevels = new HashMap<>();
    
    // Speichert das Ambush-Level (Welche Welle kommt als nächstes?)
    private final Map<UUID, Integer> playerAmbushLevels = new HashMap<>();
    
    // Das tägliche "Hauptbuch" (Welcher Spion-Mob hat welchen Spieler gesehen?)
    private final Map<UUID, Sighting> activeSightings = new HashMap<>();
    
    public long lastReckoningDay = -1;

    public static class Sighting {
        public final UUID playerUuid;
        public final int points;

        public Sighting(UUID playerUuid, int points) {
            this.playerUuid = playerUuid;
            this.points = points;
        }
    }

    public static SyndicateManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                SyndicateManager::load,
                SyndicateManager::new,
                AssassinsCreedStealthBridge.MODID + "_syndicate"
        );
    }

    // --- Threat Level ---
    public int getThreatLevel(Player player) {
        return playerThreatLevels.getOrDefault(player.getUUID(), 0);
    }

    public void resetThreatLevel(Player player) {
        playerThreatLevels.put(player.getUUID(), 0);
        setDirty();
    }

    // --- Ambush Level ---
    public int getAmbushLevel(Player player) {
        return playerAmbushLevels.getOrDefault(player.getUUID(), 1);
    }

    public void incrementAmbushLevel(Player player) {
        int current = getAmbushLevel(player);
        playerAmbushLevels.put(player.getUUID(), current + 1);
        setDirty();
    }

    // --- Sightings ---
    public int getPendingPoints(Player player) {
        int points = 0;
        UUID pUuid = player.getUUID();
        for (Sighting sighting : activeSightings.values()) {
            if (sighting.playerUuid.equals(pUuid)) {
                points += sighting.points;
            }
        }
        return points;
    }

    public Sighting getSighting(LivingEntity informant) {
        return activeSightings.get(informant.getUUID());
    }

    public void addSighting(LivingEntity informant, Player player, int points) {
        if (!activeSightings.containsKey(informant.getUUID())) {
            activeSightings.put(informant.getUUID(), new Sighting(player.getUUID(), points));
            setDirty();
            //AssassinsCreedStealthBridge.LOGGER.info("[Syndicate Debug] SPION ALARM! " + informant.getName().getString() + " hat " + player.getName().getString() + " gesehen! (+" + points + " Pending Points)");
        }
    }

    public void onMobDeath(LivingEntity mob) {
        if (activeSightings.remove(mob.getUUID()) != null) {
            setDirty();
            //AssassinsCreedStealthBridge.LOGGER.info("[Syndicate Debug] Zeuge eliminiert! Eintrag für Mob " + mob.getUUID() + " wurde aus dem Hauptbuch gelöscht.");
        }
    }

    public void executeDailyReckoning(ServerLevel level) {
        //AssassinsCreedStealthBridge.LOGGER.info("[Syndicate Debug] Führe Daily Reckoning (Tagesabrechnung) durch...");
        
        Set<UUID> playersSeenToday = new HashSet<>();
        int threshold = SyndicateConfig.THREAT_THRESHOLD.get();

        // 1. Alle Einträge aus dem Hauptbuch verarbeiten und addieren
        for (Sighting sighting : activeSightings.values()) {
            UUID pUuid = sighting.playerUuid;
            int currentThreat = playerThreatLevels.getOrDefault(pUuid, 0);
            playerThreatLevels.put(pUuid, currentThreat + sighting.points);
            playersSeenToday.add(pUuid);
        }

        // 2. Chat-Nachrichten und Decay an alle Spieler verschicken
        for (UUID pUuid : playerThreatLevels.keySet()) {
            int finalThreat = playerThreatLevels.get(pUuid);
            ServerPlayer onlinePlayer = level.getServer().getPlayerList().getPlayer(pUuid);
            
            if (!playersSeenToday.contains(pUuid)) {
                // Decay (Punkte verfallen, wenn man nicht gesehen wurde)
                if (finalThreat > 0) {
                    finalThreat = Math.max(0, finalThreat - 10);
                    playerThreatLevels.put(pUuid, finalThreat);
                    if (onlinePlayer != null) {
                        onlinePlayer.displayClientMessage(Component.literal("§7Your tracks are getting colder, but you are not safe yet... §8(Threat: " + finalThreat + "/" + threshold + ")"), false);
                    }
                }
            } else {
                if (onlinePlayer != null) {
                    if (finalThreat >= threshold) {
                        onlinePlayer.displayClientMessage(Component.literal("§4You are marked. The Syndicate will strike today. Watch your back!"), false);
                    } else {
                        onlinePlayer.displayClientMessage(Component.literal("§cSomeone snitched on you... The Syndicate is tracking your movements. §8(Threat: " + finalThreat + "/" + threshold + ")"), false);
                    }
                }
            }
        }

        // 3. Hauptbuch leeren
        activeSightings.clear();
        setDirty();
        //AssassinsCreedStealthBridge.LOGGER.info("[Syndicate Debug] Daily Reckoning abgeschlossen. Hauptbuch wurde geleert.");
    }

    // --- Speichern & Laden (NBT) ---
    public static SyndicateManager load(CompoundTag tag) {
        SyndicateManager manager = new SyndicateManager();
        manager.lastReckoningDay = tag.getLong("LastReckoningDay");

        CompoundTag levelsTag = tag.getCompound("ThreatLevels");
        for (String key : levelsTag.getAllKeys()) {
            manager.playerThreatLevels.put(UUID.fromString(key), levelsTag.getInt(key));
        }

        CompoundTag ambushTag = tag.getCompound("AmbushLevels");
        for (String key : ambushTag.getAllKeys()) {
            manager.playerAmbushLevels.put(UUID.fromString(key), ambushTag.getInt(key));
        }

        ListTag sightingsList = tag.getList("Sightings", Tag.TAG_COMPOUND);
        for (int i = 0; i < sightingsList.size(); i++) {
            CompoundTag sTag = sightingsList.getCompound(i);
            UUID mobUuid = sTag.getUUID("MobUUID");
            UUID playerUuid = sTag.getUUID("PlayerUUID");
            int points = sTag.getInt("Points");
            manager.activeSightings.put(mobUuid, new Sighting(playerUuid, points));
        }
        return manager;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("LastReckoningDay", lastReckoningDay);

        CompoundTag levelsTag = new CompoundTag();
        for (Map.Entry<UUID, Integer> entry : playerThreatLevels.entrySet()) {
            levelsTag.putInt(entry.getKey().toString(), entry.getValue());
        }
        tag.put("ThreatLevels", levelsTag);

        CompoundTag ambushTag = new CompoundTag();
        for (Map.Entry<UUID, Integer> entry : playerAmbushLevels.entrySet()) {
            ambushTag.putInt(entry.getKey().toString(), entry.getValue());
        }
        tag.put("AmbushLevels", ambushTag);

        ListTag sightingsList = new ListTag();
        for (Map.Entry<UUID, Sighting> entry : activeSightings.entrySet()) {
            CompoundTag sTag = new CompoundTag();
            sTag.putUUID("MobUUID", entry.getKey());
            sTag.putUUID("PlayerUUID", entry.getValue().playerUuid);
            sTag.putInt("Points", entry.getValue().points);
            sightingsList.add(sTag);
        }
        tag.put("Sightings", sightingsList);

        return tag;
    }
}