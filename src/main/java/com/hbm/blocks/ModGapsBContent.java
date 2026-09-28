package com.hbm.blocks;

import com.hbm.inventory.ModHeldCrateMenus;

/** One bootstrap hook for the gap batch B systems that register anything (the held-crate menus). */
public final class ModGapsBContent {

    private ModGapsBContent() {}

    public static void init() {
        ModHeldCrateMenus.init();
    }
}
