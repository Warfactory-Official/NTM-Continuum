package com.hbm.blocks.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.blocks.machine.MachineZirnoxDestroyed;
import com.hbm.blocks.machine.rbmk.RBMKDebrisBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import java.util.List;

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
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (belowState.is(Blocks.ICE) || belowState.is(Blocks.PACKED_ICE)) return false;
        if (belowState.getBlock() instanceof RBMKDebrisBlock
                || belowState.getBlock() instanceof MachineZirnoxDestroyed) return true;
        return belowState.is(BlockTags.LEAVES) || belowState.isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        // The legacy metadata block dropped one item per visible layer, not one stack per block.
        return List.of(new ItemStack(this, state.getValue(SnowLayerBlock.LAYERS)));
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return alwaysReplaceable || (context.getItemInHand().is(asItem()) && super.canBeReplaced(state, context));
    }
}
