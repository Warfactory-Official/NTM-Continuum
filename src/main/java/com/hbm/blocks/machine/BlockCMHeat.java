package com.hbm.blocks.machine;

import dev.golem.core.world.CompatBlock;

/**
 * 1.7 {@code BlockCMHeat}, a {@code BlockPillar} ({@code cm_heat_top} ends, {@code cm_heat_side} sides). As a
 * custom machine component it lets a controller with {@code maxHeat > 0} draw heat from sources around it.
 */
public class BlockCMHeat extends CompatBlock {

    public BlockCMHeat(Properties properties) {
        super(properties);
    }
}
