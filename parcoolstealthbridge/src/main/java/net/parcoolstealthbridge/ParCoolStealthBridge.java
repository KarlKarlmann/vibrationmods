package net.parcoolstealthbridge;

import com.alrex.parcool.api.unstable.action.ParCoolActionEvent;
import com.alrex.parcool.common.action.Action;
import com.alrex.parcool.common.action.impl.*;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.stealth.registry.StealthAttributes;
import net.parcoolstealthbridge.client.ParCoolStealthBridgeClient;
import net.parcoolstealthbridge.config.BridgeConfig;
import org.slf4j.Logger;

import java.util.UUID;

@Mod(ParCoolStealthBridge.MOD_ID)
public class ParCoolStealthBridge {
    public static final String MOD_ID = "parcoolstealthbridge";
    private static final Logger LOGGER = LogUtils.getLogger();

    // Eindeutige UUIDs für die transienten Attribut-Modifikatoren
    private static final UUID SLIDE_CAMOUFLAGE_UUID = UUID.fromString("a1b2c3d4-e5f6-4a5b-b6c7-d8e9f0a1b2c3");
    private static final UUID FASTRUN_CAMOUFLAGE_UUID = UUID.fromString("b2c3d4e5-f6a7-5b6c-c7d8-e9f0a1b2c3d4");
    private static final UUID FASTRUN_MUFFLING_UUID = UUID.fromString("c3d4e5f6-a7b8-6c7d-d8e9-f0a1b2c3d4e5");
    private static final UUID CLING_CAMOUFLAGE_UUID = UUID.fromString("d4e5f6a7-b8c9-7d8e-e9f0-a1b2c3d4e5f6");
    private static final UUID HIDEINBLOCK_CAMOUFLAGE_UUID = UUID.fromString("7f8e5f2e-48a5-48f8-b3d9-60a6316fa717");

    public ParCoolStealthBridge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // Registriert nur noch die Common Config
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, BridgeConfig.COMMON_SPEC);

        MinecraftForge.EVENT_BUS.register(this);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ParCoolStealthBridgeClient.init(modEventBus);
        }

        LOGGER.info("[ParCoolStealthBridge] Compatibility bridge for movement states initialized!");
    }

    @SubscribeEvent
    public void onParCoolActionStart(ParCoolActionEvent.Start.Post event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide()) return;

        Action action = event.getAction();

        if (action instanceof Slide) {
            applyModifier(player, StealthAttributes.CAMOUFLAGE.get(), SLIDE_CAMOUFLAGE_UUID, "ParCool Slide Camouflage", BridgeConfig.COMMON.slideCamouflage.get());
        }
        else if (action instanceof FastRun || action instanceof FastSwim) {
            applyModifier(player, StealthAttributes.CAMOUFLAGE.get(), FASTRUN_CAMOUFLAGE_UUID, "ParCool FastRun Visibility Penalty", BridgeConfig.COMMON.fastRunCamouflage.get());
            applyModifier(player, StealthAttributes.MUFFLING.get(), FASTRUN_MUFFLING_UUID, "ParCool FastRun Muffling Penalty", BridgeConfig.COMMON.fastRunMuffling.get());
        }
        else if (action instanceof ClingToCliff || action instanceof HangDown) {
            applyModifier(player, StealthAttributes.CAMOUFLAGE.get(), CLING_CAMOUFLAGE_UUID, "ParCool Cling Camouflage", BridgeConfig.COMMON.clingCamouflage.get());
        }
        else if (action instanceof HideInBlock) {
            applyModifier(player, StealthAttributes.CAMOUFLAGE.get(), HIDEINBLOCK_CAMOUFLAGE_UUID, "ParCool HideInBlock Camouflage", BridgeConfig.COMMON.hideInBlockCamouflage.get());
        }
    }

    @SubscribeEvent
    public void onParCoolActionFinish(ParCoolActionEvent.Finish.Post event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide()) return;

        Action action = event.getAction();

        if (action instanceof Slide) {
            removeModifier(player, StealthAttributes.CAMOUFLAGE.get(), SLIDE_CAMOUFLAGE_UUID);
        } else if (action instanceof FastRun || action instanceof FastSwim) {
            removeModifier(player, StealthAttributes.CAMOUFLAGE.get(), FASTRUN_CAMOUFLAGE_UUID);
            removeModifier(player, StealthAttributes.MUFFLING.get(), FASTRUN_MUFFLING_UUID);
        } else if (action instanceof ClingToCliff || action instanceof HangDown) {
            removeModifier(player, StealthAttributes.CAMOUFLAGE.get(), CLING_CAMOUFLAGE_UUID);
        } else if (action instanceof HideInBlock) {
            removeModifier(player, StealthAttributes.CAMOUFLAGE.get(), HIDEINBLOCK_CAMOUFLAGE_UUID);
        }
    }

    private void applyModifier(Player player, Attribute attribute, UUID uuid, String name, double value) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null && instance.getModifier(uuid) == null) {
            instance.addTransientModifier(new AttributeModifier(uuid, name, value, AttributeModifier.Operation.ADDITION));
        }
    }

    private void removeModifier(Player player, Attribute attribute, UUID uuid) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null && instance.getModifier(uuid) != null) {
            instance.removeModifier(uuid);
        }
    }
}