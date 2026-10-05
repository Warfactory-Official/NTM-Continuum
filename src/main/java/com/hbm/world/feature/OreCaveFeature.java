package com.hbm.world.feature;

import dev.golem.core.world.Compat;
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
 * <p>The sulfur variant carries 1.7's acid pools ({@code withFluid(sulfuric_acid_block)}): a rock cell with air
 * (or a spike) above, a full cube below and full cubes or acid on all four sides becomes
 * {@code sulfuric_acid_block}, with the resource stone beneath it and in its solid horizontal neighbours,
 * on half of the eligible cells (1.7's {@code canGenFluid = rand.nextBoolean()}, rolled for every rock cell
 * of either variant). History §19ej-6.
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

    /** 1.7 {@code ForgeDirection.VALID_DIRECTIONS} order. */
    private static final Direction[] DIRECTIONS_17 = {
            Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    private LegacyPerlinNoise sulfurNoise, asbestosNoise;
    private long noiseSeed = Long.MIN_VALUE;

    /**
     * Columns of this world (seed) where the sulfur stratum is generated: every {@code step}-th column within
     * {@code radius} of {@code x, z} whose noise exceeds the threshold, as {@code {x, z, range}}. For the harness search.
     */
    public static java.util.List<int[]> sulfurColumns(long seed, int x, int z, int radius, int step, int limit) {
        LegacyPerlinNoise noise = new LegacyPerlinNoise(new WorldgenRandom(new LegacyRandomSource(seed + SULFUR.salt())), 2);
        java.util.List<int[]> out = new java.util.ArrayList<>();
        for (int dx = -radius; dx <= radius && out.size() < limit; dx += step) {
            for (int dz = -radius; dz <= radius && out.size() < limit; dz += step) {
                double n = noise.getValue((x + dx) * SCALE, (z + dz) * SCALE);
                if (n > SULFUR.threshold()) out.add(new int[] {x + dx, z + dz, (int) ((n - SULFUR.threshold()) * SULFUR.rangeMult())});
            }
        }
        return out;
    }

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
                    ModOreBlocks.stalagmite_sulfur.get().defaultBlockState(),
                    com.hbm.blocks.ModFluidNetBlocks.sulfuric_acid_block.get().defaultBlockState());
        }
        if (WorldConfig.enableAsbestosCave) {
            generate(level, rand, cx, cz, asbestosNoise, ASBESTOS,
                    ModOreBlocks.stone_resource_asbestos.get().defaultBlockState(),
                    ModOreBlocks.stalactite_asbestos.get().defaultBlockState(),
                    ModOreBlocks.stalagmite_asbestos.get().defaultBlockState(), null);
        }
        return true;
    }

    private static void generate(WorldGenLevel level, RandomSource rand, int cx, int cz, LegacyPerlinNoise noise,
                                 Cave cave, BlockState ore, BlockState stalactite, BlockState stalagmite,
                                 BlockState fluid) {
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
                    boolean solidRock = Compat.isSolidRender(state) && OreVeinPlacer.STONE.test(state);

                    if (solidRock) {
                        // 1.7 OreCave.onDecorate, in its direction order (DOWN, UP, NORTH, SOUTH, WEST, EAST)
                        boolean shouldGen = false;
                        boolean canGenFluid = rand.nextBoolean();

                        for (Direction dir : DIRECTIONS_17) {
                            BlockState neighbour = level.getBlockState(pos.relative(dir));
                            boolean open = neighbour.isAir() || neighbour.getBlock() instanceof com.hbm.blocks.generic.BlockStalagmite;
                            if (open) shouldGen = true;

                            if (shouldGen && (fluid == null || !canGenFluid)) break;

                            if (fluid != null) {
                                switch (dir) {
                                    case UP -> { if (!open) canGenFluid = false; }
                                    case DOWN -> { if (!Compat.isSolidRender(neighbour)) canGenFluid = false; }
                                    default -> {
                                        if (!Compat.isSolidRender(neighbour) && !neighbour.is(fluid.getBlock())) canGenFluid = false;
                                    }
                                }
                            }
                        }

                        if (fluid != null && canGenFluid) {
                            level.setBlock(pos, fluid, 2);
                            level.setBlock(pos.below(), ore, 2);

                            for (Direction dir : Direction.Plane.HORIZONTAL) {
                                BlockPos side = pos.relative(dir);
                                if (Compat.isSolidRender(level.getBlockState(side))) level.setBlock(side, ore, 2);
                            }

                        } else if (shouldGen) {
                            level.setBlock(pos, ore, 2 | 16);
                        }
                        continue;
                    }

                    // 1.7: only air or a block that is not a full cube may get a spike (1 in 5), never liquid.
                    // A solid block that is not stone (dirt, gravel, other ores) is left alone.
                    if (!state.isAir() && Compat.isSolidRender(state)) continue;
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
