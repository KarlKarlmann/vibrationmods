package net.stealth.manhunt.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.stealth.manhunt.client.ClientEchoRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {

    /**
     * Ändert die Render-Farbe des Glow-Effekts dynamisch auf dem Client, wenn die Entity ein Echo ist.
     * Da Minecrafts Outline-Shader die Farbe intern über "getTeamColor" bezieht, fadet der Umriss
     * nun auf absolut jedem Modell (Vanilla & Geckolib) fließend von Weiß nach Blau!
     */
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void manhunt$forceEchoGlowColor(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;
        if (self.level().isClientSide() && self instanceof LivingEntity living) {
            if (ClientEchoRenderer.shouldGlow(living)) {
                cir.setReturnValue(ClientEchoRenderer.getEchoColor(living.getId()));
            }
        }
    }
}