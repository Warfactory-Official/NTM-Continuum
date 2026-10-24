package com.hbm.world.feature;

import dev.golem.core.data.Codecs;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.ModOreBlocks;
import com.hbm.blocks.generic.BlockStalagmite;
import com.hbm.lib.HbmBlockTags;
import com.hbm.world.gen.ConfigInt;
import com.hbm.world.gen.WorldgenKeys;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.hbm.world.feature.FeatureCodecs.field;
import static com.hbm.world.feature.FeatureCodecs.lazy;

/**
 * {@code hbm:ore_caves}: 1.7's {@code OreCave} strata, run in list order (sulfur, then asbestos). A 2D Perlin field
 * picks a column's half-thickness ({@code (n - threshold) * range_mult}, folded past {@code max_range}); every solid
 * host block in {@code y_level ± range} that touches air becomes {@code ore}, and open cells there get a spike one time
 * in five. With a {@code fluid}, a host cell with air above, a full cube below and full cubes or fluid on all four
 * sides becomes the fluid on half of the eligible cells, lined with the ore (1.7 {@code withFluid}).
 *
 * <p>Deviation: 1.7 salted the seed with a numeric block id; each cave carries an explicit stable {@code seed_offset}.
 */
public class OreCaveFeature extends Feature<OreCaveFeature.Config> {

