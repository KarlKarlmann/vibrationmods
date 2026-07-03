package net.targetoutlines.client;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.targetoutlines.TargetOutlinesMod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = TargetOutlinesMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientOutlineRenderer {

    // Speicher für alle Mobs, die diesen lokalen Spieler aktuell anvisieren
    private static final Set<Integer> TARGETS = new HashSet<>();

    public static void updateTargets(List<Integer> newTargets) {
        TARGETS.clear();
        TARGETS.addAll(newTargets);
    }

    /**
     * Wird von unserem Mixin aufgerufen, um zu prüfen, ob der Mob rot umrandet werden soll.
     */
    public static boolean isTarget(int entityId) {
        return TARGETS.contains(entityId);
    }

    /**
     * Da das Mixin Minecraft dazu bringt, das Entity in den Outline-Buffer zu schicken,
     * fangen wir diesen hier ab und färben ihn leuchtend rot!
     */
    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (TARGETS.contains(entity.getId())) {
            MultiBufferSource bufferSource = event.getMultiBufferSource();
            if (bufferSource instanceof OutlineBufferSource outlineBuffers) {
                // Setze die Outline-Farbe auf reines, leuchtendes Rot.
                // Epic Fight liest diese Werte direkt aus und zeichnet die Knochen-Animation fehlerfrei rot!
                outlineBuffers.setColor(255, 0, 0, 255);
            }
        }
    }
}