package com.hbm.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** A fluid receiver with a defined failure mode when an RBMK ruptures its pipe network. */
public interface IOverpressurable {

    void explode(Level level, BlockPos pos);
}
