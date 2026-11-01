package com.hbm.compat.opencomputers;

//? if opencomputers {
/*import dev.golem.core.platform.Platform;

/^*
 * Common entrypoint for OpenComputers: Rebooted integration.
 * Safe to call unconditionally from common initialization; all OpenComputers types
 * and registrations are accessed only when the mod is loaded.
 ^/
public final class OpenComputersCompat {

    public static final String MOD_ID = "opencomputers";

    private OpenComputersCompat() {}

    /^* Whether OpenComputers is loaded in the current runtime environment. ^/
    public static boolean isAvailable() {
        return Platform.isModLoaded(MOD_ID);
    }

    /^* Common init, called from MainRegistry before loader capability registration. ^/
    public static void init() {
        if (isAvailable()) {
            OpenComputersPeripherals.init();
        }
    }
}
*///?}
