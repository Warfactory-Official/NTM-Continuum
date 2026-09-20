package com.hbm.blocks.generic;

import com.hbm.saveddata.TomSaveData;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code BlockDirt} with random ticks (impact_dirt, "Scorched Dirt"): the soil Tom's impact leaves
 * behind. Drops vanilla dirt; a grass block anywhere in the 3x3x3 around it turns it back to dirt on
 * a neighbour update; on a random tick it regrows grass once the light above it is at least 9 through
 * the impact dust ({@code blockLight * (1 - dust)} against sky light) and the impact fire is over.
 */
public class BlockImpactDirt extends CompatBlock {

    public BlockImpactDirt(Properties properties) {
        super(properties);
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock, boolean movedByPiston) {
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                for (int k = -1; k < 2; k++) {
                    if (level.getBlockState(pos.offset(i, j, k)).getBlock() instanceof GrassBlock) {
                        level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 3);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        TomSaveData data = TomSaveData.get(level);
        BlockPos above = pos.above();
        int light = Math.max(level.getBrightness(LightLayer.BLOCK, above),
                (int) (level.getMaxLocalRawBrightness(above) * (1 - data.dust)));
        if (light >= 9 && data.fire == 0) {
            level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        }
    }
}
