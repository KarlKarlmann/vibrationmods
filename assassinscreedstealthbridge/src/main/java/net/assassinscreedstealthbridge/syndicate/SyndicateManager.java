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

    private final Map<UUID, Integer> playerThreatLevels = new HashMap<>();
    private final Map<UUID, Map<UUID, Sighting>> activeSightings = new HashMap<>();
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

    public int getThreatLevel(Player player) {
        return playerThreatLevels.getOrDefault(player.getUUID(), 0);
    }

    public void resetThreatLevel(Player player) {
        playerThreatLevels.put(player.getUUID(), 0);
        setDirty();
    }
    
    public void clearPendingPointsForPlayer(Player player) {
        UUID pUuid = player.getUUID();
        boolean changed = false;
        for (Map<UUID, Sighting> mobSightings : activeSightings.values()) {
            if (mobSightings.remove(pUuid) != null) {
                changed = true;
            }
        }
        if (changed) setDirty();
    }

    public int getPendingPoints(Player player) {
        int points = 0;
        UUID pUuid = player.getUUID();
        for (Map<UUID, Sighting> mobSightings : activeSightings.values()) {
            Sighting sighting = mobSightings.get(pUuid);
            if (sighting != null) {
                points += sighting.points;
            }
        }
        return points;
    }

    public Sighting getSightingForPlayer(LivingEntity informant, Player player) {
        Map<UUID, Sighting> mobSightings = activeSightings.get(informant.getUUID());
        return mobSightings != null ? mobSightings.get(player.getUUID()) : null;
    }

    public void addSighting(LivingEntity informant, Player player, int points) {
        activeSightings.computeIfAbsent(informant.getUUID(), k -> new HashMap<>())
                       .putIfAbsent(player.getUUID(), new Sighting(player.getUUID(), points));
        setDirty();
    }

    public void onMobDeath(LivingEntity mob) {
        if (activeSightings.remove(mob.getUUID()) != null) {
            setDirty();
        }
    }

    public void executeDailyReckoning(ServerLevel level) {
        Set<UUID> playersSeenToday = new HashSet<>();
        int threshold = SyndicateConfig.THREAT_THRESHOLD.get();

        for (Map<UUID, Sighting> mobSightings : activeSightings.values()) {
            for (Sighting sighting : mobSightings.values()) {
                UUID pUuid = sighting.playerUuid;
                int currentThreat = playerThreatLevels.getOrDefault(pUuid, 0);
                playerThreatLevels.put(pUuid, currentThreat + sighting.points);
                playersSeenToday.add(pUuid);
            }
        }

        for (UUID pUuid : playerThreatLevels.keySet()) {
            int finalThreat = playerThreatLevels.get(pUuid);
            ServerPlayer onlinePlayer = level.getServer().getPlayerList().getPlayer(pUuid);
            
            if (!playersSeenToday.contains(pUuid)) {
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

        activeSightings.clear();
        setDirty();
    }

    public static SyndicateManager load(CompoundTag tag) {
        SyndicateManager manager = new SyndicateManager();
        manager.lastReckoningDay = tag.getLong("LastReckoningDay");

        CompoundTag levelsTag = tag.getCompound("ThreatLevels");
        for (String key : levelsTag.getAllKeys()) {
            manager.playerThreatLevels.put(UUID.fromString(key), levelsTag.getInt(key));
        }

        ListTag sightingsList = tag.getList("Sightings", Tag.TAG_COMPOUND);
        for (int i = 0; i < sightingsList.size(); i++) {
            CompoundTag sTag = sightingsList.getCompound(i);
            UUID mobUuid = sTag.getUUID("MobUUID");
            UUID playerUuid = sTag.getUUID("PlayerUUID");
            int points = sTag.getInt("Points");
            
            manager.activeSightings.computeIfAbsent(mobUuid, k -> new HashMap<>())
                                   .put(playerUuid, new Sighting(playerUuid, points));
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

        ListTag sightingsList = new ListTag();
        for (Map.Entry<UUID, Map<UUID, Sighting>> mobEntry : activeSightings.entrySet()) {
            UUID mobUuid = mobEntry.getKey();
            for (Sighting sighting : mobEntry.getValue().values()) {
                CompoundTag sTag = new CompoundTag();
                sTag.putUUID("MobUUID", mobUuid);
                sTag.putUUID("PlayerUUID", sighting.playerUuid);
                sTag.putInt("Points", sighting.points);
                sightingsList.add(sTag);
            }
        }
        tag.put("Sightings", sightingsList);

        return tag;
    }
}