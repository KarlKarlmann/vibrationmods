package net.stealth.manhunt.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.stealth.manhunt.client.ClientEchoRenderer;

import java.util.function.Supplier;

/**
 * Übermittelt eine punktgenaue akustische Welle an den Client.
 * Wird für entfernte Erschütterungen (wie Projektileinschläge, Block-Abbau oder Hebel) genutzt,
 * um dort konzentrische, im Dunkeln glimmende Kreise zu erzeugen.
 */
public class ClientboundAcousticWavePacket {
    public final double x;
    public final double y;
    public final double z;
    public final float volume;

    public ClientboundAcousticWavePacket(double x, double y, double z, float volume) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.volume = volume;
    }

    public ClientboundAcousticWavePacket(FriendlyByteBuf buf) {
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.volume = buf.readFloat();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeFloat(this.volume);
    }

    public static void handle(ClientboundAcousticWavePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                // Triggert den wunderschönen Seelenpartikel-Ripple-Effekt am Client
                ClientEchoRenderer.spawnAcousticWave(msg.x, msg.y, msg.z, msg.volume);
            })
        );
        ctx.get().setPacketHandled(true);
    }
}