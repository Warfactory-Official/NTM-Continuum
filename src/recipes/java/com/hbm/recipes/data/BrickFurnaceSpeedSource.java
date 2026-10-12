package com.hbm.recipes.data;

import com.hbm.api.data.HbmDataMaps;
import dev.golem.core.data.DataMapSource;

/** 1.7 {@code TileEntityFurnaceBrick.burnSpeed} (its static block, in order). */
public final class BrickFurnaceSpeedSource extends DataMapSource<Integer> {

    public BrickFurnaceSpeedSource() {
        super(HbmDataMaps.BRICK_FURNACE_SPEED, false);
    }

    @Override
    protected void values() {
        put("minecraft:clay_ball", 4);
        put("hbm:ball_fireclay", 4);
        put("minecraft:netherrack", 4);
        put("minecraft:cobblestone", 2);
        // 1.7 Item.getItemFromBlock(Blocks.sand): one item for sand and red sand (metadata 0 and 1)
        put("minecraft:sand", 2);
        put("minecraft:red_sand", 2);
        // 1.7 burnSpeed.put(Item.getItemFromBlock(Blocks.log), 2) and (Blocks.log2, 2): the six 1.7 log kinds
        put("minecraft:oak_log", 2);
        put("minecraft:spruce_log", 2);
        put("minecraft:birch_log", 2);
        put("minecraft:jungle_log", 2);
        put("minecraft:acacia_log", 2);
        put("minecraft:dark_oak_log", 2);
    }
}
