package com.hbm.api.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** 1.7 {@code api.hbm.ntl.IPneumaticConnector}: a block a pneumatic tube may connect to. */
public interface IPneumaticConnector {

    /**
     * Whether the given side can be connected to; {@code dir} is THIS block's side, not the connector's.
     * 1.7 answered this from the tile, but no pneumatic tile overrides the default, so it is a property of
     * the block, which lets the tube resolve its geometry without a render-thread block-entity read.
     */
    default boolean canConnectPneumatic(Direction dir) {
        return true;
    }

    /** As {@link #canConnectPneumatic(Direction)}, for a block whose open sides are in its state. */
    default boolean canConnectPneumatic(BlockState state, Direction dir) {
        return canConnectPneumatic(dir);
    }
}
