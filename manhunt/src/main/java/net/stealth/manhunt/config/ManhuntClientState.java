package net.stealth.manhunt.config;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ManhuntClientState {
    // Standardmäßig auf true, wird beim Server-Beitritt überschrieben
    public static boolean allowHudRadar = true;
    public static boolean allowEchoParticles = true;
}