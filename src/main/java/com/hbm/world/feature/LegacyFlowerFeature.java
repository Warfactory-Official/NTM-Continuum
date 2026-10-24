package com.hbm.world.feature;

import com.hbm.saveddata.TomSaveData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import static com.hbm.world.feature.FeatureCodecs.field;

/**
 * {@code hbm:legacy_flowers}: 1.7 {@code DungeonToolbox.generateFlowers}, i.e. vanilla-1.7 {@code WorldGenFlowers}.
 * {@code tries} tries around the placed position, each {@code x + rand(xz_spread) - rand(xz_spread)},
 * {@code y + rand(y_spread) - rand(y_spread)}, {@code z + rand(xz_spread) - rand(xz_spread)}; a try places
 * {@code state} when the spot is air and the plant can stay there. The chunk gate, the column and
 * {@code y = getHeightValue} are the placed feature's {@code rarity_filter} + {@code in_square} + {@code heightmap}.
 *
 * <p>A feature of its own rather than vanilla's patch because 26.1 dissolved {@code random_patch} into placement
 * modifiers. 1.7 generated none of these after a Tom impact ({@code TomSaveData.impact}); neither does this.
 */
public class LegacyFlowerFeature extends Feature<LegacyFlowerFeature.Config> {

    public record Config(BlockState state, int tries, int xzSpread, int ySpread) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockState.CODEC.fieldOf("state").forGetter(Config::state),
                field(Codec.INT, "tries", 64).forGetter(Config::tries),
                FeatureCodecs.positive("xz_spread", 8).forGetter(Config::xzSpread),
                FeatureCodecs.positive("y_spread", 4).forGetter(Config::ySpread)
        ).apply(i, Config::new));
    }

    public LegacyFlowerFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        if (TomSaveData.impact(level.getLevel())) return false;
        Config c = context.config();
        RandomSource rand = context.random();
        BlockPos origin = context.origin();
        BlockState state = c.state();
        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int l = 0; l < c.tries(); l++) {
            pos.set(origin.getX() + rand.nextInt(c.xzSpread()) - rand.nextInt(c.xzSpread()),
                    origin.getY() + rand.nextInt(c.ySpread()) - rand.nextInt(c.ySpread()),
                    origin.getZ() + rand.nextInt(c.xzSpread()) - rand.nextInt(c.xzSpread()));
            if (pos.getY() < level.getMinY() || pos.getY() >= level.getMaxY()) continue;
            if (!level.isEmptyBlock(pos) || !state.canSurvive(level, pos)) continue;
            level.setBlock(pos, state, 2);
            placed = true;
        }
        return placed;
    }
}
