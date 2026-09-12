package com.hbm;

import com.hbm.main.MainRegistry;

public class CommonClass {

    public static void init() {

        // Populate all DeferredRegisters. Loaders flush them afterwards (NeoForge via RegisterEvent,
        // Fabric via DeferredRegister.flushAll). This must run before registration is flushed.
        MainRegistry.bootstrap();
    }
}
