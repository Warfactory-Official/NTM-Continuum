package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IBlockMutator;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Leaves slag where the blast met a surface. It runs after the block is cleared and only fills a cell
 * that still touches something solid, so a crater gets a lining rather than a solid fill.
 */
public class BlockMutatorDebris implements IBlockMutator {

    protected Supplier<BlockState> target;

    public BlockMutatorDebris(BlockState target) {
        this(() -> target);
    }

    public BlockMutatorDebris(Supplier<BlockState> target) {
        this.target = target;
    }

    @Override
    public void mutatePre(ExplosionVNT explosion, BlockState blockState, BlockPos pos) {}

    @Override
    public void mutatePost(ExplosionVNT explosion, BlockPos pos) {
        Level world = explosion.world;
        BlockState targetState = target.get();

        for (Direction dir : Direction.values()) {
            BlockPos neighbour = pos.relative(dir);
            BlockState state = world.getBlockState(neighbour);
            if (dev.golem.core.world.Compat.isSolidRender(state, world, neighbour) && state != targetState) {
                world.setBlock(pos, targetState, 3);
                return;
            }
        }
    }
}
