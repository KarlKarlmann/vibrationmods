package net.targetoutlines.compat;

import net.minecraftforge.common.MinecraftForge;

public class StealthCompatHandler {
    /**
     * Diese Methode wird nur aufgerufen, wenn Stealth geladen ist.
     * Dadurch lädt die JVM die 'StealthParticleTicker'-Klasse erst jetzt sicher in den RAM.
     */
    public static void register() {
        MinecraftForge.EVENT_BUS.register(new StealthParticleTicker());
    }
}