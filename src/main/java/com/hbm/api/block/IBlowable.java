package com.hbm.api.block;

import com.hbm.lib.ForgeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** A block that receives the legacy fan callback when it is the first obstruction in the air stream. */
public interface IBlowable {

    /** Called server-side once per fan tick while this block is in the fan's line of effect. */
    void applyFan(Level level, BlockPos pos, ForgeDirection direction, int distance);
}
