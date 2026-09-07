package net.assassinscreedstealthbridge.registry;

import net.assassinscreedstealthbridge.AssassinsCreedStealthBridge;
import net.assassinscreedstealthbridge.entity.WanderingWandererEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    
    // Der DeferredRegister sammelt alle unsere neuen Entitäten, bevor sie an Forge übergeben werden
    public static final DeferredRegister<EntityType<?>> ENTITIES = 
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AssassinsCreedStealthBridge.MODID);

    // Wir definieren den Wanderer als Monster (MobCategory.MONSTER), damit er nachts in der Dunkelheit spawnt!
    public static final RegistryObject<EntityType<WanderingWandererEntity>> WANDERING_WANDERER =
            ENTITIES.register("wandering_wanderer",
                    () -> EntityType.Builder.of(WanderingWandererEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F) // Selbe Hitbox wie Villager/Trader
                            .clientTrackingRange(8)
                            .build("wandering_wanderer"));
}