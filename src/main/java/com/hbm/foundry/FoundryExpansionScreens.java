package com.hbm.foundry;

import com.hbm.client.HbmClientRegistrations;
import com.hbm.inventory.gui.GUIMachineArcFurnaceLarge;
import com.hbm.inventory.gui.GUIMachineStrandCaster;

/** Client screen registration bridge for the two feature-local menu types. */
public final class FoundryExpansionScreens {

    private FoundryExpansionScreens() {}

    public static void register(dev.golem.core.client.ClientRegistry.ScreenSink sink) {
        sink.register(FoundryExpansion.STRAND_CASTER_MENU.get(), GUIMachineStrandCaster::new);
        sink.register(FoundryExpansion.ARC_FURNACE_MENU.get(), GUIMachineArcFurnaceLarge::new);
    }
}
