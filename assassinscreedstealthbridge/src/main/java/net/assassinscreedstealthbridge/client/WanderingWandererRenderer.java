package net.assassinscreedstealthbridge.client;

import net.assassinscreedstealthbridge.AssassinsCreedStealthBridge;
import net.assassinscreedstealthbridge.entity.WanderingWandererEntity;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class WanderingWandererRenderer extends MobRenderer<WanderingWandererEntity, VillagerModel<WanderingWandererEntity>> {
    
    // Der Pfad zu deiner Custom-Textur im resourcepacks / assets Ordner
    private static final ResourceLocation TEXTURE = 
            new ResourceLocation(AssassinsCreedStealthBridge.MODID, "textures/entity/wandering_wanderer.png");

    public WanderingWandererRenderer(EntityRendererProvider.Context context) {
        // Wir nutzen das exakte Vanilla-3D-Modell des Wandering Traders.
        // Der Wert 0.5f am Ende ist die Größe des Schattens unter dem Mob.
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.WANDERING_TRADER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(WanderingWandererEntity entity) {
        return TEXTURE;
    }
}