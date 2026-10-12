package com.hbm.recipes.data;

import com.hbm.api.data.DiFurnaceDataMaps;
import dev.golem.core.data.DataMapSource;

/** 1.7 {@code TileEntityDiFurnace.getItemPower}, item by item in its order. */
public final class DiFurnaceFuelSource extends DataMapSource<Integer> {

    public DiFurnaceFuelSource() {
        super(DiFurnaceDataMaps.DIFURNACE_FUEL, false);
    }

    @Override
    protected void values() {
        // 1.7 item == Items.coal: one item for coal and charcoal (metadata 0 and 1)
        put("minecraft:coal", 200);
        put("minecraft:charcoal", 200);
        put("minecraft:coal_block", 2000);
        // 1.7 block_coke was one metadata block; the port has one block per coke
        put("hbm:block_coke_coal", 4000);
        put("hbm:block_coke_lignite", 4000);
        put("hbm:block_coke_petroleum", 4000);
        put("minecraft:lava_bucket", 12800);
        put("minecraft:blaze_rod", 1000);
        put("minecraft:blaze_powder", 300);
        put("hbm:lignite", 150);
        put("hbm:powder_lignite", 150);
        put("hbm:powder_coal", 200);
        // 1.7 briquette was one metadata item
        put("hbm:briquette_coal", 200);
        put("hbm:briquette_lignite", 200);
        put("hbm:briquette_wood", 200);
        // 1.7 item == ModItems.coke (the item, not the ore dictionary): one metadata item, one port item per coke
        put("hbm:coke_coal", 400);
        put("hbm:coke_lignite", 400);
        put("hbm:coke_petroleum", 400);
        put("hbm:solid_fuel", 400);
    }
}
