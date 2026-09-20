package com.hbm.blocks.fluid;

import com.hbm.lib.ModDamageSources;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The 1.7 {@code AcidBlock} ({@code acid_block}): anything inside is webbed and takes 10,000 acid
 * damage; on its 5-tick placement/neighbour tick, and on every neighbour change, every neighbouring
 * block that is not acid is turned to air. That includes anything at all — it is what the original does, and it is why the acid
 * bucket is a creative-only curiosity.
 */
public class BlockAcid extends BlockStaticFluid {

    public BlockAcid(Properties properties) {
        super(properties);
    }

    @Override
    protected void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        setInWeb(entity, state);
        if (!level.isClientSide()) entity.hurt(ModDamageSources.source(level, ModDamageSources.ACID), 10000F);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) level.scheduleTick(pos, this, 5);
    }

    /**
     * 1.7's tickRate(5) only mattered while the fluid was still flowing (BlockFluidClassic re-scheduled
     * itself until it came to rest), so a resting block ticks once when added and once whenever a
     * neighbour changes — the same schedule {@link BlockCorium} keeps; there was no perpetual self-loop.
     */
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        react(level, pos);
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock, boolean movedByPiston) {
        super.onNeighborChanged(state, level, pos, neighbourBlock, movedByPiston);
        if (!level.isClientSide()) {
            react(level, pos);
            level.scheduleTick(pos, this, 5);
        }
    }

    private void react(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos at = pos.relative(dir);
            if (!level.getBlockState(at).is(this) && !level.getBlockState(at).isAir()) {
                level.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }
}
