package net.parcoolstealthbridge.client;

import com.alrex.parcool.common.action.impl.HideInBlock;
import com.alrex.parcool.common.capability.Parkourability;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.stealth.client.StealthHud;
import net.stealth.client.StealthHudConfig;
import net.stealth.registry.StealthSounds;
import net.stealth.util.StealthTextureHelper;
import net.stealth.util.ThreatLevel;

import java.lang.reflect.Field;

public class ParCoolStealthBridgeClient {

    private static final ResourceLocation TEX_HIDING_BOX = new ResourceLocation("stealth", "textures/gui/hiding_box.png");
    private static final ResourceLocation TEX_EYE_WIDE = new ResourceLocation("stealth", "textures/gui/eyewide.png");

    private static ThreatLevel lastBridgeThreatLevel = ThreatLevel.NONE;

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(ParCoolStealthBridgeClient::registerClientOverlays);
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

    private static void renderDummyHidingHUD(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        ThreatLevel maxThreat = getThreatLevel();

        if (maxThreat == ThreatLevel.HUNTED && lastBridgeThreatLevel != ThreatLevel.HUNTED) {
            long suppressSoundUntil = getSuppressSoundUntil();
            if (System.currentTimeMillis() > suppressSoundUntil) {
                mc.player.playSound(StealthSounds.DETECTED.get(), 1.0f, 1.0f);
            }
        }
        lastBridgeThreatLevel = maxThreat;

        if (maxThreat == ThreatLevel.HUNTED) {
            RenderSystem.setShaderColor(1.0f, 0.0f, 0.0f, 0.95f);
            StealthTextureHelper.TextureDimensions eyeDims = StealthTextureHelper.getDimensions(TEX_EYE_WIDE, 16, 16);
            int eyeX = StealthHud.getAbsoluteX(StealthHudConfig.eyeX, eyeDims.width, screenWidth);
            int eyeY = StealthHud.getAbsoluteY(StealthHudConfig.eyeY, eyeDims.height, screenHeight);
            guiGraphics.blit(TEX_EYE_WIDE, eyeX, eyeY, 0, 0, eyeDims.width, eyeDims.height, eyeDims.width, eyeDims.height);
        } else {
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