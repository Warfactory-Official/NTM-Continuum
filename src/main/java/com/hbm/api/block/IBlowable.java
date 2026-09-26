package com.hbm.api.block;

import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** A block that receives the legacy fan callback when it is the first obstruction in the air stream. */
public interface IBlowable {

    /** Called server-side once per fan tick while this block is in the fan's line of effect. */
    void applyFan(Level level, BlockPos pos, Direction direction, int distance);
}
