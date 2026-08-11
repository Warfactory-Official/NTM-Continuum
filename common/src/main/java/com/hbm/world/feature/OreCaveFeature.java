package com.hbm.world.feature;

import com.hbm.blocks.ModOreBlocks;
import com.hbm.config.WorldConfig;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluids;

/**
 * Cave-wall resource strata — 1.12.2's {@code OreCave}, registered twice in {@code MainRegistry}:
 * <pre>
 *   sulfur   : threshold 1.5,  rangeMult 20, yLevel 30, maxRange 20, fluid = sulfuric_acid_block
 *   asbestos : threshold 1.75, rangeMult 20, yLevel 25, maxRange 20, no fluid
 * </pre>
 *
 * <p>A 2D Perlin field picks the stratum's half-thickness per column
 * ({@code range = (n − threshold) · rangeMult}, folded back down past {@code maxRange}), then every
 * solid rock block in {@code [yLevel − range, yLevel + range]} that <b>touches air</b> becomes the
 * resource stone. Cells in that band that are <i>not</i> solid get a 1-in-5 chance of a stalactite or
 * stalagmite, whichever can anchor — this is where the cave spikes come from.
 *
 * <p><b>Deferred:</b> the sulfur variant's acid pool. Upstream, a cell with air above, solid below and
 * solid/acid on all four sides became {@code sulfuric_acid_block} with ore beneath and around it; that
 * block is not ported, so the sulfur cave currently takes the same dry path as asbestos. The pool
 * branch is marked below and is a drop-in once the acid block lands (IMPLEMENTATION.md §20).
 *
 * <p><b>Deviation:</b> the noise seed. Upstream salted the world seed with
 * {@code Block.getIdFromBlock(...) * 31 + yLevel} — a numeric block id that no longer exists in 26.1.
 * Each variant now carries an explicit stable salt instead, so the field is still per-variant distinct
 * and seed-reproducible.
 */
public class OreCaveFeature extends Feature<NoneFeatureConfiguration> {

    /** One configured cave stratum. */
    private record Cave(double threshold, int rangeMult, int maxRange, int yLevel, long salt) {}

    private static final Cave SULFUR = new Cave(1.5D, 20, 20, 30, 0x5FL * 31 + 30);
    private static final Cave ASBESTOS = new Cave(1.75D, 20, 20, 25, 0xA5BL * 31 + 25);

    private static final double SCALE = 0.01D;

    private LegacyPerlinNoise sulfurNoise, asbestosNoise;
    private long noiseSeed = Long.MIN_VALUE;

    public OreCaveFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource rand = context.random();
        long seed = level.getSeed();

        if (sulfurNoise == null || noiseSeed != seed) {
            noiseSeed = seed;
            // 2 octaves, matching the old NoiseGeneratorPerlin(rand, 2)
            sulfurNoise = new LegacyPerlinNoise(new WorldgenRandom(new LegacyRandomSource(seed + SULFUR.salt())), 2);
            asbestosNoise = new LegacyPerlinNoise(new WorldgenRandom(new LegacyRandomSource(seed + ASBESTOS.salt())), 2);
        }

        int cx = context.origin().getX();
        int cz = context.origin().getZ();

        if (WorldConfig.enableSulfurCave) {
            generate(level, rand, cx, cz, sulfurNoise, SULFUR,
                    ModOreBlocks.stone_resource_sulfur.get().defaultBlockState(),
                    ModOreBlocks.stalactite_sulfur.get().defaultBlockState(),
                    ModOreBlocks.stalagmite_sulfur.get().defaultBlockState());
        }
        if (WorldConfig.enableAsbestosCave) {
            generate(level, rand, cx, cz, asbestosNoise, ASBESTOS,
                    ModOreBlocks.stone_resource_asbestos.get().defaultBlockState(),
                    ModOreBlocks.stalactite_asbestos.get().defaultBlockState(),
                    ModOreBlocks.stalagmite_asbestos.get().defaultBlockState());
        }
        return true;
    }

    private static void generate(WorldGenLevel level, RandomSource rand, int cx, int cz, LegacyPerlinNoise noise,
                                 Cave cave, BlockState ore, BlockState stalactite, BlockState stalagmite) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int floor = level.getMinY();
        int ceiling = level.getMaxY();

        // The old +8 decoration offset on both axes, kept verbatim.
        for (int x = cx + 8; x < cx + 24; x++) {
            for (int z = cz + 8; z < cz + 24; z++) {
                double n = noise.getValue(x * SCALE, z * SCALE);
                if (n <= cave.threshold()) continue;

                int range = (int) ((n - cave.threshold()) * cave.rangeMult());
                if (range > cave.maxRange()) range = cave.maxRange() * 2 - range;
                if (range < 0) continue;

                for (int y = cave.yLevel() - range; y <= cave.yLevel() + range; y++) {
                    if (y < floor || y > ceiling) continue;
                    pos.set(x, y, z);
                    if (!level.hasChunk(x >> 4, z >> 4)) continue;

                    BlockState state = level.getBlockState(pos);
                    boolean solidRock = state.isSolidRender() && OreVeinPlacer.STONE.test(state);

                    if (solidRock) {
                        // TODO(acid pool): upstream additionally turned cells with air above, solid
                        // below and solid/acid on all four sides into sulfuric_acid_block, with ore
                        // beneath and in the solid horizontal neighbours. sulfuric_acid_block is not
                        // ported, so only the dry branch runs.
                        boolean touchesAir = false;
                        for (Direction dir : Direction.values()) {
                            BlockState neighbour = level.getBlockState(pos.relative(dir));
                            if (neighbour.isAir() || neighbour.getBlock() instanceof com.hbm.blocks.generic.BlockStalagmite) {
                                touchesAir = true;
                                break;
                            }
                        }
                        if (touchesAir) level.setBlock(pos, ore, 2 | 16);
                        continue;
                    }

                    // Not solid rock: a 1-in-5 chance of a spike, hanging or standing, never in liquid.
                    if (!state.getFluidState().is(Fluids.EMPTY)) continue;
                    if (rand.nextInt(5) != 0) continue;
                    if (stalactite.canSurvive(level, pos)) {
                        level.setBlock(pos, stalactite, 2 | 16);
                    } else if (stalagmite.canSurvive(level, pos)) {
                        level.setBlock(pos, stalagmite, 2 | 16);
                    }
                }
            }
        }
    }
}
