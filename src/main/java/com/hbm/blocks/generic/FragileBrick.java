package com.hbm.blocks.generic;

import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code FragileBrick}, the brittle enargite brick: it crumbles (no drop) the moment anything walks on it,
 * and every brittle brick touching one that crumbles is scheduled to crumble 8-11 ticks later, so a whole
 * floor of them collapses in a wave. The chain runs on scheduled ticks and on removal, as 1.7's {@code breakBlock}
 * notified its neighbours too. Drops nothing (the loot table is empty).
 */
public class FragileBrick extends CompatBlock {

    public FragileBrick(Properties properties) {
        super(properties);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // 1.7 func_147480_a(x, y, z, false): break with particles and sound, no drop
        level.destroyBlock(pos, false);
        notifyNeighbors(level, pos);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level.isClientSide()) return;
        level.destroyBlock(pos, false);
        notifyNeighbors(level, pos);
    }

    @Override
    protected void onBlockRemoved(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        if (!level.isClientSide()) notifyNeighbors(level, pos);
    }

    private void notifyNeighbors(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos n = pos.relative(dir);
            if (level.getBlockState(n).getBlock() == this) {
                level.scheduleTick(n, this, level.getRandom().nextInt(4) + 8);
            }
        }
    }
}
