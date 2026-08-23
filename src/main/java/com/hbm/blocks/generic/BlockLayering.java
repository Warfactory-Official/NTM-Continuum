package com.hbm.blocks.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;

/** The original foam layer stacks like snow but never melts from light. */
public class BlockLayering extends SnowLayerBlock {
    private final boolean alwaysReplaceable;

    public BlockLayering(Properties properties) {
        this(properties, false);
    }

    public BlockLayering(Properties properties, boolean alwaysReplaceable) {
        super(properties);
        this.alwaysReplaceable = alwaysReplaceable;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return alwaysReplaceable || (context.getItemInHand().is(asItem()) && super.canBeReplaced(state, context));
    }
}
