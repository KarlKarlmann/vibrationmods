package net.vibratingsteps;

import com.alrex.parcool.api.unstable.action.ParCoolActionEvent;
import com.alrex.parcool.common.action.Action;
import com.alrex.parcool.common.action.impl.*;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.vibratingsteps.config.VibratingStepsConfig;
import org.slf4j.Logger;

/**
 * ARCHITEKTUR: STANDALONE PARCOOL VIBRATION COUPLER (Mod 1)
 * Fängt ParCools interne Bewegungs-Events ab und übersetzt sie auf der Serverseite
 * in standardisierte Minecraft-Vibrationen (GameEvents).
 * * * CONFIGURABLE UPDATE: Alle Events, Intervalle und Trigger sind nun über
 * * die serverseitige TOML-Datei flexibel anpassbar.
 */
@Mod(VibratingSteps.MOD_ID)
public class VibratingSteps {
    public static final String MOD_ID = "vibratingsteps";
    private static final Logger LOGGER = LogUtils.getLogger();

    public VibratingSteps() {
        // Registriert die Server-Konfiguration bei Forge
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, VibratingStepsConfig.SPEC, "vibratingsteps-server.toml");

        // Registriert die Mod auf dem Forge Event Bus
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("[VibratingSteps] Successfully registered configurable ParCool vibration fix!");
    }

    /**
     * HOOK 1: KONTINUIERLICHE AKTIONS-GERÄUSCHE (Tick-basiert)
     * Verarbeitet Bewegungen, die über einen Zeitraum hinweg konstanten Lärm machen.
     * Nutzt ein konfigurierbares Intervall-Modulo zur Simulation der Schritte.
     */
    @SubscribeEvent
    public void onParCoolActionTick(ParCoolActionEvent.Tick.Post event) {
        Player player = event.getPlayer();
        
        // Logik darf ausschließlich auf der logischen Serverseite laufen!
        if (player.level().isClientSide()) {
            return;
        }

        Action action = event.getAction();
        if (!action.isDoing()) {
            return;
        }

        int ticks = action.getDoingTick();

        // 1. Schnelles Laufen (FastRun)
        if (action instanceof FastRun) {
            if (VibratingStepsConfig.FASTRUN_ENABLED.get() && ticks % VibratingStepsConfig.FASTRUN_INTERVAL.get() == 0) {
                triggerConfiguredEvent(player, VibratingStepsConfig.FASTRUN_EVENT.get());
            }
        }
        // 2. Schnelles Schwimmen (FastSwim)
        else if (action instanceof FastSwim) {
            if (VibratingStepsConfig.FASTSWIM_ENABLED.get() && ticks % VibratingStepsConfig.FASTSWIM_INTERVAL.get() == 0) {
                triggerConfiguredEvent(player, VibratingStepsConfig.FASTSWIM_EVENT.get());
            }
        }
        // 3. Wandlauf horizontal (HorizontalWallRun)
        else if (action instanceof HorizontalWallRun) {
            if (VibratingStepsConfig.WALLRUN_ENABLED.get() && ticks % VibratingStepsConfig.WALLRUN_INTERVAL.get() == 0) {
                triggerConfiguredEvent(player, VibratingStepsConfig.WALLRUN_EVENT.get());
            }
        }
        // 4. Rutschen / Sliden
        else if (action instanceof Slide) {
            if (VibratingStepsConfig.SLIDE_ENABLED.get() && ticks % VibratingStepsConfig.SLIDE_INTERVAL.get() == 0) {
                triggerConfiguredEvent(player, VibratingStepsConfig.SLIDE_EVENT.get());
            }
        }
    }

    /**
     * HOOK 2: WUCHTIGE EINZEL-GERÄUSCHE (Start-basiert)
     * Fängt den genauen Moment ab, in dem eine explosive Aktion ausgelöst wird.
     * Erzeugt konfigurierbare Erschütterungen, die Mobs sofort alarmieren.
     */
    @SubscribeEvent
    public void onParCoolActionStart(ParCoolActionEvent.Start.Post event) {
        Player player = event.getPlayer();

        // Server-Side Guard
        if (player.level().isClientSide()) {
            return;
        }

        Action action = event.getAction();

        // 1. Wuchtige Wandabsprünge (WallJump)
        if (action instanceof WallJump) {
            if (VibratingStepsConfig.WALLJUMP_ENABLED.get()) {
                triggerConfiguredEvent(player, VibratingStepsConfig.WALLJUMP_EVENT.get());
            }
        }
        // 2. Vertikaler Wandlauf-Start (VerticalWallRun)
        else if (action instanceof VerticalWallRun) {
            if (VibratingStepsConfig.VERTICAL_WALLRUN_ENABLED.get()) {
                triggerConfiguredEvent(player, VibratingStepsConfig.VERTICAL_WALLRUN_EVENT.get());
            }
        }
        // 3. Hechtsprung (CatLeap) / Weitsprung (ChargeJump)
        else if (action instanceof CatLeap) {
            if (VibratingStepsConfig.CATLEAP_ENABLED.get()) {
                triggerConfiguredEvent(player, VibratingStepsConfig.CATLEAP_EVENT.get());
            }
        }
        else if (action instanceof ChargeJump) {
            if (VibratingStepsConfig.CHARGEJUMP_ENABLED.get()) {
                triggerConfiguredEvent(player, VibratingStepsConfig.CHARGEJUMP_EVENT.get());
            }
        }
        // 4. Schnelles Hechtrollen (Roll)
        else if (action instanceof Roll) {
            if (VibratingStepsConfig.ROLL_ENABLED.get()) {
                triggerConfiguredEvent(player, VibratingStepsConfig.ROLL_EVENT.get());
            }
        }
        // 5. Schnelles Ausweichen (Dodge)
        else if (action instanceof Dodge) {
            if (VibratingStepsConfig.DODGE_ENABLED.get()) {
                triggerConfiguredEvent(player, VibratingStepsConfig.DODGE_EVENT.get());
            }
        }
        // 6. Hindernis-Überwindung (Vault)
        else if (action instanceof Vault) {
            if (VibratingStepsConfig.VAULT_ENABLED.get()) {
                triggerConfiguredEvent(player, VibratingStepsConfig.VAULT_EVENT.get());
            }
        }
        // 7. Klimmzug (ClimbUp) / Stangen-Absprung (JumpFromBar)
        else if (action instanceof ClimbUp) {
            if (VibratingStepsConfig.CLIMBUP_ENABLED.get()) {
                triggerConfiguredEvent(player, VibratingStepsConfig.CLIMBUP_EVENT.get());
            }
        }
        else if (action instanceof JumpFromBar) {
            if (VibratingStepsConfig.JUMPFROMBAR_ENABLED.get()) {
                triggerConfiguredEvent(player, VibratingStepsConfig.JUMPFROMBAR_EVENT.get());
            }
        }
    }

    /**
     * Hilfsmethode, um ein GameEvent dynamisch anhand seines Registry-Strings zu triggern.
     * Schützt vor Server-Abstürzen durch Tippfehler der Nutzer in der TOML-Konfiguration.
     */
    private void triggerConfiguredEvent(Player player, String eventRegistryName) {
        try {
            ResourceLocation location = new ResourceLocation(eventRegistryName);
            GameEvent event = BuiltInRegistries.GAME_EVENT.get(location);
            
            // Wenn das Event in den Registries existiert (auch aus anderen Mods!), triggern wir es.
            if (event != null) {
                player.gameEvent(event);
            } else {
                LOGGER.warn("[VibratingSteps] Configured GameEvent '{}' was not found in registries!", eventRegistryName);
            }
        } catch (Exception e) {
            LOGGER.error("[VibratingSteps] Failed to parse GameEvent '{}' from configuration!", eventRegistryName, e);
        }
    }
}