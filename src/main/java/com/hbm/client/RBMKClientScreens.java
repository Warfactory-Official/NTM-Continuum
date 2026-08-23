package com.hbm.client;

import com.hbm.inventory.ModRBMKMenus;
import com.hbm.inventory.gui.*;

/** RBMK client registrations stay with the RBMK feature instead of growing the global registry. */
public final class RBMKClientScreens {
    private RBMKClientScreens() {}
    public static void register(HbmClientRegistrations.ScreenSink sink) {
        sink.register(ModRBMKMenus.RBMK_ROD.get(), ScreenRBMKRod::new);
        sink.register(ModRBMKMenus.RBMK_CONTROL.get(), ScreenRBMKControl::new);
        sink.register(ModRBMKMenus.RBMK_CONTROL_AUTO.get(), ScreenRBMKControlAuto::new);
        sink.register(ModRBMKMenus.RBMK_BOILER.get(), ScreenRBMKBoiler::new);
        sink.register(ModRBMKMenus.RBMK_STORAGE.get(), ScreenRBMKStorage::new);
        sink.register(ModRBMKMenus.RBMK_HEATER.get(), ScreenRBMKHeater::new);
        sink.register(ModRBMKMenus.RBMK_OUTGASSER.get(), ScreenRBMKOutgasser::new);
        sink.register(ModRBMKMenus.RBMK_CONSOLE.get(), ScreenRBMKConsole::new);
    }
}
