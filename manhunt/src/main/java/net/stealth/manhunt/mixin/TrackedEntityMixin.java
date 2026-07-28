package net.stealth.manhunt.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.stealth.manhunt.logic.ServerVisibilityManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Wir targeten die innere Klasse TrackedEntity über ihren Bytecode-Namen ($).
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class TrackedEntityMixin {

    /**
     * In ChunkMap$TrackedEntity.updatePlayer() steht:
     * bl = d <= d3 && this.entity.broadcastToPlayer(serverPlayer);
     * 
     * Wir leiten den Methodenaufruf "broadcastToPlayer" um. 
     * Das ist extrem performant, da wir nur aufgerufen werden, wenn der Spieler 
     * bereits in der generellen Render-Distanz ist (d <= d3).
     */
    @Redirect(
            method = "updatePlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;broadcastToPlayer(Lnet/minecraft/server/level/ServerPlayer;)Z")
    )
    private boolean manhunt$cullEntityInDarkness(Entity target, ServerPlayer observer) {
        
        // 1. Zuerst fragen wir Vanilla: "Soll der Spieler das überhaupt sehen?"
        // (Berücksichtigt Spectator-Modus, Vanilla-Unsichtbarkeit etc.)
        boolean vanillaResult = target.broadcastToPlayer(observer);
        
        // Wenn Vanilla schon sagt "Nein" (z.B. Spieler ist in einer anderen Dimension),
        // brechen wir sofort ab. Wir müssen keine Stealth-Mathe durchführen.
        if (!vanillaResult) {
            return false; 
        }

        // 2. Vanilla sagt JA. Jetzt kommt der Gamma-Killer zum Einsatz.
        // Wir fragen unseren ServerVisibilityManager.
        // Gibt dieser 'false' zurück, glaubt der Server, broadcastToPlayer wäre fehlgeschlagen
        // und kappt gnadenlos das Entity-Tracking für diesen Client.
        return ServerVisibilityManager.isVisible(observer, target);
    }
}