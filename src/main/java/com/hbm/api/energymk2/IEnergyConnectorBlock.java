package com.hbm.api.energymk2;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;

/**
 * Interface for blocks that should visually connect to cables without having an IEnergyConnector tile
 * entity. This is meant for BLOCKS. Used for rendering only.
 *
 * @author hbm
 */
public interface IEnergyConnectorBlock {

    boolean canConnect(BlockGetter world, BlockPos pos, Direction dir);
}
