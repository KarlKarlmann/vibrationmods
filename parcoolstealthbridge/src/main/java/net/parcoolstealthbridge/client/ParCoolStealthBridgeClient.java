package net.parcoolstealthbridge.client;

import com.alrex.parcool.common.action.impl.Crawl;
import com.alrex.parcool.common.action.impl.HideInBlock;
import com.alrex.parcool.common.capability.Parkourability;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.parcoolstealthbridge.config.BridgeConfig;
import net.stealth.client.StealthHud;
import net.stealth.client.StealthHudConfig;
import net.stealth.registry.StealthSounds;
import net.stealth.util.StealthTextureHelper;
import net.stealth.util.ThreatLevel;

import java.lang.reflect.Field;

/**
 * ARCHITEKTUR: CLIENT-ONLY SIDE HANDLER
 * Kapselt alle Render-Pipelines, Minecraft-Client-Referenzen, Tick-Events und Reflection-Hooks.
 * Wird auf Dedicated Servern niemals geladen und verhindert somit Classloading-Abstürze.
 */
public class ParCoolStealthBridgeClient {

    private static final ResourceLocation TEX_HIDING_BOX = new ResourceLocation("stealth", "textures/gui/hiding_box.png");
    private static final ResourceLocation TEX_EYE_WIDE = new ResourceLocation("stealth", "textures/gui/eyewide.png");

    private static ThreatLevel lastBridgeThreatLevel = ThreatLevel.NONE;

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(ParCoolStealthBridgeClient::registerClientOverlays);
        
        // Registriert diese Client-Klasse auf dem Forge Event Bus, um Ticks zu horchen
        MinecraftForge.EVENT_BUS.register(ParCoolStealthBridgeClient.class);
    }

    private static void registerClientOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("stealth_parcool_bridge_hud", (gui, guiGraphics, partialTick, width, height) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;

            Parkourability parkourability = Parkourability.get(mc.player);
            if (parkourability == null) return;

            // Zeigt die Kiste auf dem Client, solange der Spieler im Block hockt
            if (parkourability.get(HideInBlock.class).isDoing()) {
                renderDummyHidingHUD(guiGraphics, width, height);
            } else {
                lastBridgeThreatLevel = ThreatLevel.NONE;
            }
        });
    }

    /**
     * HOOK: CLIENT-TICK EVENT
     * Fängt jeden Tick auf dem Client ab, um den Crawling-Status abzufragen.
     * Erzwingt das Rendern des standardmäßigen Stealth-HUDs über das Mod-übergreifende Flag,
     * wenn 'show_hud_on_crawl' aktiviert ist.
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                Parkourability parkourability = Parkourability.get(mc.player);
                if (parkourability != null) {
                    boolean isCrawling = parkourability.get(Crawl.class).isDoing();
                    
                    // Schaltet das forceHudRender-Flag der StealthMod um
                    boolean forceHud = isCrawling && BridgeConfig.CLIENT.showHudOnCrawl.get();
                    StealthHud.forceHudRender = forceHud;
                } else {
                    StealthHud.forceHudRender = false;
                }
            }
        }
    }

    private static void renderDummyHidingHUD(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Reflection-Abfrage der aktuellen Bedrohungsstufe aus der StealthHud
        ThreatLevel maxThreat = getThreatLevel();

        // SOUND-TRIGGER: Erkennung des Bedrohungs-Umschlags auf HUNTED
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
            StealthTextureHelper.TextureDimensions dims = StealthTextureHelper.getDimensions(TEX_HIDING_BOX, 16, 16);
            int x = StealthHud.getAbsoluteX(StealthHudConfig.eyeX, dims.width, screenWidth);
            int y = StealthHud.getAbsoluteY(StealthHudConfig.eyeY, dims.height, screenHeight);
            guiGraphics.blit(TEX_HIDING_BOX, x, y, 0, 0, dims.width, dims.height, dims.width, dims.height);
        }

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private static ThreatLevel getThreatLevel() {
        try {
            Field field = StealthHud.class.getDeclaredField("currentThreatLevel");
            field.setAccessible(true);
            return (ThreatLevel) field.get(null);
        } catch (Exception e) {
            return ThreatLevel.NONE;
        }
    }

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