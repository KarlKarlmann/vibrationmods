package net.targetoutlines.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import org.jetbrains.annotations.NotNull;
import net.minecraft.core.particles.SimpleParticleType;

public class IndicatorParticle extends TextureSheetParticle {

    protected IndicatorParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.friction = 0.8F;
        this.xd = 0.0;
        this.yd = 0.02; // Ganz leichtes Aufwärtsschweben
        this.zd = 0.0;
        this.quadSize *= 1.5F; // Größe des Symbols über dem Kopf
        this.lifetime = 20; // Hält genau 1 Sekunde (20 Ticks)
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        // Erzeugt einen coolen Ausblend-Effekt am Ende der Lebenszeit
        this.alpha = (float)(this.lifetime - this.age) / (float)this.lifetime;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; // Erlaubt Transparenz im PNG
    }

    // Factory für die Forge-Registrierung
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(@NotNull SimpleParticleType type, @NotNull ClientLevel level, 
                                       double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            IndicatorParticle particle = new IndicatorParticle(level, x, y, z, this.sprites);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}