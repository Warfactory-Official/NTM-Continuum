package com.hbm.blocks;

import com.hbm.items.ModTerrainItems;
import com.hbm.tileentity.ModTerrainBlockEntities;


/**
 * Bootstrap for the natural / terrain / hazard block family ({@code com.hbm.terrain}): blocks and their
 * items, the plant items, the five block-entity types, then the Dejson declarations. Hazard values are
 * attached later by the hbm:hazards and hbm:radiation_emission data maps.
 */
public final class ModTerrainContent {

    private ModTerrainContent() {}

    public static void init() {
        ModTerrainBlocks.init();
        ModTerrainItems.init();
        ModTerrainBlockEntities.init();
        TerrainDeclarations.declare();
    }
}
