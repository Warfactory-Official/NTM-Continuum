package com.hbm.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** The fire-extinguisher callback shared by damaged HBM machine cores. */
public interface IRepairable {
    void tryExtinguish(Level level, BlockPos pos, EnumExtinguishType type);

    enum EnumExtinguishType {
        WATER,
        FOAM,
        SAND,
        CO2
    }
}
