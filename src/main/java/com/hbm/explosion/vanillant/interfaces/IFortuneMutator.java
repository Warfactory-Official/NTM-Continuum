package com.hbm.explosion.vanillant.interfaces;

import com.hbm.explosion.vanillant.ExplosionVNT;
import net.minecraft.world.level.block.Block;

/** Grants a fortune level to the blocks an explosion drops. */
public interface IFortuneMutator {

    int mutateFortune(ExplosionVNT explosion, Block block, int x, int y, int z);
}
