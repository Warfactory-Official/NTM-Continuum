package com.hbm.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Something an emitter's beam can pour energy into rather than burn through. */
public interface ILaserable {

    void addEnergy(Level level, BlockPos pos, long energy, Direction dir);
}
