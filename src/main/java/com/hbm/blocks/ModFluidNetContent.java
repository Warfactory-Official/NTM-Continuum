package com.hbm.blocks;

import com.hbm.blocks.fluid.ModWorldFluids;
import com.hbm.inventory.ModFluidNetMenus;
import com.hbm.items.ModFluidNetItems;
import com.hbm.tileentity.ModFluidNetBlockEntities;


/** One bootstrap hook for the fluid-network family: blocks, items, block entities, menus. */
public final class ModFluidNetContent {

    private ModFluidNetContent() {}

    public static void init() {
        ModWorldFluids.init(); // the 1.7 world fluids, before the blocks that carry them
        ModFluidNetBlocks.init();
        ModFluidNetItems.init();
        ModFluidNetBlockEntities.init();
        ModFluidNetMenus.init();
    }
}
