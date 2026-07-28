package net.stealth.manhunt.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.stealth.client.StealthHud;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "manhunt", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEchoRenderer {

    // Speicher für aktive Echos (ID -> Echo-Instanz mit Zeitstempeln)
    private static final Map<Integer, EchoInstance> ACTIVE_ECHOS = new ConcurrentHashMap<>();

    /**
     * Prüft, ob das Stealth-HUD für den Spieler aktuell aktiv ist.
     * Das Echo-Glow-System ist untrennbar an die Sichtbarkeit des HUDs gekoppelt!
     */
    public static boolean isStealthHudActive() {
        return StealthHud.isHudVisible();
    }

    public static void addEcho(int entityId, int durationTicks) {
        if (!isStealthHudActive()) return;
        ACTIVE_ECHOS.put(entityId, new EchoInstance(durationTicks));
    }

    public static boolean isEcho(int entityId) {
        if (!isStealthHudActive()) return false;

        EchoInstance echo = ACTIVE_ECHOS.get(entityId);
        return echo != null && System.currentTimeMillis() < echo.expiresAt;
    }

    /**
     * Entscheidet dynamisch in jedem Frame, ob die Silhouette gezeichnet werden soll.
     * Da wir die Entity-Flags nicht mehr modifizieren, ist das hier absolut stabil und instantan!
     */
    public static boolean shouldGlow(LivingEntity entity) {
        if (!isStealthHudActive()) return false;
        if (!isEcho(entity.getId())) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        // Signal leuchtet nur hinter Wänden und Hindernissen auf
        return true;
    }

    /**
     * Berechnet die verblassende Echo-Farbe (RGB) basierend auf dem Alter des Echos.
     */
    public static int getEchoColor(int entityId) {
        EchoInstance echo = ACTIVE_ECHOS.get(entityId);
        if (echo != null) {
            long now = System.currentTimeMillis();

            // Berechne den Fortschritt (1.0 = Brandneu, 0.0 = Fast abgelaufen)
            float pct = (float) (echo.expiresAt - now) / echo.totalDuration;
            pct = Math.max(0.0f, Math.min(1.0f, pct));

            // Interpolation: Von Weiß/Hellblau (245, 245, 255) zu tiefem Akustik-Dunkelblau (10, 45, 170)
            int r = (int) (10 + (235 * pct));
            int g = (int) (45 + (200 * pct));
            int b = (int) (170 + (85 * pct));

            return (r << 16) | (g << 8) | b;
        }
        return 0xFFFFFF;
    }

    /**
     * Spawnt die akustische Welle (XRAY-Partikel) an der angegebenen Position.
     */
    public static void spawnAcousticWave(double x, double y, double z, float volume) {
        if (!isStealthHudActive()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            // Fügt unseren reparierten XRAY-Partikel in die Welt ein!
            mc.level.addParticle(net.stealth.manhunt.init.ManhuntParticles.ECHO_WAVE.get(), x, y, z, 0, 0, 0);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // Bereinigt abgelaufene Echos flüsterleise im Hintergrund aus dem Speicher
        long now = System.currentTimeMillis();
        ACTIVE_ECHOS.entrySet().removeIf(entry -> now > entry.getValue().expiresAt);
    }

    /**
     * THANOS SNAP PARTIKEL (Leuchtende Sculk-Seelen beim Verschwinden)
     */
    @SubscribeEvent
    public static void onEntityVanish(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof LivingEntity living) || event.getEntity() == Minecraft.getInstance().player) return;
        if (living.getHealth() <= 0) return; 

        if (!isEcho(living.getId())) return;

        AABB bounds = living.getBoundingBox();
        RandomSource rand = living.getRandom();

        double width = Math.max(bounds.maxX - bounds.minX, 0.5);
        double height = Math.max(bounds.maxY - bounds.minY, 0.5);
        double depth = Math.max(bounds.maxZ - bounds.minZ, 0.5);
        double volume = width * height * depth;

        int particleCount = Math.min((int) (volume * 120), 200);

        for (int i = 0; i < particleCount; i++) {
            double x = bounds.minX + rand.nextDouble() * width;
            double y = bounds.minY + rand.nextDouble() * height;
            double z = bounds.minZ + rand.nextDouble() * depth;

            double dx = (rand.nextDouble() - 0.5) * 0.04;
            double dy = rand.nextDouble() * 0.05 + 0.02;
            double dz = (rand.nextDouble() - 0.5) * 0.04;

            if (i % 3 == 0) {
                event.getLevel().addParticle(ParticleTypes.SQUID_INK, x, y, z, dx, dy, dz);
            } else {
                event.getLevel().addParticle(ParticleTypes.SMOKE, x, y, z, dx, dy, dz);
            }
        }

        ACTIVE_ECHOS.remove(living.getId());
    }

    private static class EchoInstance {
        final long startTime;
        final long expiresAt;
        final long totalDuration;

        EchoInstance(int durationTicks) {
            this.startTime = System.currentTimeMillis();
            this.totalDuration = durationTicks * 50L;
            this.expiresAt = this.startTime + this.totalDuration;
        }
    }
}