    public record Cave(BlockState ore, Optional<BlockState> stalactite, Optional<BlockState> stalagmite,
                       Optional<BlockState> fluid, RuleTest host, double threshold, int rangeMult, int maxRange,
                       int yLevel, double scale, int octaves, long seedOffset, ConfigInt enabled) {
        public static final Codec<Cave> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockState.CODEC.fieldOf("ore").forGetter(Cave::ore),
                Codecs.strictOptionalField(BlockState.CODEC, "stalactite").forGetter(Cave::stalactite),
                Codecs.strictOptionalField(BlockState.CODEC, "stalagmite").forGetter(Cave::stalagmite),
                Codecs.strictOptionalField(BlockState.CODEC, "fluid").forGetter(Cave::fluid),
                FeatureCodecs.rule("host", () -> FeatureCodecs.tag(HbmBlockTags.LEGACY_STONE)).forGetter(Cave::host),
                field(Codec.DOUBLE, "threshold", 1.5D).forGetter(Cave::threshold),
                field(Codec.INT, "range_mult", 20).forGetter(Cave::rangeMult),
                field(Codec.INT, "max_range", 20).forGetter(Cave::maxRange),
                field(Codec.INT, "y_level", 30).forGetter(Cave::yLevel),
                field(Codec.DOUBLE, "scale", 0.01D).forGetter(Cave::scale),
                FeatureCodecs.positive("octaves", 2).forGetter(Cave::octaves),
                field(Codec.LONG, "seed_offset", 0L).forGetter(Cave::seedOffset),
                field(ConfigInt.CODEC, "enabled", ConfigInt.of(1)).forGetter(Cave::enabled)
        ).apply(i, Cave::new));

        int range(double n) {
            int range = (int) ((n - threshold) * rangeMult);
            if (range > maxRange) range = maxRange * 2 - range;
            return range;
        }
    }

    public record Config(List<Cave> caves) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                lazy(Cave.CODEC.listOf(), "caves", Config::caves17).forGetter(Config::caves)
        ).apply(i, Config::new));

        /** 1.7 {@code MainRegistry}: sulfur (threshold 1.5, y 30, acid pools), then asbestos (1.75, y 25). */
        static List<Cave> caves17() {
            return List.of(
                    new Cave(ModOreBlocks.stone_resource_sulfur.get().defaultBlockState(),
                            Optional.of(ModOreBlocks.stalactite_sulfur.get().defaultBlockState()),
                            Optional.of(ModOreBlocks.stalagmite_sulfur.get().defaultBlockState()),
                            Optional.of(ModFluidNetBlocks.sulfuric_acid_block.get().defaultBlockState()),
                            FeatureCodecs.tag(HbmBlockTags.LEGACY_STONE), 1.5D, 20, 20, 30, 0.01D, 2, 0x5FL * 31 + 30,
                            ConfigInt.key(WorldgenKeys.SULFUR_CAVE)),
                    new Cave(ModOreBlocks.stone_resource_asbestos.get().defaultBlockState(),
                            Optional.of(ModOreBlocks.stalactite_asbestos.get().defaultBlockState()),
                            Optional.of(ModOreBlocks.stalagmite_asbestos.get().defaultBlockState()),
                            Optional.empty(),
                            FeatureCodecs.tag(HbmBlockTags.LEGACY_STONE), 1.75D, 20, 20, 25, 0.01D, 2, 0xA5BL * 31 + 25,
                            ConfigInt.key(WorldgenKeys.ASBESTOS_CAVE)));
        }
    }

    /** 1.7 {@code ForgeDirection.VALID_DIRECTIONS} order. */
    private static final Direction[] DIRECTIONS_17 = {
            Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    /**
     * Columns of this world (seed) where the default sulfur stratum is generated: every {@code step}-th column within
     * {@code radius} of {@code x, z} whose noise exceeds the threshold, as {@code {x, z, range}}. For the harness search.
     */
    public static List<int[]> sulfurColumns(long seed, int x, int z, int radius, int step, int limit) {
        Cave sulfur = Config.caves17().get(0);
        LegacyPerlinNoise noise = NoiseCache.get(seed + sulfur.seedOffset(), sulfur.octaves());
        List<int[]> out = new ArrayList<>();
        for (int dx = -radius; dx <= radius && out.size() < limit; dx += step) {
            for (int dz = -radius; dz <= radius && out.size() < limit; dz += step) {
                double n = noise.getValue((x + dx) * sulfur.scale(), (z + dz) * sulfur.scale());
                if (n > sulfur.threshold()) {
                    out.add(new int[] {x + dx, z + dz, (int) ((n - sulfur.threshold()) * sulfur.rangeMult())});
                }
            }
        }
        return out;
    }

    public OreCaveFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        RandomSource rand = context.random();
        int cx = context.origin().getX();
        int cz = context.origin().getZ();
        for (Cave cave : context.config().caves()) {
            if (cave.enabled().get() == 0) continue;
            generate(level, rand, cx, cz, NoiseCache.get(level.getSeed() + cave.seedOffset(), cave.octaves()), cave);
        }
        return true;
    }

    private static void generate(WorldGenLevel level, RandomSource rand, int cx, int cz, LegacyPerlinNoise noise, Cave cave) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int floor = level.getMinY();
        int ceiling = level.getMaxY();
        BlockState ore = cave.ore();
        BlockState fluid = cave.fluid().orElse(null);

        // the 1.7 +8 decoration offset on both axes
        for (int x = cx + 8; x < cx + 24; x++) {
            for (int z = cz + 8; z < cz + 24; z++) {
                double n = noise.getValue(x * cave.scale(), z * cave.scale());
                if (n <= cave.threshold()) continue;

                int range = cave.range(n);
                if (range < 0) continue;

                for (int y = cave.yLevel() - range; y <= cave.yLevel() + range; y++) {
                    if (y < floor || y > ceiling) continue;
                    pos.set(x, y, z);
                    if (!level.hasChunk(x >> 4, z >> 4)) continue;

                    BlockState state = level.getBlockState(pos);
                    boolean solidRock = Compat.isSolidRender(state) && cave.host().test(state, rand);

                    if (solidRock) {
                        // 1.7 OreCave.onDecorate, in its direction order
                        boolean shouldGen = false;
                        boolean canGenFluid = rand.nextBoolean();

                        for (Direction dir : DIRECTIONS_17) {
                            BlockState neighbour = level.getBlockState(pos.relative(dir));
                            boolean open = neighbour.isAir() || neighbour.getBlock() instanceof BlockStalagmite;
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

                    // 1.7: only air or a block that is not a full cube may get a spike (1 in 5), never liquid
                    if (!state.isAir() && Compat.isSolidRender(state)) continue;
                    if (!state.getFluidState().is(Fluids.EMPTY)) continue;
                    if (rand.nextInt(5) != 0) continue;
                    if (cave.stalactite().isPresent() && cave.stalactite().get().canSurvive(level, pos)) {
                        level.setBlock(pos, cave.stalactite().get(), 2 | 16);
                    } else if (cave.stalagmite().isPresent() && cave.stalagmite().get().canSurvive(level, pos)) {
                        level.setBlock(pos, cave.stalagmite().get(), 2 | 16);
                    }
                }
            }
        }
    }
}
