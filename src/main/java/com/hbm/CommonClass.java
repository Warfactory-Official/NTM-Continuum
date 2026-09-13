package com.hbm;

import com.hbm.api.energymk2.PowerNetwork;
import com.hbm.main.MainRegistry;

public class CommonClass {

    public static void init() {

        // Populate all DeferredRegisters. Loaders flush them afterwards (NeoForge via RegisterEvent,
        // Fabric via DeferredRegister.flushAll). This must run before registration is flushed.
        MainRegistry.bootstrap();

        // The power graph's per-tick round, and the channels that make a parked conductor face live again.
        PowerNetwork.init();
    }
}
