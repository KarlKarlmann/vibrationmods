package net.assassinscreedstealthbridge.syndicate;

import net.assassinscreedstealthbridge.AssassinsCreedStealthBridge;
import net.assassinscreedstealthbridge.config.SyndicateConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class SyndicateBoardManager extends SavedData {

    private final Map<UUID, SyndicateMember> members = new LinkedHashMap<>();
    private final Map<String, SyndicateDivision> divisions = new LinkedHashMap<>();
    
    // Hält fest, welche 4 Fraktionen aktuell aktiv auf dem Board sind
    private final List<String> activeDivisionIds = new ArrayList<>();
    
    private boolean initialized = false;

    public static SyndicateBoardManager get(ServerLevel level) {
        SyndicateBoardManager manager = level.getDataStorage().computeIfAbsent(
                SyndicateBoardManager::load,
                SyndicateBoardManager::new,
                AssassinsCreedStealthBridge.MODID + "_syndicate_board"
        );
        if (!manager.initialized) {
            manager.initializeBoard();
        }
        return manager;
    }

    private void initializeBoard() {
        initialized = true;
        if (!members.isEmpty()) {
            return; 
        }

        List<? extends String> divisionDefs = SyndicateConfig.SYNDICATE_DIVISIONS.get();

        // 1. Alle 8 Divisionen aus Config anlegen (aber noch nicht füllen)
        if (divisionDefs != null) {
            for (String def : divisionDefs) {
                String[] parts = def.split("\\|");
                if (parts.length < 2) continue;
                String id = parts[0];
                String displayName = parts[1];
                String flavor = parts.length > 2 ? parts[2] : "reward_box:default";
                divisions.put(id, new SyndicateDivision(id, displayName, flavor));
            }
        }

        if (divisions.isEmpty()) return;

        // 2. Wähle exakt 4 zufällige Fraktionen aus, die aktiv starten
        List<String> allIds = new ArrayList<>(divisions.keySet());
        Random rand = new Random();
        while (activeDivisionIds.size() < 4 && !allIds.isEmpty()) {
            String picked = allIds.remove(rand.nextInt(allIds.size()));
            activeDivisionIds.add(picked);
        }

        // 3. Fülle NUR die 4 aktiven Divisionen
        for (String activeId : activeDivisionIds) {
            populateDivision(divisions.get(activeId));
        }

        generateRelationships();
        setDirty();
    }

    private void populateDivision(SyndicateDivision division) {
        List<? extends String> firstNames = SyndicateConfig.SYNDICATE_FIRST_NAMES.get();
        List<? extends String> lastNames = SyndicateConfig.SYNDICATE_LAST_NAMES.get();
        Random rand = new Random();
        String[] randomEffects = {"minecraft:speed", "minecraft:strength", "minecraft:resistance", "minecraft:jump_boost", "none"};

        for (int i = 0; i < 3; i++) {
            UUID randomId = UUID.randomUUID(); 
            String rEffect = randomEffects[rand.nextInt(randomEffects.length)];
            
            String fn = (firstNames != null && !firstNames.isEmpty()) ? firstNames.get(rand.nextInt(firstNames.size())) : "Unknown";
            String ln = (lastNames != null && !lastNames.isEmpty()) ? lastNames.get(rand.nextInt(lastNames.size())) : "Agent";
            
            SyndicateMember member = new SyndicateMember(randomId, fn + " " + ln, division.id, SyndicateMember.RANK_SERGEANT, false, rEffect);
            
            members.put(member.id, member);
            division.memberIds.add(member.id);
        }
        
        // Den Ersten zum Leader machen
        if (!division.memberIds.isEmpty()) {
            UUID leaderId = division.memberIds.get(0);
            division.leaderId = leaderId;
            members.get(leaderId).leader = true;
        }
    }

    private void generateRelationships() {
        Random random = new Random();
        for (String divId : activeDivisionIds) {
            SyndicateDivision division = divisions.get(divId);
            if (division == null) continue;
            List<UUID> ids = division.memberIds;
            for (int i = 0; i < ids.size(); i++) {
                for (int j = i + 1; j < ids.size(); j++) {
                    double roll = random.nextDouble();
                    if (roll < 0.2) {
                        members.get(ids.get(i)).trusted.add(ids.get(j));
                        members.get(ids.get(j)).trusted.add(ids.get(i));
                    } else if (roll < 0.35) {
                        members.get(ids.get(i)).rivals.add(ids.get(j));
                        members.get(ids.get(j)).rivals.add(ids.get(i));
                    }
                }
            }
        }
    }

    public void replaceDivision(String oldDivId) {
        SyndicateDivision oldDiv = divisions.get(oldDivId);
        if (oldDiv == null) return;

        // 1. Alte Mitglieder restlos löschen
        for (UUID mId : oldDiv.memberIds) {
            members.remove(mId);
            // Beziehungen bei anderen aufräumen
            for (SyndicateMember other : members.values()) {
                other.trusted.remove(mId);
                other.rivals.remove(mId);
            }
        }
        oldDiv.memberIds.clear();
        oldDiv.leaderId = null;
        oldDiv.intel = 0;
        activeDivisionIds.remove(oldDivId);

        // 2. Suche eine Fraktion, die gerade nicht auf dem Board ist
        List<String> inactiveIds = new ArrayList<>(divisions.keySet());
        inactiveIds.removeAll(activeDivisionIds);
        
        if (!inactiveIds.isEmpty()) {
            String newId = inactiveIds.get(new Random().nextInt(inactiveIds.size()));
            activeDivisionIds.add(newId);
            populateDivision(divisions.get(newId));
        }

        generateRelationships();
        setDirty();
    }

    public List<SyndicateDivision> getActiveDivisions() {
        List<SyndicateDivision> active = new ArrayList<>();
        for (String id : activeDivisionIds) {
            SyndicateDivision d = divisions.get(id);
            if (d != null) active.add(d);
        }
        return active;
    }

    public SyndicateMember getMember(UUID id) {
        return members.get(id);
    }

    public Collection<SyndicateMember> getAllMembers() {
        return members.values();
    }

    public SyndicateDivision getDivision(String id) {
        return id == null ? null : divisions.get(id);
    }

    public Collection<SyndicateDivision> getAllDivisions() {
        return divisions.values();
    }

    public List<SyndicateMember> getAssignedMembers() {
        List<SyndicateMember> result = new ArrayList<>();
        for (SyndicateMember m : members.values()) {
            if (m.isAssigned()) result.add(m);
        }
        return result;
    }

    public void executeMember(SyndicateMember member) {
        if (member == null) return;
        member.rank = Math.min(SyndicateMember.RANK_CAPTAIN, member.rank + 1);
        member.capturedBy = null; 
        setDirty();
    }

    public void interrogateMember(SyndicateMember member) {
        if (member == null) return;
        member.rank -= 1;
        
        // Wenn ein Mitglied Rang 0 erreicht, wird es "verbrannt" und neu ausgewürfelt
        if (member.rank <= 0) {
            rerollMember(member);
        }
        
        member.capturedBy = null; 
        setDirty();
    }

    private void rerollMember(SyndicateMember member) {
        SyndicateDivision division = divisions.get(member.divisionId);
        
        if (member.leader && division != null) {
            member.leader = false;
            promoteNewLeader(division, member.id);
        }

        List<? extends String> firstNames = SyndicateConfig.SYNDICATE_FIRST_NAMES.get();
        List<? extends String> lastNames = SyndicateConfig.SYNDICATE_LAST_NAMES.get();
        Random rand = new Random();
        String[] randomEffects = {"minecraft:speed", "minecraft:strength", "minecraft:resistance", "minecraft:jump_boost", "none"};

        if (firstNames != null && !firstNames.isEmpty() && lastNames != null && !lastNames.isEmpty()) {
            member.name = firstNames.get(rand.nextInt(firstNames.size())) + " " + lastNames.get(rand.nextInt(lastNames.size()));
        }

        member.signatureEffect = randomEffects[rand.nextInt(randomEffects.length)];
        member.rank = SyndicateMember.RANK_SERGEANT; // Startet als frischer Sergeant

        // Beziehungen kappen (er ist ja nun jemand völlig Neues)
        member.trusted.clear();
        member.rivals.clear();
        for (SyndicateMember m : members.values()) {
            m.trusted.remove(member.id);
            m.rivals.remove(member.id);
        }
    }

    private void promoteNewLeader(SyndicateDivision division, UUID exLeaderId) {
        SyndicateMember best = null;
        for (UUID memberId : division.memberIds) {
            if (memberId.equals(exLeaderId)) continue;
            SyndicateMember candidate = members.get(memberId);
            if (candidate == null) continue;
            if (best == null || candidate.rank > best.rank) best = candidate;
        }

        if (best != null) {
            best.leader = true;
            division.leaderId = best.id;
        } else {
            division.leaderId = null;
        }
    }

    public static SyndicateBoardManager load(CompoundTag tag) {
        SyndicateBoardManager manager = new SyndicateBoardManager();

        // 1. Lade IMMER alle 8 Config-Fraktionen frisch rein, um den Pool an Inaktiven zu sichern!
        List<? extends String> divisionDefs = SyndicateConfig.SYNDICATE_DIVISIONS.get();
        if (divisionDefs != null) {
            for (String def : divisionDefs) {
                String[] parts = def.split("\\|");
                if (parts.length >= 2) {
                    String flavor = parts.length > 2 ? parts[2] : "reward_box:default";
                    manager.divisions.put(parts[0], new SyndicateDivision(parts[0], parts[1], flavor));
                }
            }
        }

        // 2. Lade Active Divisions aus NBT
        ListTag activeList = tag.getList("ActiveDivisions", Tag.TAG_STRING);
        for (int i = 0; i < activeList.size(); i++) {
            manager.activeDivisionIds.add(activeList.getString(i));
        }

        // 3. Überschreibe den Fortschritt der gespeicherten Divisionen
        ListTag divisionsList = tag.getList("Divisions", Tag.TAG_COMPOUND);
        for (int i = 0; i < divisionsList.size(); i++) {
            CompoundTag dTag = divisionsList.getCompound(i);
            String id = dTag.getString("Id");
            
            SyndicateDivision division = manager.divisions.get(id);
            if (division != null) {
                division.intel = dTag.getInt("Intel");
                
                if (dTag.hasUUID("Leader")) {
                    division.leaderId = dTag.getUUID("Leader");
                }
                ListTag memberIdsList = dTag.getList("MemberIds", Tag.TAG_INT_ARRAY);
                for (int j = 0; j < memberIdsList.size(); j++) {
                    division.memberIds.add(NbtUtils.loadUUID(memberIdsList.get(j)));
                }
            }
        }

        // 4. Lade die gespeicherten Mitglieder
        ListTag membersList = tag.getList("Members", Tag.TAG_COMPOUND);
        for (int i = 0; i < membersList.size(); i++) {
            CompoundTag mTag = membersList.getCompound(i);
            UUID id = mTag.getUUID("Id");
            String name = mTag.getString("Name");
            String divisionId = mTag.getString("DivisionId");
            int rank = mTag.getInt("Rank");
            boolean leader = mTag.getBoolean("Leader");
            String effect = mTag.getString("Effect");
            SyndicateMember member = new SyndicateMember(id, name, divisionId, rank, leader, effect);

            if (mTag.hasUUID("CapturedBy")) {
                member.capturedBy = mTag.getUUID("CapturedBy");
            }

            ListTag trustedList = mTag.getList("Trusted", Tag.TAG_INT_ARRAY);
            for (int j = 0; j < trustedList.size(); j++) {
                member.trusted.add(NbtUtils.loadUUID(trustedList.get(j)));
            }
            ListTag rivalsList = mTag.getList("Rivals", Tag.TAG_INT_ARRAY);
            for (int j = 0; j < rivalsList.size(); j++) {
                member.rivals.add(NbtUtils.loadUUID(rivalsList.get(j)));
            }

            manager.members.put(id, member);
        }

        manager.initialized = !manager.members.isEmpty();
        
        // Failsafe: Falls die Welt alt ist und keine aktiven IDs gespeichert hat
        if (manager.activeDivisionIds.isEmpty()) {
            for (SyndicateDivision d : manager.divisions.values()) {
                if (!d.memberIds.isEmpty() && manager.activeDivisionIds.size() < 4) {
                    manager.activeDivisionIds.add(d.id);
                } else if (!manager.activeDivisionIds.contains(d.id)) {
                    d.memberIds.clear(); // Leert nicht verwendete Fraktionen aus alten Savegames
                }
            }
        }
        
        return manager;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        // Speichere Active Divisions
        ListTag activeList = new ListTag();
        for (String id : activeDivisionIds) {
            activeList.add(StringTag.valueOf(id));
        }
        tag.put("ActiveDivisions", activeList);

        ListTag divisionsList = new ListTag();
        for (SyndicateDivision division : divisions.values()) {
            CompoundTag dTag = new CompoundTag();
            dTag.putString("Id", division.id);
            dTag.putString("Name", division.name);
            dTag.putString("Flavor", division.rewardFlavor);
            dTag.putInt("Intel", division.intel);
            
            if (division.leaderId != null) {
                dTag.putUUID("Leader", division.leaderId);
            }
            ListTag memberIdsList = new ListTag();
            for (UUID id : division.memberIds) {
                memberIdsList.add(NbtUtils.createUUID(id));
            }
            dTag.put("MemberIds", memberIdsList);
            divisionsList.add(dTag);
        }
        tag.put("Divisions", divisionsList);

        ListTag membersList = new ListTag();
        for (SyndicateMember member : members.values()) {
            CompoundTag mTag = new CompoundTag();
            mTag.putUUID("Id", member.id);
            mTag.putString("Name", member.name);
            mTag.putString("DivisionId", member.divisionId == null ? "" : member.divisionId);
            mTag.putInt("Rank", member.rank);
            mTag.putBoolean("Leader", member.leader);
            mTag.putString("Effect", member.signatureEffect == null ? "none" : member.signatureEffect);
            
            if (member.capturedBy != null) {
                mTag.putUUID("CapturedBy", member.capturedBy);
            }

            ListTag trustedList = new ListTag();
            for (UUID id : member.trusted) {
                trustedList.add(NbtUtils.createUUID(id));
            }
            mTag.put("Trusted", trustedList);

            ListTag rivalsList = new ListTag();
            for (UUID id : member.rivals) {
                rivalsList.add(NbtUtils.createUUID(id));
            }
            mTag.put("Rivals", rivalsList);

            membersList.add(mTag);
        }
        tag.put("Members", membersList);

        return tag;
    }
}