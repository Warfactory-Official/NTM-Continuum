package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IBlockMutator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Leaves fire in a third of the positions the blast cleared, if the block below can hold it. */
public class BlockMutatorFire implements IBlockMutator {

    @Override
    public void mutatePre(ExplosionVNT explosion, BlockState state, BlockPos pos) {}

    @Override
    public void mutatePost(ExplosionVNT explosion, BlockPos pos) {
        BlockState here = explosion.world.getBlockState(pos);
        BlockState below = explosion.world.getBlockState(pos.below());

        if (here.isAir() && dev.golem.core.world.Compat.isSolidRender(below, explosion.world, pos.below())
                && explosion.world.getRandom().nextInt(3) == 0) {
            explosion.world.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
        }
    }
}
