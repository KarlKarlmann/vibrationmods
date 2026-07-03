package net.targetoutlines;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import net.targetoutlines.network.SyncTargetsPacket;
import net.targetoutlines.network.TargetNetwork;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SERVERSEITE:
 * Überfängt das LivingChangeTargetEvent, um sofort und hocheffizient zu reagieren,
 * wenn ein Mob sein Angriffsziel ändert. Dies ersetzt den rechenintensiven Bereichsscan.
 */
public class TargetEventHandler {

    // Speichert die IDs der Mobs, die einen Spieler (UUID) anvisieren
    private static final Map<UUID, Set<Integer>> PLAYER_TARGET_MAP = new ConcurrentHashMap<>();

    /**
     * WICHTIGE INTEGRATION MIT STEALTH:
     * Wir setzen die Priorität auf LOW. Da die Stealth-Mod auf NORMAL läuft, darf sie
     * zuerst entscheiden, ob sie den Zielwechsel abbricht (z.B. weil der Spieler im Dunkeln steht).
     * Da 'receiveCanceled' standardmäßig false ist, wird diese Methode bei einem Abbruch durch
     * die Stealth-Mod gar nicht erst ausgeführt. Wir erhalten also NUR die echten, bereinigten Ziele!
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onSetTarget(LivingChangeTargetEvent event) {
        // Nur auf der Serverseite verarbeiten
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;

        LivingEntity oldTarget = mob.getTarget();
        LivingEntity newTarget = event.getNewTarget();

        // Falls sich das Target nicht geändert hat, überspringen
        if (oldTarget == newTarget) return;

        // 1. Vom alten Spieler-Target entfernen
        if (oldTarget instanceof ServerPlayer oldPlayer) {
            removeTargetForPlayer(oldPlayer, mob.getId());
        }

        // 2. Zum neuen Spieler-Target hinzufügen
        if (newTarget instanceof ServerPlayer newPlayer) {
            addTargetForPlayer(newPlayer, mob.getId());
        }
    }

    @SubscribeEvent
    public void onMobDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;

        // Wenn ein Mob stirbt, entfernen wir ihn aus allen aktiven Spieler-Listen
        for (Map.Entry<UUID, Set<Integer>> entry : PLAYER_TARGET_MAP.entrySet()) {
            if (entry.getValue().contains(mob.getId())) {
                ServerPlayer player = (ServerPlayer) mob.level().getPlayerByUUID(entry.getKey());
                if (player != null) {
                    removeTargetForPlayer(player, mob.getId());
                } else {
                    entry.getValue().remove(mob.getId());
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        // Speicher freigeben, wenn der Spieler den Server verlässt
        PLAYER_TARGET_MAP.remove(event.getEntity().getUUID());
    }

    private void addTargetForPlayer(ServerPlayer player, int mobId) {
        Set<Integer> targets = PLAYER_TARGET_MAP.computeIfAbsent(player.getUUID(), k -> Collections.synchronizedSet(new HashSet<>()));
        if (targets.add(mobId)) {
            syncTargets(player);
        }
    }

    private void removeTargetForPlayer(ServerPlayer player, int mobId) {
        Set<Integer> targets = PLAYER_TARGET_MAP.get(player.getUUID());
        if (targets != null && targets.remove(mobId)) {
            syncTargets(player);
        }
    }

    private void syncTargets(ServerPlayer player) {
        Set<Integer> targets = PLAYER_TARGET_MAP.get(player.getUUID());
        List<Integer> targetList = new ArrayList<>();
        
        if (targets != null) {
            synchronized (targets) {
                Iterator<Integer> iterator = targets.iterator();
                while (iterator.hasNext()) {
                    int id = iterator.next();
                    // Validierung: Existiert die Entität in der Welt des Spielers und lebt sie noch?
                    if (player.level().getEntity(id) instanceof Mob mob) {
                        if (mob.isAlive()) {
                            targetList.add(id);
                            continue;
                        }
                    }
                    // Ungültige oder tote Mobs fliegen direkt raus
                    iterator.remove();
                }
            }
        }
        
        // Sofortiges privates Netzwerkpaket an den betroffenen Spieler senden
        TargetNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncTargetsPacket(targetList));
    }
}