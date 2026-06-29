package net.parcoolstealthbridge;

import com.alrex.parcool.api.unstable.action.ParCoolActionEvent;
import com.alrex.parcool.common.action.Action;
import com.alrex.parcool.common.action.impl.*;
import com.alrex.parcool.common.capability.Parkourability;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.stealth.client.StealthHud;
import net.stealth.client.StealthHudConfig;
import net.stealth.registry.StealthAttributes;
import net.stealth.registry.StealthSounds;
import net.stealth.util.StealthTextureHelper;
import net.stealth.util.ThreatLevel;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.UUID;

/**
 * ARCHITEKTUR: STEALTH & PARCOOL STATE BRIDGE (Mod 2)
 * Synchronisiert die dynamischen Parkour-Zustände von ParCool direkt mit
 * den Stealth-Attributen (Camouflage & Muffling) der StealthMod.
 * * * 1. Übersetzt Bewegungs-States (Crawl, Slide, FastRun, Cling) in Attribut-Modifikatoren.
 * * 2. Integriert HideInBlock mit einer hohen, aber nicht absoluten Tarnung von 90%.
 * * 3. Zeichnet die Pappkiste im Versteck, welche bei Entdeckung (HUNTED) durch das Alarm-Auge ersetzt wird.
 * * 4. Spielt das Entdeckungs-Geräusch im Versteck ab, falls der Spieler entdeckt wird.
 */
@Mod(ParCoolStealthBridge.MOD_ID)
public class ParCoolStealthBridge {
    public static final String MOD_ID = "parcoolstealthbridge";
    private static final Logger LOGGER = LogUtils.getLogger();

    // Eindeutige UUIDs für die transienten Attribut-Modifikatoren der verschiedenen Bewegungs-States
    private static final UUID CRAWL_CAMOUFLAGE_UUID = UUID.fromString("6a4f7e2a-19b3-4f9e-a1c8-204b7e9a8d11");
    private static final UUID CRAWL_MUFFLING_UUID = UUID.fromString("8e7c6d5b-21a4-4f9e-b2d9-305c8f1a9e22");
    
    private static final UUID SLIDE_CAMOUFLAGE_UUID = UUID.fromString("a1b2c3d4-e5f6-4a5b-b6c7-d8e9f0a1b2c3");
    
    private static final UUID FASTRUN_CAMOUFLAGE_UUID = UUID.fromString("b2c3d4e5-f6a7-5b6c-c7d8-e9f0a1b2c3d4");
    private static final UUID FASTRUN_MUFFLING_UUID = UUID.fromString("c3d4e5f6-a7b8-6c7d-d8e9-f0a1b2c3d4e5");
    
    private static final UUID CLING_CAMOUFLAGE_UUID = UUID.fromString("d4e5f6a7-b8c9-7d8e-e9f0-a1b2c3d4e5f6");

    // UUID für den HideInBlock-Tarnungs-Modifikator
    private static final UUID HIDEINBLOCK_CAMOUFLAGE_UUID = UUID.fromString("7f8e5f2e-48a5-48f8-b3d9-60a6316fa717");

    // Texturen für das Dummy-Kisten-HUD und das weit geöffnete Auge bei Alarm
    private static final ResourceLocation TEX_HIDING_BOX = new ResourceLocation("stealth", "textures/gui/hiding_box.png");
    private static final ResourceLocation TEX_EYE_WIDE = new ResourceLocation("stealth", "textures/gui/eyewide.png");

    // Lokaler Cache zur Erkennung des Bedrohungs-Umschlags
    private static ThreatLevel lastBridgeThreatLevel = ThreatLevel.NONE;

    public ParCoolStealthBridge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // Registrierung auf dem Forge Event Bus
        MinecraftForge.EVENT_BUS.register(this);

