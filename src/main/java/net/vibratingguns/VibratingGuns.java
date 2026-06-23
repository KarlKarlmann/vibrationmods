package com.example.vibratingguns;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(VibratingGuns.MOD_ID)
public class VibratingGuns {
    public static final String MOD_ID = "vibratingguns";

    public VibratingGuns() {
        MinecraftForge.EVENT_BUS.register(this);
    }

		@SubscribeEvent
		public void onGunFire(GunFireEvent event) {
			// Logik läuft ausschließlich auf der Serverseite
			if (event.getLogicalSide().isServer()) {
				LivingEntity shooter = event.getShooter();
				
				if (shooter != null) {
					boolean isSilenced = false;
					
					// Wir holen uns den Zubehör-Zwischenspeicher des Schützen von TACZ
					AttachmentCacheProperty cacheProperty = IGunOperator.fromLivingEntity(shooter).getCacheProperty();
					if (cacheProperty != null) {
						// TACZ speichert den Schalldämpfer-Status als Pair<Integer, Boolean> unter dem Key "silence"
						Pair<Integer, Boolean> silence = cacheProperty.getCache("silence");
						if (silence != null) {
							isSilenced = silence.right();
					}

					if (isSilenced) {
						// Mit Schalldämpfer: Ein normales Projektil-Event
						shooter.gameEvent(GameEvent.PROJECTILE_SHOOT);
					} else {
						shooter.gameEvent(GameEvent.EXPLODE);
					}
				}
			}
		}
	}
}