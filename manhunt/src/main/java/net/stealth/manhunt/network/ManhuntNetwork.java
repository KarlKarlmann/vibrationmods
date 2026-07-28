package net.stealth.manhunt.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.stealth.manhunt.StealthManhunt;

/**
 * Registriert alle Netzwerkpakete für die Manhunt-Mod.
 * Beinhaltet nun auch die punktgenauen akustischen Wellen.
 */
public class ManhuntNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static SimpleChannel CHANNEL;

    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(StealthManhunt.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
        );

        int id = 0;
        
        // ID 0: Zwingt ein LivingEntity zum Glühen
        CHANNEL.registerMessage(id++,
            ClientboundMarkEchoPacket.class,
            ClientboundMarkEchoPacket::encode,
            ClientboundMarkEchoPacket::new,
            ClientboundMarkEchoPacket::handle
        );


        // ID 1: Punktgenaue akustische Wellen an Koordinaten (Einschläge, Hebel)
        CHANNEL.registerMessage(id++,
            ClientboundAcousticWavePacket.class,
            ClientboundAcousticWavePacket::encode,
            ClientboundAcousticWavePacket::new,
            ClientboundAcousticWavePacket::handle
        );
    }
}