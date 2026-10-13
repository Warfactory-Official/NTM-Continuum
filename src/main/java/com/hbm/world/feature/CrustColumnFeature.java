package com.hbm.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;

/**
 * {@code hbm:crust_column}: the body of 1.7 {@code HbmWorldGen.generateNether}'s two surface loops, for a column
 * and a depth {@code d}, every netherrack block in {@code d-5 .. d} with air directly above becomes {@code state}.
 * 1.7 ran it 30 times per chunk with {@code ore_nether_smoldering} and once with {@code geysir_nether}; the column is
 * the placed feature's {@code in_square} and {@code d = 16 + rand.nextInt(96)} its uniform {@code height_range}
 * 16..111, the repetition its {@code count}.
 */
public class CrustColumnFeature extends Feature<BlockStateConfiguration> {

    public CrustColumnFeature(Codec<BlockStateConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<BlockStateConfiguration> context) {
        return scan(context.level(), context.origin(), context.config().state) > 0;
    }

    /** One 1.7 column pass at {@code top} = (x, d, z); returns how many blocks it converted. */
    public static int scan(WorldGenLevel level, BlockPos top, BlockState state) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int placed = 0;
        for (int y = top.getY() - 5; y <= top.getY(); y++) {
            if (y < level.getMinY() || y >= level.getMaxY()) continue;
            pos.set(top.getX(), y, top.getZ());
            if (!level.getBlockState(pos).is(Blocks.NETHERRACK)) continue;
            if (!level.getBlockState(pos.above()).isAir()) continue;
            level.setBlock(pos, state, 2 | 16);
            placed++;
        }
        return placed;
    }
}
