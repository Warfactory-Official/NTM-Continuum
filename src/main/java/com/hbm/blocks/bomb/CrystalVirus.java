package com.hbm.blocks.bomb;

import com.hbm.blocks.ModBlocks;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.config.TerrainConfig;

/**
 * 1.7 {@code CrystalVirus} ({@code crystal_virus}, "Dark Crystal"): with the virus config on, every random
 * tick it converts each non-air, non-crystal neighbour into more virus and itself into hardened crystal;
 * a virus block whose six neighbours are all air or crystal hardens on the next neighbour change.
 * Hardness 15, infinite blast resistance, no creative item.
 */
public class CrystalVirus extends CompatBlock {

    public CrystalVirus(Properties properties) {
        super(properties);
    }

    /** air, virus, hardened crystal or pulsar — the blocks the virus leaves alone. */
    public static boolean isInert(BlockState state) {
        Block b = state.getBlock();
        return state.isAir() || b instanceof CrystalVirus || b == ModBlocks.crystal_hardened.get() || b instanceof CrystalPulsar;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!TerrainConfig.enableVirus) return;
        for (Direction dir : Direction.values()) {
            BlockPos at = pos.relative(dir);
            if (!isInert(level.getBlockState(at))) level.setBlockAndUpdate(at, defaultBlockState());
        }
        level.setBlockAndUpdate(pos, ModBlocks.crystal_hardened.get().defaultBlockState());
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock, boolean movedByPiston) {
        if (level.isClientSide()) return;
        for (Direction dir : Direction.values()) {
            if (!isInert(level.getBlockState(pos.relative(dir)))) return;
        }
        level.setBlockAndUpdate(pos, ModBlocks.crystal_hardened.get().defaultBlockState());
    }
}
