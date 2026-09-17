package com.hbm.api.conveyor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// 1.7 api.hbm.conveyor.IConveyorBelt
public interface IConveyorBelt {

    /** Returns true if the item should stay on the conveyor, false if the item should drop off */
    boolean canItemStay(Level level, BlockPos pos, Vec3 itemPos);

    Vec3 getTravelLocation(Level level, BlockPos pos, Vec3 itemPos, double speed);

    Vec3 getClosestSnappingPosition(Level level, BlockPos pos, Vec3 itemPos);
}
