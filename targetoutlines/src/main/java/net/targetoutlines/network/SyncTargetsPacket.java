package net.targetoutlines.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.targetoutlines.client.ClientOutlineRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SyncTargetsPacket {
    private final List<Integer> targetIds;

    public SyncTargetsPacket(List<Integer> targetIds) {
        this.targetIds = targetIds;
    }

    public SyncTargetsPacket(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        this.targetIds = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            this.targetIds.add(buffer.readVarInt());
        }
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(targetIds.size());
        for (int id : targetIds) {
            buffer.writeVarInt(id);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Sicherstellen, dass das Verarbeiten der Daten nur auf dem Client passiert
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientOutlineRenderer.updateTargets(targetIds));
        });
        context.setPacketHandled(true);
    }
}