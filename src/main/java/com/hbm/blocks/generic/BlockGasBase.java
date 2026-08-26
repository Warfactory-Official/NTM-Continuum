package com.hbm.blocks.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/** Shared invisible-cell behaviour for the old {@code BlockGasBase} family. */
public abstract class BlockGasBase extends dev.golem.core.world.CompatBlock {
    protected BlockGasBase(Properties properties) {
        super(properties);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    protected static Direction randomHorizontal(RandomSource random) {
        return Direction.Plane.HORIZONTAL.getRandomDirection(random);
    }

    /** Old gas drift moved only into literal air, not into another gas cell. */
    protected static boolean tryMove(ServerLevel level, BlockState state, BlockPos pos, Direction direction) {
        BlockPos target = pos.relative(direction);
        if (!level.isLoaded(target) || !level.getBlockState(target).is(Blocks.AIR)) return false;
        level.removeBlock(pos, false);
        level.setBlock(target, state, 2);
        return true;
    }

    /** Vanilla no longer exposes the old block-level isAir override; keep its outgas meaning locally. */
    protected static boolean isAirLike(BlockState state) {
        return state.isAir() || state.getBlock() instanceof BlockGasBase;
    }

    protected static void drift(ServerLevel level, BlockState state, BlockPos pos, RandomSource random,
                                Direction first, Direction second) {
        if (random.nextInt(2) == 0 && !tryMove(level, state, pos, first)) {
            tryMove(level, state, pos, second);
        }
    }
}
