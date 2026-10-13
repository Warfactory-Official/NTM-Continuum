package com.hbm.blocks.fluid;

import com.hbm.lib.ModDamageSources;
import dev.golem.core.fluid.FluidBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The 1.7 {@code AcidBlock} ({@code acid_block}), a flowing {@code BlockFluidClassic} of quanta 4 (three blocks out)
 * on a 5-tick flow: anything inside is webbed and takes 10,000 acid damage; after every flow update, and on every
 * neighbour change, every neighbouring block that is not acid is turned to air. That includes anything at all,
 * the floor too, so acid bores straight down: it is what the original does, and it is why the acid bucket is a
 * creative-only curiosity. It never displaces another liquid.
 */
public class BlockAcid extends FluidBlock {

    public BlockAcid(Properties properties) {
        super(properties, ModWorldFluids.ACID);
    }

    @Override
    protected void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        ModWorldFluids.setInWeb(entity, state);
        if (!level.isClientSide()) entity.hurt(ModDamageSources.source(level, ModDamageSources.ACID), 10000F);
    }

    /** 1.7 {@code updateTick}: {@code super.updateTick}, then {@code reactToBlocks} on all six sides. */
    @Override
    protected void afterFlow(ServerLevel level, BlockPos pos, RandomSource random) {
        react(level, pos);
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock, boolean movedByPiston) {
        super.onNeighborChanged(state, level, pos, neighbourBlock, movedByPiston);
        if (!level.isClientSide()) react(level, pos);
    }

    private void react(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos at = pos.relative(dir);
            BlockState there = level.getBlockState(at);
            if (!there.is(this) && !there.isAir()) level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    @Override
    protected boolean canDisplace(BlockGetter level, BlockPos pos) {
        return !isLiquid(level.getBlockState(pos)) && super.canDisplace(level, pos);
    }

    @Override
    protected boolean displaceIfPossible(Level level, BlockPos pos) {
        return !isLiquid(level.getBlockState(pos)) && super.displaceIfPossible(level, pos);
    }
}
