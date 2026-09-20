package com.hbm.blocks.fluid;

import com.hbm.blocks.ModHazardBlocks;
import com.hbm.blocks.ModOutgasBlocks;
import com.hbm.lib.ModDamageSources;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The 1.7 {@code CoriumFinite} ({@code corium_block}): webs, sets alight for 3 seconds, deals 2
 * radiation damage and 1 rad of contamination per contact tick. On a tick (1.7: every 30 ticks of
 * flow, plus random ticks) with nothing corium below it, one time in ten it solidifies — a third of
 * those into solid {@code block_corium}, the rest into {@code block_corium_cobble}.
 */
public class BlockCorium extends BlockStaticFluid {

    public BlockCorium(Properties properties) {
        super(properties);
    }

    @Override
    protected void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        setInWeb(entity, state);
        entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(), 3 * 20)); // setFire(3)
        if (level.isClientSide()) return;
        entity.hurt(ModDamageSources.source(level, ModDamageSources.RADIATION), 2F);
        if (entity instanceof LivingEntity living) {
            ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, 1F);
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) level.scheduleTick(pos, this, 30);
    }

    /**
     * 1.7's finite fluid only re-scheduled itself while it was still flowing, so a resting block ticks
     * once when added and once whenever a neighbour changes; there was no random tick and no self-loop.
     */
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) == 0 && !level.getBlockState(pos.below()).is(this)) {
            if (random.nextInt(3) == 0) level.setBlock(pos, ModHazardBlocks.block_corium.get().defaultBlockState(), 3);
            else level.setBlock(pos, ModOutgasBlocks.block_corium_cobble.get().defaultBlockState(), 3);
        }
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock, boolean movedByPiston) {
        super.onNeighborChanged(state, level, pos, neighbourBlock, movedByPiston);
        if (!level.isClientSide()) level.scheduleTick(pos, this, 30);
    }
}
