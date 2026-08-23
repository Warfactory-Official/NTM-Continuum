package com.hbm;

import com.hbm.platform.Services;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;

public class CommonClass {

    public static void init() {

        // Populate all DeferredRegisters. Loaders flush them afterwards (NeoForge via RegisterEvent,
        // Fabric via DeferredRegister.flushAll). This must run before registration is flushed.
        com.hbm.main.MainRegistry.bootstrap();

        Constants.LOG.info("Hello from Common init on {}! we are currently in a {} environment!", Services.PLATFORM.getPlatformName(), Services.PLATFORM.getEnvironmentName());
        Constants.LOG.info("The ID for diamonds is {}", BuiltInRegistries.ITEM.getKey(Items.DIAMOND));

        if (Services.PLATFORM.isModLoaded("hbm")) {

            Constants.LOG.info("Hello to hbm");
        }
    }
}