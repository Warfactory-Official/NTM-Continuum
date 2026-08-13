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
 * The gneiss (schist) band — 1.12.2's {@code SchistStratum}, registered in {@code MainRegistry} as
 * {@code new SchistStratum(stone_gneiss, 0.01, 5, 8, 30)}.
 *
 * <p>A 2D Perlin field over the chunk decides, per column, how thick a slab of {@code stone_gneiss} to
 * lay down around y=30:
 * <pre>
 *   n = noise(x * 0.01, z * 0.01)
 *   if n &lt;= 5            -&gt; nothing here
 *   range = (n - 5) * (8 * 0.5 - 1)
 *   if range &gt; 8 * 0.5    -&gt; range = 8 - range     // the band tapers again past its peak
 *   if range &lt;= 0         -&gt; nothing
 *   fill y in [30 - range, 30 + range] where the block is a solid rock cube
 * </pre>
 *
 * <p><b>This must run before the gneiss ores</b> — {@link OverworldOreFeature}'s gneiss veins target
 * {@code stone_gneiss} exactly and find nothing if the band is not down yet. That ordering is why this
 * feature is injected at the {@code UNDERGROUND_DECORATION} step while the ores sit at
 * {@code UNDERGROUND_ORES}.
 *
 * <p><b>Deliberate fix vs upstream:</b> the original seeded its noise from the decoration event's
 * {@code Random}, so the band was not reproducible for a given world seed and shifted between
 * regenerations. Here it is seeded from the world seed, which is what the original clearly intended.
 */
public class SchistStratumFeature extends Feature<NoneFeatureConfiguration> {

    private static final double SCALE = 0.01D;
    private static final double THRESHOLD = 5.0D;
    private static final double THICKNESS = 8.0D;
    private static final int HEIGHT = 30;
    /** Distinguishes this noise field from the other HBM noise features sharing the world seed. */
    private static final long SEED_SALT = 0x5C4157L;

    private LegacyPerlinNoise noise;
    private long noiseSeed;

    public SchistStratumFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private LegacyPerlinNoise noise(long worldSeed) {
        if (noise == null || noiseSeed != worldSeed) {
            noiseSeed = worldSeed;
            noise = new LegacyPerlinNoise(new WorldgenRandom(new LegacyRandomSource(worldSeed + SEED_SALT)), 4);
        }
        return noise;
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

                double range = (n - THRESHOLD) * (THICKNESS * 0.5D - 1.0D);
                if (range > THICKNESS * 0.5D) range = THICKNESS - range;
                if (range <= 0) continue;

                int r = (int) range;
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
