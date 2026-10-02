package com.hbm.blocks;

import com.hbm.inventory.ModConveyorMenus;
import com.hbm.items.ModConveyorItems;
import com.hbm.tileentity.ModConveyorBlockEntities;


/** One common bootstrap hook for the conveyor belt and crane family. */
public final class ModConveyorContent {

    private ModConveyorContent() {}

    public static void init() {
        ModConveyorBlocks.init();
        ModConveyorItems.init();
        ModConveyorBlockEntities.init();
        ModConveyorMenus.init();
    }
}
