[TaCZ] Warden & Sculk Gun Detection & Vibration Integration by Karl Karlmann
============================================================================

> **Repository Description / About-Text:** A lightweight, server-side Minecraft mod bridging Timeless and Classics Zero (TACZ) gunfire with vanilla game events for Warden and Sculk detection. Built under CC BY-NC 4.0 by Karl Karlmann.

This server-side Minecraft mod dynamically integrates the gunfire from **Timeless and Classics Zero (TACZ)** with Minecraft's native acoustic game event system. No more quiet shooting in the Deep Dark---now, every shot has actual game-world consequences.

Technical Specifications & Dependencies
---------------------------------------

This mod is built for the **Forge** mod loader and requires the following environment:

-   **Minecraft Version:** `1.20.1` (Supports `[1.20.1, 1.21)`)

-   **Mod Loader:** Forge / JavaFML (Loader Version `[47,)`)

-   **Required Mods:**

    -   **Timeless and Classics Zero (TACZ)** (Version `[1.0,)` --- loaded *after* TACZ)

-   **Official Website:** [n3g.de](https://www.n3g.de "null")

How It Works
------------

The core logic is handled entirely on the logical server side inside `VibratingGuns.java` (internally registered under the Mod ID `vibratingguns`). When a player fires a weapon, the mod dynamically checks the shooter's active attachments and fires corresponding vanilla events:

-   **Unsilenced Gunfire:** Triggers the vanilla `EXPLODE` game event, alerting all nearby Sculk Sensors and the Warden.

-   **Silenced Gunfire:** Automatically detects if a valid TACZ silencer attachment is active on the weapon. If detected, it triggers the much quieter `PROJECTILE_SHOOT` game event instead.

Features
--------

-   **Dynamic Attachment Detection:** Automatically reads TACZ attachment caches on the shooter to check for active silencers.

-   **Performance-Focused:** Executes strictly on the logical server side to prevent client-side overhead or lag.

-   **Vanilla Integration:** Leverages Minecraft's native `GameEvent` system, making it compatible with any mod or mechanic that listens to standard vibration frequencies.

Technical Details
-----------------

The mod listens to the `GunFireEvent` provided by the TACZ API:

```
// Logic from VibratingGuns.java
AttachmentCacheProperty cacheProperty = IGunOperator.fromLivingEntity(shooter).getCacheProperty();
if (cacheProperty != null) {
    Pair<Integer, Boolean> silence = cacheProperty.getCache("silence");
    if (silence != null) {
        isSilenced = silence.right();
    }
}

```

License & Modpacks
------------------

-   **License:** Licensed under the **Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)**. You are free to share and adapt this material as long as you give appropriate credit and do not use it for commercial purposes.

-   **Modpacks:** You are completely free to include this mod in any CurseForge or Modrinth modpack (Non-Commercial use only).

*Created with passion (and a lot of sweat) by Karl Karlmann.*