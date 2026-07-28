package net.stealth.manhunt.client.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.stealth.manhunt.client.ClientEchoRenderer;

@OnlyIn(Dist.CLIENT)
public class EchoWaveParticle extends TextureSheetParticle {

    public static final ParticleRenderType XRAY = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder buffer, TextureManager textureManager) {
            RenderSystem.disableDepthTest(); // X-RAY: Durch Wände sichtbar!
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            textureManager.bindForSetup(TextureAtlas.LOCATION_PARTICLES); // Standard Vanilla Atlas!
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public void end(Tesselator tesselator) {
            tesselator.end();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        }

        @Override
        public String toString() {
            return "manhunt:xray_particle";
        }
    };

    // Speichert die finale Größe, die der Partikel erreichen soll.
    private final float targetSize;

    protected EchoWaveParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z, dx, dy, dz);
        this.friction = 0.94F; // Sanftes Abbremsen in der Luft
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        this.lifetime = 25 + this.random.nextInt(15);
        
        // Der Partikel startet unsichtbar klein (Wassertropfen-Effekt)
        this.quadSize = 0.0F;
        // Und wächst bis zu dieser Größe an
        this.targetSize = 0.3F + (this.random.nextFloat() * 0.2F);
        
        // Kein Tint - Partikel behalten ihre Originalfarbe aus der Textur!
        this.rCol = 1.0f;
        this.gCol = 1.0f;
        this.bCol = 1.0f;
        
        // Startet voll deckend
        this.alpha = 1.0f;
        
        this.pickSprite(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return XRAY; // Nutzt unseren Custom X-Ray Renderer
    }

    @Override
    public int getLightColor(float partialTick) {
        return 15728880; // Fullbright (Leuchtet im Dunkeln)
    }

    @Override
    public void render(com.mojang.blaze3d.vertex.VertexConsumer buffer, net.minecraft.client.Camera camera, float partialTicks) {
        // === DIE MAGIE DER WAHRNEHMUNG ===
        // Der Partikel breitet sich physikalisch immer in der Welt aus.
        // Er wird aber NUR auf den Bildschirm gezeichnet, wenn das HUD (der Fokus) aktiv ist!
        if (!ClientEchoRenderer.isStealthHudActive()) return;
        
        super.render(buffer, camera, partialTicks);
    }

    @Override
    public void tick() {
        super.tick();
        
        // Berechnet den Fortschritt von 0.0 (Start) bis 1.0 (Ende)
        float progress = (float)this.age / (float)this.lifetime;
        
        // Wassertropfen-Effekt: Partikel dehnt sich über seine Lebenszeit aus
        this.quadSize = this.targetSize * progress;

        // === NEUE Transparenz-Logik ===
        // Der Partikel startet voll deckend (1.0f) und endet fast durchsichtig (0.1f)
        // Wir interpolieren linear zwischen diesen beiden Werten über die gesamte Lebensdauer.
        // Am Anfang (progress 0.0): 1.0 - (0.0 * 0.9) = 1.0
        // Am Ende (progress 1.0): 1.0 - (1.0 * 0.9) = 0.1
        this.setAlpha(1.0f - (progress * 0.9f));
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new EchoWaveParticle(level, x, y, z, dx, dy, dz, this.sprites);
        }
    }
}