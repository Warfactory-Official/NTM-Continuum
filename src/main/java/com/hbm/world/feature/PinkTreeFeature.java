package com.hbm.world.feature;

import com.hbm.blocks.ModDecoCrateBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * {@code hbm:pink_tree}: 1.7 {@code HbmWorldGen}'s pink tree, one chunk in 1000. In a random column, every upright oak
 * log (1.7 {@code Blocks.log} metadata 0: oak, vertical) from the bottom of the world to the top becomes
 * {@code pink_log}. Side-ways branch logs are left alone, as they were in 1.7.
 *
 * <p>It must see the trees, so it is added after vanilla's vegetation (see {@code ModBiomeModifiers}); 1.7 ran it
 * after the biome decorator for the same reason.
 */
public class PinkTreeFeature extends Feature<NoneFeatureConfiguration> {

    public PinkTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        BlockState pink = ModDecoCrateBlocks.pink_log.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean done = false;
        for (int y = level.getMinY(); y < level.getMaxY(); y++) {
            pos.set(origin.getX(), y, origin.getZ());
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.OAK_LOG) && state.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y) {
                level.setBlock(pos, pink, 2);
                done = true;
            }
        }
        return done;
    }
}
