package com.hbm.inventory.gui;

import com.hbm.client.HbmClientRegistrations;
import com.hbm.inventory.ModGunSupportMenus;

/** Client registration helper for the stack-backed gun support menus. */
public final class GunSupportClientRegistrations {

    private GunSupportClientRegistrations() {}

    public static void registerScreens(HbmClientRegistrations.ScreenSink sink) {
        sink.register(ModGunSupportMenus.AMMO_BAG.get(), ScreenItemBox::new);
        sink.register(ModGunSupportMenus.CASING_BAG.get(), ScreenItemBox::new);
    }
}
