package com.hbm.client;

import com.hbm.inventory.ModMeleeKitMenus;
import com.hbm.inventory.gui.GUIBook;
import com.hbm.inventory.gui.GUIScreenBobmazon;
import com.hbm.inventory.gui.GUIToolBox;
import com.hbm.items.tool.ItemCatalog;

/** Client registrations of the special melee / kit family: the toolbox and book screens and the Bobmazon opener. */
public final class MeleeKitClientRegistrations {

    private MeleeKitClientRegistrations() {}

    public static void registerScreens(HbmClientRegistrations.ScreenSink sink) {
        ItemCatalog.setClientScreenOpener(GUIScreenBobmazon::open);
        sink.register(ModMeleeKitMenus.TOOLBOX.get(), GUIToolBox::new);
        sink.register(ModMeleeKitMenus.BOOK.get(), GUIBook::new);
    }
}
