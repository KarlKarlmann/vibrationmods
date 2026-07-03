package net.targetoutlines.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.targetoutlines.TargetConfig;
import net.targetoutlines.client.ClientOutlineRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    /**
     * Überfängt die Methode, die entscheidet, ob ein Entity den leuchtenden Glow-Effekt bekommen soll.
     * Wenn der Mob auf unserer Gefahrenliste steht, zwingen wir das Ergebnis auf true!
     */
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void onShouldEntityAppearGlowing(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof LivingEntity living) {
            if (ClientOutlineRenderer.isTarget(living.getId())) {
                boolean xray = TargetConfig.CLIENT.seeThroughWalls.get();
                Minecraft mc = Minecraft.getInstance();
                
                // Falls X-Ray aktiv ist, leuchtet der Mob immer.
                // Falls X-Ray deaktiviert ist, leuchtet er nur, wenn der Spieler eine freie Sichtlinie hat.
                if (xray || (mc.player != null && mc.player.hasLineOfSight(living))) {
                    cir.setReturnValue(true);
                }
            }
        }
    }
}