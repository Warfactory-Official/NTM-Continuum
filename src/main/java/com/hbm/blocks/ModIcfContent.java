package com.hbm.blocks;

import com.hbm.inventory.ModIcfMenus;
import com.hbm.items.ModIcfItems;
import com.hbm.tileentity.ModIcfBlockEntities;

/**
 * One bootstrap hook for the inertial confinement fusion reactor: the reactor and its seed, the vessel components,
 * the laser controller and array parts, the assembled laser shell, the pellet maker and the filled pellet. The two
 * tool conversions of the vessel components live in {@code NTMToolHandler}'s table with the other conversions, and
 * the plasma-forge recipes in {@code ICFRecipes}.
 */
public final class ModIcfContent {

    private ModIcfContent() {}

    public static void init() {
        ModIcfBlocks.init();
        ModIcfItems.init();
        ModIcfBlockEntities.init();
        ModIcfMenus.init();
        IcfDeclarations.declare();
    }
}
