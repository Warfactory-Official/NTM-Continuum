package com.hbm.explosion.vanillant.interfaces;

import com.hbm.explosion.vanillant.ExplosionVNT;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Converts blocks the explosion touched. {@code mutatePre} sees the block before it is broken,
 * {@code mutatePost} sees the position after: that ordering is what lets the fire mutator only light
 * positions the blast actually cleared.
 */
public interface IBlockMutator {

    void mutatePre(ExplosionVNT explosion, BlockState state, BlockPos pos);

    void mutatePost(ExplosionVNT explosion, BlockPos pos);
}
