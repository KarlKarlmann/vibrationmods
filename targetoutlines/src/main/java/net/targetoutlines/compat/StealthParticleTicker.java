package net.targetoutlines.compat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.stealth.capability.StealthStateProvider;
import net.targetoutlines.registry.ModParticles; // Unser neues Register

public class StealthParticleTicker {

    @SubscribeEvent
    public void onMobTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        
        if (event.getEntity() instanceof Mob mob) {
            if (mob.tickCount % 10 != 0) return;

            // LOGIK-UPDATE: Wenn der Mob bereits einen Spieler anvisiert,
            // hat er bereits die rote Outline. Keine Partikel nötig!
            if (mob.getTarget() instanceof Player) {
                return;
            }

            mob.getCapability(StealthStateProvider.STEALTH_CAPABILITY).ifPresent(state -> {
                float alertLevel = state.getAlertLevel();
                
                if (alertLevel > 0.0f && mob.level() instanceof ServerLevel serverLevel) {
                    
                    // FALL 1: Voll alarmiert (Hunted-Zustand, sucht aber noch die Sichtlinie) -> Das "!"
                    if (alertLevel >= 0.95f) {
                        serverLevel.sendParticles(
                            ModParticles.ALERT_MARK.get(), 
                            mob.getX(), mob.getEyeY() + 0.6, mob.getZ(), 
                            1, 0.0, 0.0, 0.0, 0.0
                        );
                    } 
                    // FALL 2: Semi-alarmiert (Suspicious/Watched, untersucht Geräusch) -> Das "?"
                    else if (alertLevel > 0.25f) {
                        serverLevel.sendParticles(
                            ModParticles.SUSPICIOUS_MARK.get(), 
                            mob.getX(), mob.getEyeY() + 0.6, mob.getZ(), 
                            1, 0.0, 0.0, 0.0, 0.0
                        );
                    }
                }
            });
        }
    }
}