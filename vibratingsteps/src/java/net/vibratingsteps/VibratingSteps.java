package net.vibratingsteps;

import com.alrex.parcool.api.unstable.action.ParCoolActionEvent;
import com.alrex.parcool.common.action.Action;
import com.alrex.parcool.common.action.impl.*;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * ARCHITEKTUR: STANDALONE PARCOOL VIBRATION COUPLER (Mod 1)
 * Fängt ParCools interne Bewegungs-Events ab und übersetzt sie auf der Serverseite
 * in standardisierte Minecraft-Vibrationen (GameEvents).
 * * Diese Mod repariert die fehlende Sculk-Kompatibilität von ParCool, sodass der
 * Warden und Sculk-Sensoren (sowie das Gehör der StealthMod) auf Parkour reagieren.
 */
@Mod(VibratingSteps.MOD_ID)
public class VibratingSteps {
    public static final String MOD_ID = "vibratingsteps";
    private static final Logger LOGGER = LogUtils.getLogger();

    public VibratingSteps() {
        // Registriert die Mod auf dem Forge Event Bus
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("[VibratingSteps] Successfully registered ParCool vibration fix!");
    }

    /**
     * HOOK 1: KONTINUIERLICHE AKTIONS-GERÄUSCHE (Tick-basiert)
     * Verarbeitet Bewegungen, die über einen Zeitraum hinweg konstanten Lärm machen.
     * Nutzt ein Intervall-Modulo, um ein natürliches Schritt-Tempo für Vibrationen zu simulieren.
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

        // 1. Schnelles Laufen (FastRun) -> Schnelle, kontinuierliche Schritte
        if (action instanceof FastRun) {
            if (ticks % 5 == 0) {
                player.gameEvent(GameEvent.STEP);
            }
        }
        // 2. Schnelles Schwimmen (FastSwim) -> Kontinuierliches Platschen
        else if (action instanceof FastSwim) {
            if (ticks % 5 == 0) {
                player.gameEvent(GameEvent.SWIM);
            }
        }
        // 3. Wandlauf horizontal (HorizontalWallRun) -> Schnelle Berührungen an der Wand
        else if (action instanceof HorizontalWallRun) {
            if (ticks % 4 == 0) {
                player.gameEvent(GameEvent.STEP); // Mobs hören Schritte an der Wand
            }
        }
        // 4. Rutschen / Sliden -> Erzeugt kontinuierliches Schleifgeräusch
        else if (action instanceof Slide) {
            if (ticks % 3 == 0) {
                player.gameEvent(GameEvent.STEP);
            }
        }
    }

    /**
     * HOOK 2: WUCHTIGE EINZEL-GERÄUSCHE (Start-basiert)
     * Fängt den genauen Moment ab, in dem eine explosive Aktion ausgelöst wird.
     * Erzeugt wuchtige Erschütterungen (HIT_GROUND), die Mobs sofort alarmieren.
     */
    @SubscribeEvent
    public void onParCoolActionStart(ParCoolActionEvent.Start.Post event) {
        Player player = event.getPlayer();

        // Server-Side Guard
        if (player.level().isClientSide()) {
            return;
        }

        Action action = event.getAction();

        // 1. Wuchtige Wandabsprünge (WallJump) -> Erschütterung
        if (action instanceof WallJump) {
            player.gameEvent(GameEvent.HIT_GROUND);
        }
        // 2. Vertikaler Wandlauf-Start (VerticalWallRun) -> Kraftvoller Abstoß nach oben
        else if (action instanceof VerticalWallRun) {
            player.gameEvent(GameEvent.HIT_GROUND);
        }
        // 3. Hechtsprung (CatLeap) / Weitsprung (ChargeJump) -> Explosiver Absprung
        else if (action instanceof CatLeap || action instanceof ChargeJump) {
            player.gameEvent(GameEvent.HIT_GROUND);
        }
        // 4. Schnelles Hechtrollen (Roll) -> Dumpfer Aufprall am Boden vor dem Abrollen
        else if (action instanceof Roll) {
            player.gameEvent(GameEvent.STEP);
        }
        // 5. Schnelles Ausweichen (Dodge) -> Schneller, hörbarer Ausfallschritt
        else if (action instanceof Dodge) {
            player.gameEvent(GameEvent.STEP);
        }
        // 6. Hindernis-Überwindung (Vault) -> Hände stoßen sich kraftvoll vom Block ab
        else if (action instanceof Vault) {
            player.gameEvent(GameEvent.STEP);
        }
        // 7. Klimmzug (ClimbUp) / Stangen-Absprung (JumpFromBar)
        else if (action instanceof ClimbUp || action instanceof JumpFromBar) {
            player.gameEvent(GameEvent.STEP);
        }
    }
}