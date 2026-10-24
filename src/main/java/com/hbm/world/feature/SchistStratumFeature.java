package com.hbm.world.feature;

import com.hbm.blocks.ModBlocks;
import com.hbm.lib.HbmBlockTags;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

import static com.hbm.world.feature.FeatureCodecs.field;

/**
 * {@code hbm:schist_stratum}, 1.7.10's {@code SchistStratum}: a 2D Perlin field over the chunk decides, per column, how
 * thick a slab of {@code state} to lay down around {@code y_level}:
 * <pre>
 *   n = noise(x * scale, z * scale); nothing unless n &gt; threshold
 *   range = (int) ((n - threshold) * range_mult)    // 1.7 truncates BEFORE the fold
 *   if range &gt; max_range: range = 2 * max_range - range; nothing if range &lt; 0
 *   fill y in [y_level - range, y_level + range] where the block is a solid host cube
 * </pre>
 * It must run before the gneiss veins, which target {@code stone_gneiss} exactly. The noise is 1.7's
 * {@code NoiseGeneratorPerlin(new Random(worldSeed + seed_offset), octaves)}, in world coordinates (no +8 offset).
 */
public class SchistStratumFeature extends Feature<SchistStratumFeature.Config> {

    public record Config(BlockState state, RuleTest host, double scale, double threshold, int rangeMult, int maxRange,
                         int yLevel, int octaves, long seedOffset) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                FeatureCodecs.state("state", ModBlocks.stone_gneiss).forGetter(Config::state),
                FeatureCodecs.rule("host", () -> FeatureCodecs.tag(HbmBlockTags.LEGACY_STONE)).forGetter(Config::host),
                field(Codec.DOUBLE, "scale", 0.01D).forGetter(Config::scale),
                field(Codec.DOUBLE, "threshold", 5.0D).forGetter(Config::threshold),
                field(Codec.INT, "range_mult", 3).forGetter(Config::rangeMult),
                field(Codec.INT, "max_range", 4).forGetter(Config::maxRange),
                field(Codec.INT, "y_level", 30).forGetter(Config::yLevel),
                FeatureCodecs.positive("octaves", 4).forGetter(Config::octaves),
                field(Codec.LONG, "seed_offset", 0L).forGetter(Config::seedOffset)
        ).apply(i, Config::new));

        int range(double n) {
            int range = (int) ((n - threshold) * rangeMult);
            if (range > maxRange) range = maxRange * 2 - range;
            return range;
        }
    }

    public SchistStratumFeature(Codec<Config> codec) {
        super(codec);
    }

    /** The band's half-thickness at the default configuration for a noise value above 5, or negative for none. */
    public static int range17(double n) {
        return FeatureCodecs.defaults(Config.CODEC).range(n);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        Config c = context.config();
        WorldGenLevel level = context.level();
        RandomSource rand = context.random();
        int cx = context.origin().getX();
        int cz = context.origin().getZ();
        LegacyPerlinNoise perlin = NoiseCache.get(level.getSeed() + c.seedOffset(), c.octaves());

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = level.getMinY() + 1;
        int maxY = level.getMaxY() - 1;

        for (int x = cx; x < cx + 16; x++) {
            for (int z = cz; z < cz + 16; z++) {
                double n = perlin.getValue(x * c.scale(), z * c.scale());
                if (n <= c.threshold()) continue;

                int r = c.range(n);
                if (r < 0) continue;

                int yStart = Math.max(minY, c.yLevel() - r);
                int yEnd = Math.min(maxY, c.yLevel() + r);

                for (int y = yStart; y <= yEnd; y++) {
                    pos.set(x, y, z);
                    BlockState target = level.getBlockState(pos);
                    // 1.7: target.isNormalCube() && material == ROCK
                    if (Compat.isSolidRender(target) && c.host().test(target, rand)) {
                        level.setBlock(pos, c.state(), 2 | 16);
                    }
                }
            }
        }
        return true;
    }
}
