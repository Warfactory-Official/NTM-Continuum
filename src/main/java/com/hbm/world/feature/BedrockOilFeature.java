package com.hbm.world.feature;

import com.hbm.blocks.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Bedrock oil deposit — 1:1 port of the 1.12.2 {@code world/feature/BedrockOilDeposit} + its
 * {@code HbmWorldGen.generateBedrockOil} invocation. 1/200 per chunk (old {@code bedrockOilSpawn} default
 * {@code 0:200}): replaces BEDROCK inside the L1-diamond {@code |dx| + dy + |dz| <= 6} (dx,dz ∈ [-4,4],
 * dy ∈ [0,4]) with {@code ore_bedrock_oil}, then blights the surface via {@link OilSpot} (radius 5, rich).
 *
 * <p>Deviations (IMPLEMENTATION.md §20): rarity/gate lives in {@code place()} (empty placement); the
 * y-scan is offset to the world floor {@code minY..minY+4} (1.12 bedrock was at y0-4, now it's at minY);
 * the {@code stone_porous} ore scatter is skipped (block unported).
 */
public class BedrockOilFeature extends Feature<NoneFeatureConfiguration> {

    private static final int DXZ_LIMIT = 4;
    private static final int MAX_Y = 4;
    private static final int L1_MAX = 6;
    private static final int OIL_SPOT_RADIUS = 5;
    private static final int OIL_SPOT_HEIGHT = 50;

    public BedrockOilFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource rand = context.random();
        BlockPos chunkOrigin = context.origin();

        if (rand.nextInt(200) != 0) return false;

        int centerX = chunkOrigin.getX() + rand.nextInt(16);
        int centerZ = chunkOrigin.getZ() + rand.nextInt(16);
        int floor = level.getMinY();

        BlockState oil = ModBlocks.ore_bedrock_oil.get().defaultBlockState();
        MutableBlockPos pos = new MutableBlockPos();

        for (int dx = -DXZ_LIMIT; dx <= DXZ_LIMIT; dx++) {
            for (int dy = 0; dy <= MAX_Y; dy++) {
                for (int dz = -DXZ_LIMIT; dz <= DXZ_LIMIT; dz++) {
                    if (Math.abs(dx) + dy + Math.abs(dz) <= L1_MAX) {
                        pos.set(centerX + dx, floor + dy, centerZ + dz);
                        if (level.getBlockState(pos).is(Blocks.BEDROCK)) {
                            level.setBlock(pos, oil, 2 | 16);
                        }
                    }
                }
            }
        }

        // old: DungeonToolbox.generateOre(... stone_porous) — skipped (block unported) §20
        OilSpot.generateOilSpot(level, centerX, centerZ, OIL_SPOT_RADIUS, OIL_SPOT_HEIGHT, true);
        return true;
    }
}
