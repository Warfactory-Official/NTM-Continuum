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
        // The fluid graph's round. Its chunk and level hooks are the ones PowerNetwork already installed.
        com.hbm.api.fluidmk2.FluidNetwork.init();

        // Only a run started with -Dhbm.generateIds does anything here; see IdConstants.
        com.hbm.lib.IdConstants.install();
    }
}
