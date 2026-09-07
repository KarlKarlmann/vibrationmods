package net.assassinscreedstealthbridge.syndicate;

import net.assassinscreedstealthbridge.AssassinsCreedStealthBridge;
import net.assassinscreedstealthbridge.config.SyndicateConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.stealth.registry.StealthSounds;
import net.assassinscreedstealthbridge.network.SyndicateNetwork;
import net.assassinscreedstealthbridge.registry.BridgeTags;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = AssassinsCreedStealthBridge.MODID)
public class SyndicateEvents {

    private static final String SYNDICATE_MEMBER_KEY = "AC_SyndicateMemberId";

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

            if (player.isCrouching()) {
                if (player.tickCount % 20 == 0) {
                    player.displayClientMessage(Component.literal("§8[Eagle Vision] §cThreat: " + threat + "/" + threshold + " §7| §ePending: +" + pending), true);
                }
                
                if (player.tickCount % 10 == 0) {
                    AABB searchBox = player.getBoundingBox().inflate(32.0);
                    for (Mob mob : level.getEntitiesOfClass(Mob.class, searchBox)) {
                        SyndicateManager.Sighting sighting = manager.getSightingForPlayer(mob, player);
                        
                        ResourceLocation mobKey = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
                        boolean isTemplar = mobKey != null && mobKey.getNamespace().equals("assassins_creed") && mobKey.getPath().contains("templar");
                        boolean isSyndicateMember = mob.getPersistentData().hasUUID(SYNDICATE_MEMBER_KEY);

                        if (sighting != null || isTemplar || isSyndicateMember) {
                            level.sendParticles((ServerPlayer) player, ParticleTypes.DAMAGE_INDICATOR, false, 
                                    mob.getX(), mob.getEyeY() + 0.8, mob.getZ(), 
                                    1, 0.1, 0.1, 0.1, 0.0);
                        }
                    }
                }
            }

            if (threat >= threshold) {
                if (!isSafeToAmbush(player, level)) return;
                
                if (level.random.nextInt(300) == 0) {
                    player.displayClientMessage(Component.literal("§4The Syndicate has found you!").withStyle(net.minecraft.ChatFormatting.BOLD), true);
                    triggerSyndicateAmbush(player, level, manager);
                    manager.resetThreatLevel(player);
                } else if (level.random.nextInt(400) == 0 && !player.isCrouching()) { 
                    level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.5f, 0.6f);
                    player.displayClientMessage(Component.literal("§cYou feel like you are being watched...").withStyle(net.minecraft.ChatFormatting.ITALIC), true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onMobDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        ServerLevel level = (ServerLevel) event.getEntity().level();
        SyndicateManager.get(level).onMobDeath(event.getEntity());

        CompoundTag persistentData = event.getEntity().getPersistentData();
        
        // 1. RAID BOSS LOOT (Droppt physisch beim Tod, da die Agenten nicht mehr auf dem Board existieren)
        if (persistentData.contains("AC_RaidRewardFlavor")) {
            String flavor = persistentData.getString("AC_RaidRewardFlavor");
            int tier = persistentData.getInt("AC_RaidRewardTier");
            
            // Droppt die Kiste für den eigenen Rang
            dropRewardBox(level, event.getEntity(), flavor, tier);
            
            // Der Leader hat die Tier 5 Kiste im Gepäck!
            if (persistentData.getBoolean("AC_DropTier5")) {
                dropRewardBox(level, event.getEntity(), flavor, 5);
            }
            return; // Beenden, damit keine Capture-Logik mehr ausgeführt wird!
        }

        // 2. NORMALES BOARD CAPTURE (für reguläre Ambushes)
        if (persistentData.hasUUID(SYNDICATE_MEMBER_KEY)) {
            UUID memberId = persistentData.getUUID(SYNDICATE_MEMBER_KEY);
            SyndicateBoardManager board = SyndicateBoardManager.get(level);
            SyndicateMember member = board.getMember(memberId);

            if (member != null && event.getSource().getEntity() instanceof Player player) {
                member.capturedBy = player.getUUID();
                board.setDirty();

                player.displayClientMessage(Component.literal("§8[Syndicate] §fYou defeated " + member.name 
                        + ". Return to the DaVinci Table to decide their fate."), false);
                
                // 30% Chance auf einen Codex Drop
                if (level.random.nextFloat() < 0.3f) {
                    Item codexItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation("assassins_creed", "kodeks"));
                    if (codexItem != null && codexItem != Items.AIR) {
                        event.getEntity().spawnAtLocation(new ItemStack(codexItem));
                    }
                }
            }
        }
    }
    
