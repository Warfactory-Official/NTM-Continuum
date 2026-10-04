package com.hbm.world.feature;

import dev.golem.core.world.Compat;
import com.hbm.blocks.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The gneiss (schist) band — 1.7.10's {@code SchistStratum} (hard-coded threshold 5, factor 3, fold at 4,
 * y 30; 1.12.2 generalised it as {@code new SchistStratum(stone_gneiss, 0.01, 5, 8, 30)}).
 *
 * <p>A 2D Perlin field over the chunk decides, per column, how thick a slab of {@code stone_gneiss} to
 * lay down around y=30:
 * <pre>
 *   n = noise(x * 0.01, z * 0.01)
 *   if n &lt;= 5            -&gt; nothing here
 *   range = (int) ((n - 5) * 3)                     // 1.7 truncates BEFORE the fold
 *   if range &gt; 4          -&gt; range = 8 - range     // the band tapers again past its peak
 *   if range &lt; 0          -&gt; nothing               // range 0 is still one layer, at y 30
 *   fill y in [30 - range, 30 + range] where the block is a solid rock cube
 * </pre>
 *
 * <p><b>This must run before the gneiss ores</b> — the gneiss {@code hbm:legacy_ore} veins target
 * {@code stone_gneiss} exactly and find nothing if the band is not down yet. That ordering is why this
 * feature is injected at the {@code UNDERGROUND_DECORATION} step while the ores sit at
 * {@code UNDERGROUND_ORES}.
 *
 * <p>The noise is 1.7's {@code new NoiseGeneratorPerlin(new Random(worldSeed), 4)}: the world seed, unsalted
 * (1.12.2 had moved it to the decoration event's random). The only deviation is the missing +8 decoration
 * offset: the field is in world coordinates, so the band is the same, and laying each chunk's own columns
 * keeps it ahead of that chunk's gneiss ores. History §19ej-6.
 */
public class SchistStratumFeature extends Feature<NoneFeatureConfiguration> {

    private static final double SCALE = 0.01D;
    private static final double THRESHOLD = 5.0D;
    private static final int HEIGHT = 30;

    private LegacyPerlinNoise noise;
    private long noiseSeed;

    public SchistStratumFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private LegacyPerlinNoise noise(long worldSeed) {
        if (noise == null || noiseSeed != worldSeed) {
            noiseSeed = worldSeed;
            noise = new LegacyPerlinNoise(new WorldgenRandom(new LegacyRandomSource(worldSeed)), 4);
        }
        return noise;
    }

    /** 1.7 {@code SchistStratum}: the band's half-thickness for a noise value above 5, or negative for none. */
    public static int range17(double n) {
        int range = (int) ((n - THRESHOLD) * 3);
        if (range > 4) range = 8 - range;
        return range;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        int cx = context.origin().getX();
        int cz = context.origin().getZ();
        LegacyPerlinNoise perlin = noise(level.getSeed());

        BlockState gneiss = ModBlocks.stone_gneiss.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = level.getMinY() + 1;
        int maxY = level.getMaxY() - 1;

        for (int x = cx; x < cx + 16; x++) {
            for (int z = cz; z < cz + 16; z++) {
                double n = perlin.getValue(x * SCALE, z * SCALE);
                if (n <= THRESHOLD) continue;

                int r = range17(n);
                if (r < 0) continue;

                int yStart = Math.max(minY, HEIGHT - r);
                int yEnd = Math.min(maxY, HEIGHT + r);

                for (int y = yStart; y <= yEnd; y++) {
                    pos.set(x, y, z);
                    BlockState target = level.getBlockState(pos);
                    // old: target.isNormalCube() && material == ROCK
                    if (Compat.isSolidRender(target) && OreVeinPlacer.STONE.test(target)) {
                        level.setBlock(pos, gneiss, 2 | 16);
                    }
                }
            }
        }
        return true;
    }
}
