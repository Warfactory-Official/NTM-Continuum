package com.hbm.world.feature;

import dev.golem.core.world.Compat;
import com.hbm.blocks.ModOreBlocks;
import com.hbm.config.WorldConfig;
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
 * The 3D noise ore layers — 1.12.2's {@code OreLayer3D}, registered three times in
 * {@code MainRegistry} in this exact order:
 * <pre>
 *   id 0  hematite   scaleH 0.04  scaleV 0.25  threshold 230
 *   id 1  bauxite    scaleH 0.03  scaleV 0.15  threshold 300
 *   id 2  malachite  scaleH 0.10  scaleV 0.15  threshold 275
 * </pre>
 *
 * <p>Three independent Perlin fields are sampled per cell and <b>multiplied</b>; the cell becomes the
 * resource stone when {@code nX · nY · nZ > threshold}. Because it is a triple product of unnormalised
 * noise, the thresholds are in the hundreds — see {@link LegacyPerlinNoise} for why that matters.
 *
 * <pre>
 *   nX = noiseX(y · scaleV, z · scaleH)
 *   nZ = noiseZ(x · scaleH, y · scaleV)
 *   nY = noiseY(x · scaleH, z · scaleH)
 * </pre>
 *
 * <p>The layers span y 6–64 over the chunk's +8 decoration offset, and only replace solid rock that
 * passes the standard stone predicate.
 *
 * <p><b>Deliberate fix vs upstream:</b> the noise seed offset was {@code seed + 101/102/103 + id},
 * where {@code id} came from a static counter incremented at registration. Reordering the three
 * registrations therefore silently changed everyone's world. Each layer now carries its explicit
 * upstream id (0/1/2) so generation is order-independent while producing the same fields.
 */
public class OreLayer3DFeature extends Feature<NoneFeatureConfiguration> {

    private static final int MIN_Y = 6;
    private static final int MAX_Y = 64;

    /** One configured layer. {@code id} is the upstream static-counter value, pinned. */
    private record Layer(int id, double scaleH, double scaleV, double threshold) {}

    private static final Layer HEMATITE = new Layer(0, 0.04D, 0.25D, 230D);
    private static final Layer BAUXITE = new Layer(1, 0.03D, 0.15D, 300D);
    private static final Layer MALACHITE = new Layer(2, 0.10D, 0.15D, 275D);

    private record Noises(LegacyPerlinNoise x, LegacyPerlinNoise y, LegacyPerlinNoise z) {}

    private Noises hematiteNoise, bauxiteNoise, malachiteNoise;
    private long noiseSeed = Long.MIN_VALUE;

    public OreLayer3DFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static Noises noisesFor(long seed, Layer layer) {
        return new Noises(
                new LegacyPerlinNoise(new WorldgenRandom(new LegacyRandomSource(seed + 101L + layer.id())), 4),
                new LegacyPerlinNoise(new WorldgenRandom(new LegacyRandomSource(seed + 102L + layer.id())), 4),
                new LegacyPerlinNoise(new WorldgenRandom(new LegacyRandomSource(seed + 103L + layer.id())), 4));
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        long seed = level.getSeed();
        if (hematiteNoise == null || noiseSeed != seed) {
            noiseSeed = seed;
            hematiteNoise = noisesFor(seed, HEMATITE);
            bauxiteNoise = noisesFor(seed, BAUXITE);
            malachiteNoise = noisesFor(seed, MALACHITE);
        }

        int cx = context.origin().getX();
        int cz = context.origin().getZ();

        if (WorldConfig.enableHematite) {
            generate(level, cx, cz, HEMATITE, hematiteNoise, ModOreBlocks.stone_resource_hematite.get().defaultBlockState());
        }
        if (WorldConfig.enableBauxite) {
            generate(level, cx, cz, BAUXITE, bauxiteNoise, ModOreBlocks.stone_resource_bauxite.get().defaultBlockState());
        }
        if (WorldConfig.enableMalachite) {
            generate(level, cx, cz, MALACHITE, malachiteNoise, ModOreBlocks.stone_resource_malachite.get().defaultBlockState());
        }
        return true;
    }

    private static void generate(WorldGenLevel level, int cx, int cz, Layer layer, Noises noise, BlockState ore) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int startX = cx + 8;   // the old +8 decoration offset
        int startZ = cz + 8;
        int floor = level.getMinY();
        int ceiling = level.getMaxY();

        for (int xOff = 0; xOff < 16; xOff++) {
            int x = startX + xOff;
            for (int zOff = 0; zOff < 16; zOff++) {
                int z = startZ + zOff;
                if (!level.hasChunk(x >> 4, z >> 4)) continue;
                double nY = noise.y().getValue(x * layer.scaleH(), z * layer.scaleH());

                for (int y = MAX_Y; y >= MIN_Y; y--) {
                    if (y < floor || y > ceiling) continue;
                    double nX = noise.x().getValue(y * layer.scaleV(), z * layer.scaleH());
                    double nZ = noise.z().getValue(x * layer.scaleH(), y * layer.scaleV());
                    if (nX * nY * nZ <= layer.threshold()) continue;

                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (Compat.isSolidRender(state) && OreVeinPlacer.STONE.test(state)) {
                        level.setBlock(pos, ore, 2 | 16);
                    }
                }
            }
        }
    }
}
