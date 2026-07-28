package net.stealth.manhunt.events;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.VanillaGameEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.stealth.manhunt.StealthManhunt;
import net.stealth.manhunt.logic.ServerVisibilityManager;
import net.stealth.manhunt.network.ClientboundMarkEchoPacket;
import net.stealth.manhunt.network.ClientboundAcousticWavePacket;
import net.stealth.manhunt.network.ManhuntNetwork;

@Mod.EventBusSubscriber(modid = "manhunt", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerVibrationHandler {

    @SubscribeEvent
    public static void onVibration(VanillaGameEvent event) {
        if (event.getLevel().isClientSide()) return;
        
        // KORREKTUR: In neueren Forge-Mappings heißt das getEventPosition() anstatt getPos()
        Vec3 eventPos = event.getEventPosition();
        if (eventPos == null) return;

        Entity source = event.getContext().sourceEntity();
        
        // 1. Finde den verantwortlichen Verursacher (LivingEntity)
        LivingEntity responsible = null;
        if (source instanceof LivingEntity living) {
            responsible = living;
        } else if (source instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity owner) {
            // Falls es ein Pfeil/Zauberspruch ist, ist der Schütze der Verursacher!
            responsible = owner;
        }

        // 2. Lese die konfigurierte Priorität (Lautstärke) direkt aus der Stealth-Config aus
        float priority = getPriorityForEvent(event.getVanillaEvent());

        // 3. Multipliziere Rüstungsgeräusche (Armor Muffling) und Schleichen in die Lautstärke hinein!
        if (responsible != null) {
            double noiseMultiplier = net.stealth.util.StealthMath.getArmorNoiseMultiplier(responsible);
            
            boolean isMovement = event.getVanillaEvent() == GameEvent.STEP 
                    || event.getVanillaEvent() == GameEvent.SWIM 
                    || event.getVanillaEvent() == GameEvent.ELYTRA_GLIDE 
                    || event.getVanillaEvent() == GameEvent.HIT_GROUND;
            
            if (responsible.isCrouching()) {
                if (isMovement) {
                    noiseMultiplier *= 0.1f; // Extrem leise Schritte beim Schleichen
                } else {
                    noiseMultiplier *= 0.8f; // Geringere Dämpfung für andere Aktionen
                }
            }
            priority *= noiseMultiplier;
        }

        // 4. Berechne die dynamische Hörreichweite basierend auf der Config und dem Dämpfungsgrad
        double baseRange = net.stealth.config.StealthConfig.COMMON.BASE_HEARING_RANGE.get();
        double dynamicRange = baseRange * (priority / 5.0); // Normiert auf Standard-Priorität 5.0
        double dynamicRangeSq = dynamicRange * dynamicRange;

        // 5. Errechne die relative Intensität (Volume-Faktor) für die Größe der Welle auf dem Client
        float volume = priority / 5.0f;

        // Berechne die Distanz zwischen der Erschütterung und dem Verursacher
        double distanceToSourceSq = responsible != null ? responsible.position().distanceToSqr(eventPos) : -1;

        for (ServerPlayer player : event.getLevel().getServer().getPlayerList().getPlayers()) {
            // Sende das Signal nur an Spieler, die sich innerhalb des dynamischen Gehörradius befinden!
            if (player.level() == event.getLevel() && player.position().distanceToSqr(eventPos) < dynamicRangeSq) { 
                if (player == responsible) continue;

                // A. POSITIONELLE SCHALLWELLE (Spawnt JETZT IMMER am Punkt der Vibration als cooler Indikator!)
                ManhuntNetwork.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), 
                    new ClientboundAcousticWavePacket(eventPos.x, eventPos.y, eventPos.z, volume)
                );

                // B. DIRECT SUBJECT GLOW: Wenn das Event direkt an den Koordinaten der Entity stattfindet
                // Wir verzichten komplett auf statische Event-Listen. Ist die Entity nah genug am Geschehen, leuchtet sie auf!
                if (responsible != null && distanceToSourceSq <= 2.25) {
                    StealthManhunt.LOGGER.info("Direktes Echo von " + responsible.getName().getString() + " (Event: " + event.getVanillaEvent().getName() + ")");
                    
                    ServerVisibilityManager.addEcho(player, responsible, 20);

                    // Sende das Glow-Paket
                    ManhuntNetwork.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), 
                        new ClientboundMarkEchoPacket(responsible.getId(), 40)
                    );
                }
            }
        }
    }

    private static float getPriorityForEvent(GameEvent event) {
        ResourceLocation key = BuiltInRegistries.GAME_EVENT.getKey(event);
        if (key == null) return 5.0f;
        
        try {
            java.util.List<? extends String> priorityList = net.stealth.config.StealthConfig.COMMON.VIBRATION_PRIORITIES.get();
            for (String s : priorityList) {
                int separatorIdx = s.indexOf(';');
                if (separatorIdx != -1) {
                    String eventName = s.substring(0, separatorIdx).trim();
                    if (eventName.equals(key.toString())) {
                        return Float.parseFloat(s.substring(separatorIdx + 1).trim());
                    }
                }
            }
        } catch (Exception e) {
            StealthManhunt.LOGGER.error("[Manhunt] Fehler beim Auslesen des Config-Prioritäts-Werts: ", e);
        }
        return 5.0f; // Standard-Fallback
    }
}