        // Client-seitige GUI-Registrierung
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::registerClientOverlays);
        }

        LOGGER.info("[ParCoolStealthBridge] Compatibility bridge for movement states initialized!");
    }

    /**
     * HOOK 1: BEWEGUNGS-STATE GESTARTET
     * Wendet je nach gestarteter ParCool-Aktion passende Stealth-Attribute an.
     */
    @SubscribeEvent
    public void onParCoolActionStart(ParCoolActionEvent.Start.Post event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide()) return;

        Action action = event.getAction();

        // 1. Kriechen (Crawl) -> Extrem flach am Boden (mehr Camouflage) und sehr leise (mehr Muffling)
        if (action instanceof Crawl) {
            applyModifier(player, StealthAttributes.CAMOUFLAGE.get(), CRAWL_CAMOUFLAGE_UUID, "ParCool Crawl Camouflage", 0.4);
            applyModifier(player, StealthAttributes.MUFFLING.get(), CRAWL_MUFFLING_UUID, "ParCool Crawl Muffling", 0.5);
        }
        // 2. Rutschen (Slide) -> Flach am Boden (mehr Camouflage), aber erzeugt Reibung (kein Muffling-Bonus)
        else if (action instanceof Slide) {
            applyModifier(player, StealthAttributes.CAMOUFLAGE.get(), SLIDE_CAMOUFLAGE_UUID, "ParCool Slide Camouflage", 0.3);
        }
        // 3. FastRun / FastSwim -> Auffällige, wilde Moves machen den Spieler deutlich sichtbarer und lauter (Negative Modifikatoren)
        else if (action instanceof FastRun || action instanceof FastSwim) {
            applyModifier(player, StealthAttributes.CAMOUFLAGE.get(), FASTRUN_CAMOUFLAGE_UUID, "ParCool FastRun Visibility Penalty", -0.3);
            applyModifier(player, StealthAttributes.MUFFLING.get(), FASTRUN_MUFFLING_UUID, "ParCool FastRun Muffling Penalty", -0.4);
        }
        // 4. Hangeln / Klettern (ClingToCliff, HangDown) -> Erhöhte Kanten-Positionen erschweren das Entdecken leicht
        else if (action instanceof ClingToCliff || action instanceof HangDown) {
            applyModifier(player, StealthAttributes.CAMOUFLAGE.get(), CLING_CAMOUFLAGE_UUID, "ParCool Cling Camouflage", 0.2);
        }
        // 5. Im Block Verstecken (HideInBlock) -> Verleiht 90% Tarnung, um das HUD-Feedback abzustimmen und Rest-Entdeckung zu behalten
        else if (action instanceof HideInBlock) {
            applyModifier(player, StealthAttributes.CAMOUFLAGE.get(), HIDEINBLOCK_CAMOUFLAGE_UUID, "ParCool HideInBlock Camouflage", 0.9);
        }
    }

    /**
     * HOOK 2: BEWEGUNGS-STATE BEENDET
     * Entfernt die Modifikatoren wieder restlos, sobald die Aktion endet.
     */
    @SubscribeEvent
    public void onParCoolActionFinish(ParCoolActionEvent.Finish.Post event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide()) return;

        Action action = event.getAction();

        if (action instanceof Crawl) {
            removeModifier(player, StealthAttributes.CAMOUFLAGE.get(), CRAWL_CAMOUFLAGE_UUID);
            removeModifier(player, StealthAttributes.MUFFLING.get(), CRAWL_MUFFLING_UUID);
        } else if (action instanceof Slide) {
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

    /**
     * HILFSMETHODE: MODIFIKATOR ANWENDEN
     */
    private void applyModifier(Player player, Attribute attribute, UUID uuid, String name, double value) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null && instance.getModifier(uuid) == null) {
            instance.addTransientModifier(new AttributeModifier(uuid, name, value, AttributeModifier.Operation.ADDITION));
        }
    }

    /**
     * HILFSMETHODE: MODIFIKATOR ENTFERNEN
     */
    private void removeModifier(Player player, Attribute attribute, UUID uuid) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null && instance.getModifier(uuid) != null) {
            instance.removeModifier(uuid);
        }
    }

    /**
     * CLIENT-REGISTRIERUNG: DUMMY KISTEN HUD
     */
    private void registerClientOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("stealth_parcool_bridge_hud", (gui, guiGraphics, partialTick, width, height) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;

            Parkourability parkourability = Parkourability.get(mc.player);
            if (parkourability == null) return;

            // Zeigt die Kiste auf dem Client, solange der Spieler im Block hockt
            if (parkourability.get(HideInBlock.class).isDoing()) {
                renderDummyHidingHUD(guiGraphics, width, height);
            } else {
                // Cache zurücksetzen, wenn der Spieler nicht mehr versteckt ist
                lastBridgeThreatLevel = ThreatLevel.NONE;
            }
        });
    }

    /**
     * RENDERING: DUMMY HIDING HUD
     * Zeichnet standardmäßig NUR die Pappkiste.
     * Sobald die Bedrohungsstufe auf HUNTED springt, wird die Kiste durch
     * das weit geöffnete, rote Alarm-Auge ersetzt!
     * * * NEU: Spielt verlässlich das Entdeckungs-Geräusch ab, falls die Bedrohung umschlägt
     * * und der Server keinen Dämpfungs-Befehl gesendet hat.
     */
    private void renderDummyHidingHUD(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Reflection-Abfrage der aktuellen Bedrohungsstufe aus der StealthHud
        ThreatLevel maxThreat = getThreatLevel();

        // SOUND-TRIGER: Erkennung des Bedrohungs-Umschlags auf HUNTED
        if (maxThreat == ThreatLevel.HUNTED && lastBridgeThreatLevel != ThreatLevel.HUNTED) {
            long suppressSoundUntil = getSuppressSoundUntil();
            if (System.currentTimeMillis() > suppressSoundUntil) {
                mc.player.playSound(StealthSounds.DETECTED.get(), 1.0f, 1.0f);
            }
        }
        lastBridgeThreatLevel = maxThreat;

        if (maxThreat == ThreatLevel.HUNTED) {
            // ALARM-MODUS: Kiste verschwindet, weites rotes Auge wird gerendert
            RenderSystem.setShaderColor(1.0f, 0.0f, 0.0f, 0.95f); // Knallrot
            StealthTextureHelper.TextureDimensions eyeDims = StealthTextureHelper.getDimensions(TEX_EYE_WIDE, 16, 16);
            int eyeX = StealthHud.getAbsoluteX(StealthHudConfig.eyeX, eyeDims.width, screenWidth);
            int eyeY = StealthHud.getAbsoluteY(StealthHudConfig.eyeY, eyeDims.height, screenHeight);
            guiGraphics.blit(TEX_EYE_WIDE, eyeX, eyeY, 0, 0, eyeDims.width, eyeDims.height, eyeDims.width, eyeDims.height);
        } else {
            // TARN-MODUS: Zeige nur die Pappkiste in ruhigem Graublau
            RenderSystem.setShaderColor(0.4f, 0.6f, 0.9f, 0.9f); // Graublau
            StealthTextureHelper.TextureDimensions dims = StealthTextureHelper.getDimensions(TEX_HIDING_BOX, 16, 16);
            int x = StealthHud.getAbsoluteX(StealthHudConfig.eyeX, dims.width, screenWidth);
            int y = StealthHud.getAbsoluteY(StealthHudConfig.eyeY, dims.height, screenHeight);
            guiGraphics.blit(TEX_HIDING_BOX, x, y, 0, 0, dims.width, dims.height, dims.width, dims.height);
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    /**
     * REFLECTION HELFER: Holt die aktuelle Bedrohungsstufe geräuschlos aus der StealthHud
     */
    private static ThreatLevel getThreatLevel() {
        try {
            Field field = StealthHud.class.getDeclaredField("currentThreatLevel");
            field.setAccessible(true);
            return (ThreatLevel) field.get(null);
        } catch (Exception e) {
            return ThreatLevel.NONE;
        }
    }

    /**
     * REFLECTION HELFER: Holt die aktuelle Sichtbarkeit geräuschlos aus der StealthHud
     */
    private static float getVisibility() {
        try {
            Field field = StealthHud.class.getDeclaredField("currentVisibility");
            field.setAccessible(true);
            return (float) field.get(null);
        } catch (Exception e) {
            return 0.0f;
        }
    }

    /**
     * REFLECTION HELFER: Holt die Sound-Unterdrückungsgrenze geräuschlos aus der StealthHud
     */
    private static long getSuppressSoundUntil() {
        try {
            Field field = StealthHud.class.getDeclaredField("suppressSoundUntil");
            field.setAccessible(true);
            return (long) field.get(null);
        } catch (Exception e) {
            return 0L;
        }
    }
}