package com.hbm.world.feature;

import com.hbm.blocks.generic.BlockReeds;
import com.hbm.saveddata.TomSaveData;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;

/**
 * {@code hbm:legacy_flowers}: 1.7 {@code DungeonToolbox.generateFlowers}, i.e. vanilla-1.7 {@code WorldGenFlowers}.
 * 64 tries around the placed position, each {@code x + rand(8) - rand(8)}, {@code y + rand(4) - rand(4)},
 * {@code z + rand(8) - rand(8)}; a try places {@code state} when the spot is air and the plant can stay there.
 * The chunk gate, the column and {@code y = getHeightValue} are the placed feature's {@code rarity_filter} +
 * {@code in_square} + {@code heightmap}.
 *
 * <p>Two things keep this a feature of its own rather than vanilla's patch: 26.1 dissolved {@code random_patch} into
 * placement modifiers (the patch would be spelled differently per version), and {@code plant_reeds} carries the depth
 * of the water column below it in its state, which a plain block placer cannot compute.
 *
 * <p>1.7 generated none of these after a Tom impact ({@code TomSaveData.impact}); neither does this.
 */
public class LegacyFlowerFeature extends Feature<BlockStateConfiguration> {

    public LegacyFlowerFeature(Codec<BlockStateConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<BlockStateConfiguration> context) {
        WorldGenLevel level = context.level();
        if (TomSaveData.impact(level.getLevel())) return false;
        RandomSource rand = context.random();
        BlockPos origin = context.origin();
        BlockState state = context.config().state;
        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int l = 0; l < 64; l++) {
            pos.set(origin.getX() + rand.nextInt(8) - rand.nextInt(8),
                    origin.getY() + rand.nextInt(4) - rand.nextInt(4),
                    origin.getZ() + rand.nextInt(8) - rand.nextInt(8));
            if (pos.getY() < level.getMinY() || pos.getY() >= level.getMaxY()) continue;
            if (!level.isEmptyBlock(pos) || !state.canSurvive(level, pos)) continue;
            BlockState put = state;
            if (state.getBlock() instanceof BlockReeds) put = state.setValue(BlockReeds.DEPTH, BlockReeds.depth(level, pos));
            level.setBlock(pos, put, 2);
            placed = true;
        }
        return placed;
    }
}
