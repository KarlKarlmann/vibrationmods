package net.targetoutlines.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.targetoutlines.TargetOutlinesMod;

public class TargetNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static SimpleChannel CHANNEL;

    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TargetOutlinesMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
        );

        CHANNEL.registerMessage(0,
            SyncTargetsPacket.class,
            SyncTargetsPacket::encode,
            SyncTargetsPacket::new,
            SyncTargetsPacket::handle
        );
    }
}