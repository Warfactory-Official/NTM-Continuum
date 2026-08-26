package com.hbm.blocks.generic;

/**
 * Marker block for the old {@code BlockBeaconable} family. In modern Minecraft beacon membership is
 * data-driven through {@code minecraft:beacon_base_blocks}; the dedicated type keeps the hand-registered
 * family explicit without duplicating vanilla beacon logic.
 */
public class BlockBeaconable extends dev.golem.core.world.CompatBlock {

    public BlockBeaconable(Properties properties) {
        super(properties);
    }
}
