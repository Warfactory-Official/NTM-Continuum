package com.hbm.blocks.machine.rbmk;

import com.hbm.blocks.ModHazardBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class RBMKDebrisDigamma extends RBMKDebrisBlock {
    public RBMKDebrisDigamma(BlockBehaviour.Properties props) { super(props); }
    private static boolean corruptible(net.minecraft.world.level.block.Block block) { return block == ModRBMKBlocks.rbmk_debris.get() || block == ModRBMKBlocks.rbmk_debris_burning.get() || block == ModRBMKBlocks.rbmk_debris_radiating.get() || block == ModHazardBlocks.block_corium.get(); }
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) { if (level instanceof ServerLevel server && !oldState.is(this)) server.scheduleTick(pos, this, 2); }
    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbour = pos.relative(dir);
            if (corruptible(level.getBlockState(neighbour).getBlock())) level.setBlock(neighbour, defaultBlockState(), 3);
        }
    }
}
