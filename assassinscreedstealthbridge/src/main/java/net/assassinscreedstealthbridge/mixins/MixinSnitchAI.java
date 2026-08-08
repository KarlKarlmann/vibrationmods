package net.assassinscreedstealthbridge.mixins;

import net.assassinscreedstealthbridge.syndicate.SyndicateManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.stealth.util.StealthMath;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Verteilt die Rechenlast auf die Entitäten selbst. 
 * Agiert als "unsichtbare" Snitch-AI für Villager und Piglins.
 */
@Mixin({Villager.class, AbstractPiglin.class})
public abstract class MixinSnitchAI {

    // customServerAiStep ist die Methode, die Vanilla für Mob-Logik-Ticks nutzt.
    // Bei Villagern und Piglins wird hier (oder in tickBrain) die Logik abgearbeitet.
    // Durch das Binden an diese beiden Klassen greifen wir genau richtig ein!
    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void stealth$snitchOnPlayer(CallbackInfo ci) {
        Mob informant = (Mob)(Object)this;

        // Läuft nur alle 40 Ticks (2 Sekunden) für eine perfekte Performance
        if (informant.tickCount % 40 == 0 && !informant.level().isClientSide()) {
            
            // Sucht den nächsten Spieler im maximalen Sichtradius
            Player player = informant.level().getNearestPlayer(informant, 16.0);
            
            if (player != null && !player.isCreative() && !player.isSpectator()) {
                
                // 1. Schaut der Informant in die Richtung des Spielers?
                // 2. Ist keine Wand im Weg?
                if (StealthMath.isEntityInFieldOfView(informant, player, 80.0) && StealthMath.hasLineOfSight(informant, player)) {
                    
                    // 3. Wie gut sichtbar ist der Spieler durch die StealthMod?
                    double visibility = StealthMath.getVisibilityScore(player, informant);
                    double detectionRange = 16.0 * visibility;
                    
                    // Wenn der Spieler nah genug und sichtbar genug ist -> PETZEN!
                    if (informant.distanceTo(player) <= detectionRange) {
                        ServerLevel level = (ServerLevel) informant.level();
                        
                        // Piglins sind wertvoller (2) als einfache Villager (1)
                        int points = (informant instanceof Villager) ? 1 : 2;
                        SyndicateManager.get(level).addSighting(informant, player, points);
                    }
                }
            }
        }
    }
}