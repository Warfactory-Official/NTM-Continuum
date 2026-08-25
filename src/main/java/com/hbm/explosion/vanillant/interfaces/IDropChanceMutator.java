package com.hbm.explosion.vanillant.interfaces;

import com.hbm.explosion.vanillant.ExplosionVNT;
import net.minecraft.world.level.block.Block;

/** Rewrites the per-block drop chance (vanilla's {@code 1 / size}). */
public interface IDropChanceMutator {

    float mutateDropChance(ExplosionVNT explosion, Block block, int x, int y, int z, float dropChance);
}
