package com.hbm.world.feature;

import com.hbm.blocks.ModBlocks;
import com.hbm.lib.HbmBlockTags;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

import static com.hbm.world.feature.FeatureCodecs.field;

/**
 * {@code hbm:australium}: 1.7's hardcoded australium pocket: {@code rand.nextInt(attempts)} tries a chunk, each a
 * random position ({@code y = min_y + rand(y_range)}) that gets a vein only inside the fixed square
 * {@code min_x..max_x}, {@code min_z..max_z}. A fixed world location is not a chance, so it stays in the feature.
 */
public class AustraliumFeature extends Feature<AustraliumFeature.Config> {

    public record Config(BlockState ore, RuleTest host, int size, int attempts, int minY, int yRange,
                         int minX, int maxX, int minZ, int maxZ) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                FeatureCodecs.state("ore", ModBlocks.ore_australium).forGetter(Config::ore),
                FeatureCodecs.rule("host", () -> FeatureCodecs.tag(HbmBlockTags.LEGACY_STONE)).forGetter(Config::host),
                field(Codec.INT, "size", 50).forGetter(Config::size),
                FeatureCodecs.positive("attempts", 4).forGetter(Config::attempts),
                field(Codec.INT, "min_y", 15).forGetter(Config::minY),
                FeatureCodecs.positive("y_range", 15).forGetter(Config::yRange),
                field(Codec.INT, "min_x", -450).forGetter(Config::minX),
                field(Codec.INT, "max_x", -350).forGetter(Config::maxX),
                field(Codec.INT, "min_z", -450).forGetter(Config::minZ),
                field(Codec.INT, "max_z", -350).forGetter(Config::maxZ)
        ).apply(i, Config::new));
    }

    public AustraliumFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        Config c = context.config();
        RandomSource rand = context.random();
        int i = context.origin().getX() & ~15, j = context.origin().getZ() & ~15;
        boolean placed = false;
        // 1.7 re-evaluates rand.nextInt(4) as the loop bound on every iteration; kept
        for (int k = 0; k < rand.nextInt(c.attempts()); k++) {
            int x = i + rand.nextInt(16);
            int y = rand.nextInt(c.yRange()) + c.minY();
            int z = j + rand.nextInt(16);
            if (x <= c.maxX() && x >= c.minX() && z <= c.maxZ() && z >= c.minZ()) {
                OreVeinPlacer.placeVein(context.level(), rand, x, y, z, c.size(), state -> c.host().test(state, rand) ? c.ore() : null);
                placed = true;
            }
        }
        return placed;
    }
}
