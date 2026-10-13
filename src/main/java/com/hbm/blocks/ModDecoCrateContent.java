package com.hbm.blocks;

import com.hbm.inventory.ModDecoCrateMenus;
import com.hbm.tileentity.ModDecoCrateBlockEntities;

/**
 * One bootstrap hook for the storage crates, decorative props and simple blocks ({@link ModDecoCrateBlocks}).
 */
public final class ModDecoCrateContent {

    private ModDecoCrateContent() {}

    public static void init() {
        ModDecoCrateBlocks.init();
        ModDecoCrateBlockEntities.init();
        ModDecoCrateMenus.init();
        DecoCrateDeclarations.declare();
    }
}
