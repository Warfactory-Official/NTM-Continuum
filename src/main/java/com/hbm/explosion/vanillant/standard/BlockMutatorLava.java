package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IBlockMutator;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code ExplosionNT} with {@code ExAttrib.LAVA_V} / {@code LAVA_R}: every destroyed block that was a
 * normal cube ({@code isNormalCube()}) is refilled with the volcano's lava; everything else the blast hit
 * is left as the air {@code onBlockExploded} made of it. Both attributes sit outside the fire/balefire
 * scenery pass, so there is no {@code mutatePost} half. Same shape as ntm-next's {@code BlockMutatorLava}.
 */
public class BlockMutatorLava implements IBlockMutator {

    private final Block lava;

    public BlockMutatorLava(Block lava) {
        this.lava = lava;
    }

    @Override
    public void mutatePre(ExplosionVNT explosion, BlockState state, BlockPos pos) {
        // 1.7 gates the write on the pre-blast block's isNormalCube(); the solid-render test is its modern spelling.
        if (Compat.isSolidRender(state, explosion.world, pos)) {
            explosion.world.setBlock(pos, lava.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public void mutatePost(ExplosionVNT explosion, BlockPos pos) {}
}
