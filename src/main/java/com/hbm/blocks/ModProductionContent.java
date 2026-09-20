package com.hbm.blocks;

import com.hbm.inventory.ModProductionMenus;
import com.hbm.sound.ModProductionSounds;
import com.hbm.tileentity.ModProductionBlockEntities;


/**
 * One common bootstrap hook for the production-machine family (group A): rotary furnace, thresher,
 * microwave, automatic crafting table, conveyor press and the burner press preheater.
 */
public final class ModProductionContent {
    private ModProductionContent() {}

    public static void init() {
        ModProductionBlocks.init();
        ModProductionBlockEntities.init();
        ModProductionMenus.init();
        ModProductionSounds.init();
        ProductionDeclarations.declare();
    }
}
