package com.hbm.blocks;

import com.hbm.inventory.ModExplosivesMenus;
import com.hbm.items.ModExplosivesItems;
import com.hbm.tileentity.ModExplosivesBlockEntities;


/** One common bootstrap hook for the conventional explosives, mines, charges and detonators. */
public final class ModExplosivesContent {
    private ModExplosivesContent() {}

    public static void init() {
        ModExplosivesBlocks.init();
        ModExplosivesItems.init();
        ModExplosivesBlockEntities.init();
        ModExplosivesMenus.init();
        ExplosivesDeclarations.declare();
    }
}
