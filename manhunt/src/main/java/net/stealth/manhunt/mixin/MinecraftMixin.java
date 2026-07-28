package net.stealth.manhunt.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.stealth.manhunt.client.ClientEchoRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    /**
     * Überfängt die clientseitige Methode, die entscheidet, ob eine Entity leuchtend umrandet werden soll.
     * Da Geckolib explizit "mc.shouldEntityAppearGlowing(entity)" aufruft, wird das System dadurch
     * zu 100% kompatibel mit allen Geckolib-Modellen, Custom-Renderern und Epic-Fight-Animationen!
     */
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void manhunt$onShouldEntityAppearGlowing(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof LivingEntity living) {
            if (ClientEchoRenderer.shouldGlow(living)) {
                cir.setReturnValue(true);
            }
        }
    }
}