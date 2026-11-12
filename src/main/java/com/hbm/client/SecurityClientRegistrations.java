package com.hbm.client;

import com.hbm.inventory.ModSecurityMenus;
import com.hbm.inventory.gui.GUIMachineKeyForge;

/** Client hooks for the security family: the locksmith screen. */
public final class SecurityClientRegistrations {
    private SecurityClientRegistrations() {}

    public static void registerScreens(HbmClientRegistrations.ScreenSink sink) {
        sink.register(ModSecurityMenus.MACHINE_KEYFORGE.get(), GUIMachineKeyForge::new);
    }
}
