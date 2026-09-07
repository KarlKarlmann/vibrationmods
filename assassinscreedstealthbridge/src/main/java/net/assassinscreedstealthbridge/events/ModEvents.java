package net.assassinscreedstealthbridge.events;

import net.assassinscreedstealthbridge.AssassinsCreedStealthBridge;
import net.assassinscreedstealthbridge.client.WanderingWandererRenderer;
import net.assassinscreedstealthbridge.entity.WanderingWandererEntity;
import net.assassinscreedstealthbridge.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;

// WICHTIG: bus = Mod.EventBusSubscriber.Bus.MOD sorgt dafür, dass diese Events beim Starten des Spiels feuern!
@Mod.EventBusSubscriber(modid = AssassinsCreedStealthBridge.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEvents {

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        // Hier weisen wir unserem Mob die Methode zu, die wir vorher in seiner Klasse geschrieben haben
        event.put(ModEntities.WANDERING_WANDERER.get(), WanderingWandererEntity.createAttributes().build());
    }
    
    @SubscribeEvent
    public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
        // Definiert: Er spawnt auf dem Boden, braucht einen festen Block unter sich und nutzt die Standard-Monster-Regel (braucht Dunkelheit!)
        event.register(ModEntities.WANDERING_WANDERER.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
    }

    // Dies darf nur auf dem Client passieren (Server brauchen keine 3D-Modelle)
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.WANDERING_WANDERER.get(), WanderingWandererRenderer::new);
    }
}