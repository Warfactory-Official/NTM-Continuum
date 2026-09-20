package com.hbm.blocks.generic;

import com.hbm.blocks.ModNuclearBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code BlockNuclearWaste} ({@code block_waste}, {@code block_waste_painted}, {@code block_waste_vitrified}):
 * a {@code BlockHazard} with the RADFOG display that, on every random tick, has a 50% chance of venting a
 * dense radon cell into a random adjacent air block before the usual radiation emission.
 */
public class BlockNuclearWaste extends BlockHazard {

    public BlockNuclearWaste(Properties properties) {
        super(properties, DisplayEffect.RADFOG);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        Direction dir = Direction.values()[rand.nextInt(6)];
        BlockPos at = pos.relative(dir);
        if (rand.nextInt(2) == 0 && level.getBlockState(at).isAir()) {
            level.setBlockAndUpdate(at, ModNuclearBlocks.gas_radon_dense.get().defaultBlockState());
        }
        super.randomTick(state, level, pos, rand);
    }
}