    // Hilfsmethode, um Reward Boxen physisch in der Welt droppen zu lassen
    private static void dropRewardBox(ServerLevel level, Entity entity, String boxId, int tier) {
        Item boxItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation("reward_box", "reward_chest"));
        if (boxItem == null || boxItem == Items.AIR) boxItem = Items.CHEST; 
        
        ItemStack reward = new ItemStack(boxItem, 1);
        CompoundTag tag = reward.getOrCreateTag();
        tag.putString("BoxId", boxId);
        tag.putInt("RewardTier", tier);
        
        String readableName = boxId.replace("reward_box:", ""); 
        readableName = readableName.substring(0, 1).toUpperCase() + readableName.substring(1).replace("_", " "); 
        net.minecraft.network.chat.MutableComponent displayName = net.minecraft.network.chat.Component.literal("Tier " + tier + " " + readableName + " Box")
            .withStyle(net.minecraft.ChatFormatting.GOLD)
            .withStyle(net.minecraft.ChatFormatting.BOLD);
            
        reward.setHoverName(displayName);
        entity.spawnAtLocation(reward);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSyndicateBoardInteract(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return; 

        BlockState state = event.getLevel().getBlockState(event.getPos());
        boolean hasTag = state.is(BridgeTags.SYNDICATE_BOARD);

        if (!hasTag) return;

        event.setCanceled(true);
        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.DENY);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);

        if (event.getEntity() instanceof ServerPlayer player) {
            SyndicateBoardManager board = SyndicateBoardManager.get((ServerLevel) event.getLevel());
            CompoundTag boardData = board.save(new CompoundTag());
            SyndicateNetwork.sendToPlayer(new SyndicateNetwork.OpenBoardPacket(boardData), player);
        }
    }

    @SubscribeEvent
    public static void onTraderBribe(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof net.minecraft.world.entity.npc.WanderingTrader trader) {
            Player player = event.getEntity();
            ItemStack item = event.getItemStack();

            boolean isEmeraldBlock = item.is(Items.EMERALD_BLOCK);

            if (player.isCrouching() && isEmeraldBlock) {
                if (!player.level().isClientSide()) {
                    ServerLevel level = (ServerLevel) player.level();
                    SyndicateManager manager = SyndicateManager.get(level);

                    int currentThreat = manager.getThreatLevel(player);
                    int pendingThreat = manager.getPendingPoints(player);

                    if (currentThreat > 0 || pendingThreat > 0) {
                        manager.resetThreatLevel(player);
                        manager.clearPendingPointsForPlayer(player);
                        
                        if (!player.isCreative()) {
                            item.shrink(1);
                        }

                        level.playSound(null, trader.blockPosition(), net.minecraft.sounds.SoundEvents.WANDERING_TRADER_YES, SoundSource.NEUTRAL, 1.0f, 1.0f);
                        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, trader.getX(), trader.getEyeY() + 1.0, trader.getZ(), 15, 0.5, 0.5, 0.5, 0.0);
                        player.displayClientMessage(Component.literal("§a[Information Broker] Your Syndicate ledger has been wiped clean.").withStyle(net.minecraft.ChatFormatting.ITALIC), true);
                    } else {
                        player.displayClientMessage(Component.literal("§7[Information Broker] You have no bounty on your head. Keep your currency."), true);
                    }
                }
                
                event.setCanceled(true);
                event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            }
        }
    }

    @SubscribeEvent
    public static void onCodexUse(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        
        if (itemId != null && itemId.getNamespace().equals("assassins_creed") && itemId.getPath().equals("kodeks")) {
            Player player = event.getEntity();
            Level level = event.getLevel();

            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);

            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                SyndicateBoardManager board = SyndicateBoardManager.get((ServerLevel) level);
                
                List<net.assassinscreedstealthbridge.syndicate.SyndicateDivision> availableDivisions = board.getActiveDivisions();
                availableDivisions.removeIf(d -> d.intel >= 100);
                
                if (!availableDivisions.isEmpty()) {
                    net.assassinscreedstealthbridge.syndicate.SyndicateDivision randomDiv = availableDivisions.get(level.random.nextInt(availableDivisions.size()));
                    int intelGain = 1 + level.random.nextInt(11); 
                    randomDiv.intel = Math.min(100, randomDiv.intel + intelGain);
                    board.setDirty();
                    
                    serverPlayer.sendSystemMessage(Component.literal("§a[Syndicate] §fDeciphered Codex pages! §e+" + intelGain + "% Intel §ffor " + randomDiv.name));
                } else {
                    serverPlayer.sendSystemMessage(Component.literal("§7[Syndicate] You already know everything there is to know... for now."));
                }

                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                
                level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 1.0f);

                CompoundTag boardData = board.save(new CompoundTag());
                SyndicateNetwork.sendToPlayer(new SyndicateNetwork.OpenBoardPacket(boardData), serverPlayer);
            }
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

    public static void triggerSyndicateAmbush(Player player, ServerLevel level, SyndicateManager manager) {
        SyndicateBoardManager board = SyndicateBoardManager.get(level);
        List<SyndicateMember> activeMembers = board.getAssignedMembers();
        
        if (activeMembers.isEmpty()) return;

        // 1. Wähle den garantierten Initiator
        SyndicateMember initiator = activeMembers.get(level.random.nextInt(activeMembers.size()));
        List<SyndicateMember> ambushTeam = new ArrayList<>();
        ambushTeam.add(initiator);

        // 2. Füge vertraute Freunde hinzu
        for (UUID friendId : initiator.trusted) {
            SyndicateMember friend = board.getMember(friendId);
            if (friend != null && friend.isAssigned() && friend.capturedBy == null && !ambushTeam.contains(friend)) {
                ambushTeam.add(friend);
            }
        }

        // 3. Mischen aus den AKTIVEN Fraktionen abhängig vom Intel
        for (net.assassinscreedstealthbridge.syndicate.SyndicateDivision div : board.getActiveDivisions()) {
            if (div.intel > 0) {
                List<SyndicateMember> availableRegulars = new ArrayList<>();
                SyndicateMember availableLeader = null;
                
                // Sortiere verfügbare Agenten
                for (UUID mId : div.memberIds) {
                    SyndicateMember m = board.getMember(mId);
                    if (m != null && m.isAssigned() && m.capturedBy == null && !ambushTeam.contains(m)) {
                        if (m.leader) availableLeader = m;
                        else availableRegulars.add(m);
                    }
                }
                
                // Mische die normalen Agenten
                Collections.shuffle(availableRegulars, new java.util.Random(level.random.nextLong()));

                // Gestaffelte Chancen basierend auf Intel (1. Normal, 2. Normal, 3. Leader)
                if (!availableRegulars.isEmpty() && level.random.nextInt(100) < div.intel) {
                    ambushTeam.add(availableRegulars.remove(0));
                }
                if (!availableRegulars.isEmpty() && level.random.nextInt(100) < (div.intel / 2)) {
                    ambushTeam.add(availableRegulars.remove(0));
                }
                if (availableLeader != null && level.random.nextInt(100) < (div.intel / 4)) {
                    ambushTeam.add(availableLeader);
                }
            }
        }

        // Maximal 6 Boss-Agenten
        if (ambushTeam.size() > 6) {
            Collections.shuffle(ambushTeam, new java.util.Random(level.random.nextLong()));
            ambushTeam = ambushTeam.subList(0, 6);
        }

        List<Map.Entry<Mob, SyndicateMember>> spawnedPairs = new ArrayList<>();
        int totalStars = 0;

        // Spawne das Team
        for (SyndicateMember member : ambushTeam) {
            totalStars += member.rank;

            // Das Modell bleibt durch UUID-Hash immer identisch!
            int modelIndex = Math.abs(member.id.hashCode()) % 6; 
            String mobId = "assassins_creed:templar" + (modelIndex == 0 ? "" : "_" + (modelIndex + 1));

            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(mobId));
            if (type == null) type = EntityType.ZOMBIE;

            Entity entity = type.create(level);
            if (entity instanceof Mob ambusher) {
                double angle = level.random.nextDouble() * 2 * Math.PI;
                double distance = 8.0 + level.random.nextDouble() * 7.0; 
                
                double spawnX = player.getX() + distance * Math.cos(angle);
                double spawnZ = player.getZ() + distance * Math.sin(angle);
                double spawnY = player.getY(); 
                
                ambusher.moveTo(spawnX, spawnY, spawnZ, level.random.nextFloat() * 360F, 0.0F);

                ambusher.setCustomName(Component.literal("§c" + member.getRankStars() + " " + member.name));
                ambusher.setCustomNameVisible(true);
                ambusher.getPersistentData().putUUID(SYNDICATE_MEMBER_KEY, member.id);

                applyRankBonus(ambusher, member.rank);
                applySignatureEffect(ambusher, member);
                spawnedPairs.add(new AbstractMap.SimpleEntry<>(ambusher, member));

                ambusher.setTarget(player);
                level.addFreshEntity(ambusher);
                
                BlockPos spawnPos = BlockPos.containing(spawnX, spawnY, spawnZ);
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, spawnX, spawnY + 1.0, spawnZ, 30, 0.5, 1.0, 0.5, 0.05);
                level.sendParticles(ParticleTypes.SQUID_INK, spawnX, spawnY + 1.0, spawnZ, 20, 0.5, 1.0, 0.5, 0.1);
                level.playSound(null, spawnPos, StealthSounds.BACKSTAB.get(), SoundSource.HOSTILE, 1.5f, 0.5f);
            }
        }

        // Grunts spawnen exakt basierend auf der Sterne-Anzahl!
        int gruntCount = totalStars;
        EntityType<?> gruntType = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("assassins_creed:templar"));
        if (gruntType != null && gruntCount > 0) {
            for (int i = 0; i < gruntCount; i++) {
                Entity grunt = gruntType.create(level);
                if (grunt instanceof Mob gMob) {
                    double angle = level.random.nextDouble() * 2 * Math.PI;
                    double distance = 6.0 + level.random.nextDouble() * 6.0; 
                    gMob.moveTo(player.getX() + distance * Math.cos(angle), player.getY(), player.getZ() + distance * Math.sin(angle), level.random.nextFloat() * 360F, 0.0F);
                    gMob.setTarget(player);
                    level.addFreshEntity(gMob);
                    level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, gMob.getX(), gMob.getY() + 1.0, gMob.getZ(), 30, 0.5, 1.0, 0.5, 0.05);
                }
            }
        }

        applyRelationshipEffects(spawnedPairs, level);
    }

    private static void applyRankBonus(Mob ambusher, int rank) {
        int extraStars = rank - SyndicateMember.RANK_SERGEANT;
        if (extraStars <= 0) return;

        if (ambusher.getAttribute(Attributes.MAX_HEALTH) != null) {
            double newMax = ambusher.getAttribute(Attributes.MAX_HEALTH).getBaseValue() + extraStars * 4.0;
            ambusher.getAttribute(Attributes.MAX_HEALTH).setBaseValue(newMax);
            ambusher.setHealth((float) newMax);
        }
        if (ambusher.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            double newDamage = ambusher.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue() + extraStars * 1.0;
            ambusher.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(newDamage);
        }
    }

    private static void applySignatureEffect(Mob ambusher, SyndicateMember member) {
        if (member.signatureEffect != null && !member.signatureEffect.isEmpty() && !member.signatureEffect.equals("none")) {
            net.minecraft.world.effect.MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(member.signatureEffect));
            if (effect != null) {
                ambusher.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect, 999999, Math.max(0, member.rank - 1), false, true));
            }
        }
    }

    public static void triggerDesperateAssault(Player player, ServerLevel level, net.assassinscreedstealthbridge.syndicate.SyndicateDivision div) {
        SyndicateBoardManager board = SyndicateBoardManager.get(level);
        
        player.playSound(net.minecraft.sounds.SoundEvents.WITHER_SPAWN, 1.0f, 0.5f);
        
        int totalStars = 0;

        for (UUID memberId : div.memberIds) {
            SyndicateMember member = board.getMember(memberId);
            if (member != null && member.isAssigned()) {
                totalStars += member.rank;

                int modelIndex = Math.abs(member.id.hashCode()) % 6; 
                String mobId = "assassins_creed:templar" + (modelIndex == 0 ? "" : "_" + (modelIndex + 1));
                
                EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(mobId));
                if (type == null) type = EntityType.ZOMBIE;
                
                Entity entity = type.create(level);
                if (entity instanceof Mob ambusher) {
                    double angle = level.random.nextDouble() * 2 * Math.PI;
                    double distance = 5.0 + level.random.nextDouble() * 5.0; 
                    
                    ambusher.moveTo(player.getX() + distance * Math.cos(angle), player.getY(), player.getZ() + distance * Math.sin(angle), 0, 0);
                    
                    ambusher.setCustomName(Component.literal(member.leader ? "§6[Leader] " + member.name : "§c" + member.getRankStars() + " " + member.name));
                    ambusher.setCustomNameVisible(true);
                    
                    // NEU: Schreibe den Loot direkt in den Boss hinein, damit er beim Tod droppt!
                    CompoundTag data = ambusher.getPersistentData();
                    data.putString("AC_RaidRewardFlavor", div.rewardFlavor);
                    data.putInt("AC_RaidRewardTier", Math.max(1, member.rank)); 
                    if (member.leader) {
                        data.putBoolean("AC_DropTier5", true); 
                    }
                    
                    applyRankBonus(ambusher, member.rank + 1); 
                    applySignatureEffect(ambusher, member);
                    
                    ambusher.setTarget(player);
                    level.addFreshEntity(ambusher);
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, ambusher.getX(), ambusher.getY() + 1.0, ambusher.getZ(), 20, 0.5, 1.0, 0.5, 0.1);
                }
            }
        }
        
        // Massive Verteidigungs-Welle an Grunts
        int gruntCount = totalStars + 2; 
        EntityType<?> gruntType = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("assassins_creed:templar"));
        if (gruntType != null && gruntCount > 0) {
            for (int i = 0; i < gruntCount; i++) {
                Entity grunt = gruntType.create(level);
                if (grunt instanceof Mob gMob) {
                    double angle = level.random.nextDouble() * 2 * Math.PI;
                    double distance = 6.0 + level.random.nextDouble() * 6.0; 
                    gMob.moveTo(player.getX() + distance * Math.cos(angle), player.getY(), player.getZ() + distance * Math.sin(angle), level.random.nextFloat() * 360F, 0.0F);
                    gMob.setTarget(player);
                    level.addFreshEntity(gMob);
                    level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, gMob.getX(), gMob.getY() + 1.0, gMob.getZ(), 30, 0.5, 1.0, 0.5, 0.1);
                }
            }
        }
    }

    private static void applyRelationshipEffects(List<Map.Entry<Mob, SyndicateMember>> spawnedPairs, ServerLevel level) {
        for (int i = 0; i < spawnedPairs.size(); i++) {
            for (int j = i + 1; j < spawnedPairs.size(); j++) {
                Mob mobA = spawnedPairs.get(i).getKey();
                SyndicateMember memA = spawnedPairs.get(i).getValue();
                Mob mobB = spawnedPairs.get(j).getKey();
                SyndicateMember memB = spawnedPairs.get(j).getValue();

                if (memA.trusted.contains(memB.id)) {
                    buffTrustedPair(mobA);
                    buffTrustedPair(mobB);
                } else if (memA.rivals.contains(memB.id)) {
                    if (level.random.nextBoolean()) mobA.setTarget(mobB);
                    else mobB.setTarget(mobA);
                }
            }
        }
    }

    private static void buffTrustedPair(Mob mob) {
        if (mob.getAttribute(Attributes.MAX_HEALTH) != null) {
            double newMax = mob.getAttribute(Attributes.MAX_HEALTH).getBaseValue() * 1.2;
            mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(newMax);
            mob.setHealth((float) newMax);
        }
        if (mob.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            double newDamage = mob.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue() * 1.15;
            mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(newDamage);
        }
    }
}