package net.stealth.manhunt.logic;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.stealth.manhunt.config.ManhuntConfig;
import net.stealth.util.StealthMath;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerVisibilityManager {

    // Speichert: UUID des Spielers -> (ID der Entity -> Tick, an dem sie wieder verschwindet)
    private static final Map<UUID, Map<Integer, Long>> ECHO_TIMERS = new ConcurrentHashMap<>();

    /**
     * Wird aufgerufen, wenn eine Entity Lärm macht (Vibration).
     * Zwingt den Server, diese Entity für den angegebenen Spieler kurzzeitig zu rendern.
     */
    public static void addEcho(ServerPlayer observer, Entity target, int durationTicks) {
        ECHO_TIMERS.computeIfAbsent(observer.getUUID(), k -> new ConcurrentHashMap<>())
                   .put(target.getId(), observer.level().getGameTime() + durationTicks);
    }

    /**
     * Der absolute Kern der Manhunt-Mod. 
     * Hier entscheidet der SERVER, ob der Client überhaupt erfährt, dass diese Entität existiert.
     */
    public static boolean isVisible(ServerPlayer observer, Entity target) {
        // 1. Nur lebende Dinge können sich im Schatten verstecken (Items, Blöcke, etc. bleiben normal)
        if (!(target instanceof LivingEntity livingTarget)) {
            return true;
        }
        
        // 2. VANILLA GLOWING (z.B. Spectral Arrow) bricht das Culling ab.
        if (livingTarget.isCurrentlyGlowing()) {
            return true;
        }

        // 3. TEAM-CHECK (Config-gesteuert)
        if (ManhuntConfig.ALLIES_VISIBLE.get() && observer.isAlliedTo(livingTarget)) {
            return true;
        }

        // 4. KAMPF-OVERRIDE (Das Anti-Desync Sicherheitsnetz)
        // Wenn man sich haut, verschwindet man nicht im Schatten.
        if (livingTarget.hurtTime > 0) {
            return true;
        }
        if (livingTarget.getLastHurtMobTimestamp() > livingTarget.tickCount - 60) {
            return true;
        }
        if (observer.getLastHurtMob() == livingTarget && observer.getLastHurtMobTimestamp() > observer.tickCount - 60) {
            return true;
        }

        // 5. IST DIE ENTITY EIN AKTIVES ECHO (LÄRM)?
        Map<Integer, Long> playerEchos = ECHO_TIMERS.get(observer.getUUID());
        if (playerEchos != null) {
            Long expirationTick = playerEchos.get(target.getId());
            if (expirationTick != null) {
                if (observer.level().getGameTime() < expirationTick) {
                    return true; // LASS SIE DURCH! Der Server schickt sie an den Client für den roten Schatten!
                } else {
                    playerEchos.remove(target.getId()); // Cooldown abgelaufen, Zeit für den Thanos-Snap!
                }
            }
        }

        // ==========================================
        // DIE MANHUNT REGELN (Powered by Karl's StealthMath)
        // ==========================================
        
        // 6. THE WALLHACK KILLER (Anti-ESP)
        // (Wichtig: Wird nach dem Echo-Check aufgerufen! So kann man Echos sogar kurz durch Wände aufblitzen sehen, 
        //  falls man echtes "Gehör" visualisieren will. Wenn du Echos nicht durch Wände sehen willst, 
        //  muss das über den Echo-Check wandern!)
        if (!StealthMath.hasLineOfSight(observer, livingTarget)) {
            return false;
        }

        // 7. Der Gamma-Killer: Core-Logik aus dem Stealth-System
        double visibilityScore = StealthMath.getVisibilityScore(livingTarget, observer);
        double PARANOIA_THRESHOLD = ManhuntConfig.PARANOIA_THRESHOLD.get(); 

        // 8. Der Culling-Schnitt
        if (visibilityScore <= PARANOIA_THRESHOLD) {
            return false; 
        }

        // Standard: Im Licht und alles ist gut sichtbar
        return true;
    }
}