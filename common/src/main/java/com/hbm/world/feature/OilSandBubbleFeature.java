package com.hbm.world.feature;

import com.hbm.blocks.ModFallingBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Tar-sand bubble — 1:1 port of the 1.12.2 {@code world/OilSandBubble} + its {@code HbmWorldGen} L661-668
 * invocation. In hot-dry biomes ({@code !hasPrecipitation() && baseTemperature >= 1.8}), 1/600 per chunk:
 * a random in-chunk surface origin, radius {@code 15 + rand(31)}; the ellipsoid {@code xx² + 3·yy² + zz² <
 * r²/2 + rand(r²/6)} replacing SAND with {@code ore_oil_sand}.
 *
 * <p>Deviations (IMPLEMENTATION.md §20): rarity/biome gate lives in {@code place()} (one placed feature,
 * empty placement), like {@link OilBubbleFeature}; writes outside the origin-chunk ±1 region are skipped
 * (radius up to 45 can exceed it); the origin y is the WORLD_SURFACE_WG heightmap (old {@code getHeight}).
 */
public class OilSandBubbleFeature extends Feature<NoneFeatureConfiguration> {

    public OilSandBubbleFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource rand = context.random();
        BlockPos chunkOrigin = context.origin();

        Biome biome = level.getBiome(chunkOrigin).value();
        if (biome.hasPrecipitation() || biome.getBaseTemperature() < 1.8F) return false;
        if (rand.nextInt(600) != 0) return false;

        int x = chunkOrigin.getX() + rand.nextInt(16);
        int z = chunkOrigin.getZ() + rand.nextInt(16);
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        int radius = 15 + rand.nextInt(31);

        spawnOil(level, rand, chunkOrigin, x, y, z, radius);
        return true;
    }

    /** Old OilSandBubble.spawnOil: ellipsoid XX + yy²·3 + zz² < r²/2 + rand(r²/6), SAND → ore_oil_sand. */
    private static void spawnOil(WorldGenLevel level, RandomSource rand, BlockPos chunkOrigin, int x, int y, int z, int radius) {
        int r2 = radius * radius;
        int r22 = r2 / 2;
        BlockState oilSand = ModFallingBlocks.ore_oil_sand.get().defaultBlockState();
        MutableBlockPos pos = new MutableBlockPos();

        // Features may only write to the origin chunk ± 1 chunk; large radii exceed that.
        int minX = chunkOrigin.getX() - 16, maxX = chunkOrigin.getX() + 31;
        int minZ = chunkOrigin.getZ() - 16, maxZ = chunkOrigin.getZ() + 31;

        for (int xx = -radius; xx < radius; xx++) {
            int X = xx + x;
            int XX = xx * xx;
            for (int yy = -radius; yy < radius; yy++) {
                int Y = yy + y;
                int YY = XX + yy * yy * 3;
                for (int zz = -radius; zz < radius; zz++) {
                    int Z = zz + z;
                    if (YY + zz * zz < r22 + rand.nextInt(r22 / 3)) {
                        if (X < minX || X > maxX || Z < minZ || Z > maxZ) continue;
                        pos.set(X, Y, Z);
                        if (level.getBlockState(pos).is(Blocks.SAND)) {
                            level.setBlock(pos, oilSand, 2 | 16);
                        }
                    }
                }
            }
        }
    }
}
