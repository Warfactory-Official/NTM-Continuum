package com.hbm.blocks.machine;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

// 1.7 ISpotlight: marks a beam source
public interface ISpotlight {

    int getBeamLength();

    /**
     * Whether this state drives a beam out of {@code dir}. A fixture answers for its own facing alone; the
     * tritium lamp answers for every face.
     */
    boolean lights(BlockState state, Direction dir);
}
