package net.stealth.manhunt.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.stealth.manhunt.client.ClientEchoRenderer;

import java.util.function.Supplier;

public class ClientboundMarkEchoPacket {
    public final int entityId;
    public final int durationTicks;

    public ClientboundMarkEchoPacket(int entityId, int durationTicks) {
        this.entityId = entityId;
        this.durationTicks = durationTicks;
    }

    public ClientboundMarkEchoPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        this.durationTicks = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId);
        buf.writeVarInt(this.durationTicks);
    }

    public static void handle(ClientboundMarkEchoPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                // Leitet die ID und Dauer an deinen Renderer weiter
                ClientEchoRenderer.addEcho(msg.entityId, msg.durationTicks);
            })
        );
        ctx.get().setPacketHandled(true);
    }
}