package net.assassinscreedstealthbridge.mixins;

import net.assassinscreedstealthbridge.syndicate.SyndicateManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.assassinscreedstealthbridge.config.SyndicateConfig;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.phys.AABB;
import net.stealth.util.StealthMath;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Verteilt die Rechenlast auf die Entitäten selbst. 
 * Agiert als "unsichtbare" Snitch-AI für alle in der Config definierten Mobs.
 */
@Mixin(Mob.class)
public abstract class MixinSnitchAI {

    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void stealth$snitchOnPlayer(CallbackInfo ci) {
        Mob informant = (Mob)(Object)this;

        // Läuft nur alle 40 Ticks (2 Sekunden) für eine perfekte Performance
        if (informant.tickCount % 40 == 0 && !informant.level().isClientSide()) {
            
            ResourceLocation rl = ForgeRegistries.ENTITY_TYPES.getKey(informant.getType());
            if (rl == null) return;
            String entityId = rl.toString();

            int maxPoints = -1;
            // Prüfen, ob dieser Mob in der Config als Snitch eingetragen ist
            for (String snitchDef : SyndicateConfig.SNITCH_ENTITIES.get()) {
                String[] parts = snitchDef.split("\\|");
                if (parts.length == 2 && parts[0].equals(entityId)) {
                    try {
                        maxPoints = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException ignored) {}
                    break;
                }
            }

            // Wenn der Mob nicht in der Config steht, machen wir gar nichts weiter
            if (maxPoints == -1) return;
            
            // Sucht ALLE Spieler im maximalen Sichtradius (16 Blöcke) ab
            AABB searchBox = informant.getBoundingBox().inflate(16.0);
            java.util.List<Player> players = informant.level().getEntitiesOfClass(Player.class, searchBox);
            
            for (Player player : players) {
                if (!player.isCreative() && !player.isSpectator()) {
                    
                    // 1. Schaut der Informant in die Richtung des Spielers?
                    // 2. Ist keine Wand im Weg?
                    if (StealthMath.isEntityInFieldOfView(informant, player, 80.0) && StealthMath.hasLineOfSight(informant, player)) {
                        
                        double visibility = StealthMath.getVisibilityScore(player, informant);
                        double detectionRange = 16.0 * visibility;
                        
                        // Wenn der Spieler nah genug ist -> PETZEN!
                        if (informant.distanceTo(player) <= detectionRange) {
                            ServerLevel level = (ServerLevel) informant.level();
                            
                            // UUID als Seed: Dieser spezifische NPC würfelt immer denselben Basis-Wert!
                            java.util.Random npcRandom = new java.util.Random(informant.getUUID().getMostSignificantBits());
                            int basePoints = npcRandom.nextInt(maxPoints) + 1;
                            
                            double finalPoints = basePoints;

                            // Beziehungs-Status beim Villager abfragen (bleibt erhalten!)
                            if (informant instanceof Villager villager) {
                                // Vanilla Methode: Wert > 0 ist gut (Handel), Wert < 0 ist schlecht (Schlagen)
                                int reputation = villager.getPlayerReputation(player);
                                
                                // Ab einer Reputation von 25 ist der Villager dir absolut treu und schweigt
                                if (reputation >= 25) {
                                    continue; 
                                }
                                
                                if (reputation > 0) {
                                    // Zieht prozentual Punkte ab, je besser die Reputation ist
                                    double reduction = reputation / 25.0; 
                                    finalPoints -= (finalPoints * reduction);
                                } else if (reputation < 0) {
                                    // Bei negativer Reputation (Dorfbewohner gequält) verdoppeln sich die Punkte im schlimmsten Fall
                                    double penalty = Math.min(1.0, Math.abs(reputation) / 25.0);
                                    finalPoints += (finalPoints * penalty);
                                }
                            }
                            
                            int pointsToApply = Math.max(1, (int) Math.round(finalPoints));
                            
                            SyndicateManager.get(level).addSighting(informant, player, pointsToApply);
                        }
                    }
                }
            }
        }
    }
}