package net.stealth.manhunt.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public class MixinMobAmbientSound {

    /**
     * Fängt jedes Mal ab, wenn ein Mob sein zufälliges Idle-Geräusch (Ambient Sound) abspielt.
     * Wir zwingen die Engine dazu, parallel zum Audio auch ein physikalisches Vibration-Event auszulösen,
     * damit unser Echo-System und die Stealth-KI darauf reagieren können.
     */
    @Inject(method = "playAmbientSound", at = @At("TAIL"))
    private void manhunt$triggerAmbientVibration(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        
        // Wirft ein leises Event (Prio 3.0 in der Config), das nicht zur Eskalation führt.
        // So spawnen wunderschöne Echo-Wellen, ohne dass Wachen sofort durchdrehen!
        self.gameEvent(GameEvent.ENTITY_INTERACT);
    }
}