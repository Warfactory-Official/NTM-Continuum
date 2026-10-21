package com.hbm.compat.sable;

import com.hbm.Constants;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/** NeoForge's entry to HBM's Sable integration ({@link HbmSableIntegration}), found by NeoForge's annotation scan. */
@EventBusSubscriber(modid = Constants.MOD_ID)
public final class HbmSable {

    private HbmSable() {}

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        if (ModList.get().isLoaded("sable")) HbmSableIntegration.setup();
    }
}
