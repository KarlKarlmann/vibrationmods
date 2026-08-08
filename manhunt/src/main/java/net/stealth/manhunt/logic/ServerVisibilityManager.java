package net.stealth.manhunt.logic;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.stealth.manhunt.config.ManhuntConfig;
import net.stealth.util.StealthMath;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerVisibilityManager {

    private static final Map<UUID, Map<Integer, Long>> ECHO_TIMERS = new ConcurrentHashMap<>();

    public static void addEcho(ServerPlayer observer, Entity target, int durationTicks) {
        ECHO_TIMERS.computeIfAbsent(observer.getUUID(), k -> new ConcurrentHashMap<>())
                   .put(target.getId(), observer.level().getGameTime() + durationTicks);
    }

    /**
     * Entfernt die Echo-Timer eines Spielers beim Logout.
     */
    public static void clearPlayer(UUID playerUuid) {
        ECHO_TIMERS.remove(playerUuid);
    }

    public static boolean isVisible(ServerPlayer observer, Entity target) {
        if (!(target instanceof LivingEntity livingTarget)) {
            return true;
        }
        
        if (livingTarget.isCurrentlyGlowing()) {
            return true;
        }

        if (ManhuntConfig.ALLIES_VISIBLE.get() && observer.isAlliedTo(livingTarget)) {
            return true;
        }

        if (livingTarget.hurtTime > 0) {
            return true;
        }
        if (livingTarget.getLastHurtMobTimestamp() > livingTarget.tickCount - 60) {
            return true;
        }
        if (observer.getLastHurtMob() == livingTarget && observer.getLastHurtMobTimestamp() > observer.tickCount - 60) {
            return true;
        }

        Map<Integer, Long> playerEchos = ECHO_TIMERS.get(observer.getUUID());
        if (playerEchos != null) {
            Long expirationTick = playerEchos.get(target.getId());
            if (expirationTick != null) {
                if (observer.level().getGameTime() < expirationTick) {
                    return true; 
                } else {
                    playerEchos.remove(target.getId()); 
                }
            }
        }

        // ==========================================
        // 6. MANHUNT-SPEZIFISCHES RAYCASTING (Culling-Schutz)
        // ==========================================
        if (!hasManhuntLineOfSight(observer, livingTarget)) {
            return false;
        }

        double visibilityScore = StealthMath.getVisibilityScore(livingTarget, observer);
        double PARANOIA_THRESHOLD = ManhuntConfig.PARANOIA_THRESHOLD.get(); 

        if (visibilityScore <= PARANOIA_THRESHOLD) {
            return false; 
        }

        return true;
    }

    /**
     * Präzises Silhouetten-Raycasting exklusiv für das Manhunt-Culling.
     * Prüft Kopf, Mitte, Füße sowie die linke und rechte Kante der Hitbox.
     */
    public static boolean hasManhuntLineOfSight(ServerPlayer observer, LivingEntity target) {
        // 1. Standard Vanilla-Sichtprüfung
        if (StealthMath.hasLineOfSight(observer, target)) {
            return true;
        }

        Vec3 start = observer.getEyePosition();

        // 2. GENERISCHER KOPF-TEST (Essenziell für alle sitzenden Entities):
        // Prüft direkt die Augenhöhe. Wenn der Kopf frei steht, ist das Entity sichtbar!
        Vec3 eyePos = target.getEyePosition();
        if (observer.level().clip(new ClipContext(start, eyePos, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, observer)).getType() == HitResult.Type.MISS) {
            return true;
        }

        // 3. Silhouetten-Prüfung der Bounding-Box-Mitte (Kanten)
        Vec3 endCenter = target.getBoundingBox().getCenter();
        double width = target.getBbWidth();

        Vec3 viewDir = endCenter.subtract(start);
        Vec3 flatDir = new Vec3(viewDir.x, 0, viewDir.z);
        Vec3 rightVec = (flatDir.lengthSqr() < 1E-4) ? new Vec3(1, 0, 0) : new Vec3(-flatDir.z, 0, flatDir.x).normalize();
        Vec3 sideOffset = rightVec.scale(width * 0.45);

        Vec3[] sidePoints = new Vec3[]{
            endCenter.add(sideOffset),
            endCenter.subtract(sideOffset)
        };

        for (Vec3 end : sidePoints) {
            if (observer.level().clip(new ClipContext(start, end, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, observer)).getType() == HitResult.Type.MISS) {
                return true;
            }
        }

        // 4. GENERISCHE VEHIKEL-REKURSION (Für JEGLICHE Reittiere / Sitz-Objekte):
        boolean targetIsInvisible = target.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY);
        if (!targetIsInvisible && target.isPassenger()) {
            Entity vehicle = target.getVehicle();
            if (vehicle instanceof LivingEntity mount) {
                return hasManhuntLineOfSight(observer, mount);
            }
            // Für nicht-lebende Vehikel (Boote, Loren, Sitze): Ist das Vehikel selbst im Sichtfeld?
            if (vehicle != null) {
                Vec3 vehicleCenter = vehicle.getBoundingBox().getCenter();
                return observer.level().clip(new ClipContext(start, vehicleCenter, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, observer)).getType() == HitResult.Type.MISS;
            }
        }

        return false;
    }
}