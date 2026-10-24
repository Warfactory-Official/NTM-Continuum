package com.hbm.world.feature;

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
 * {@code hbm:fallen_meteorite}: 1.7 {@code HbmWorldGen}'s fallen meteorite, one chunk in {@code meteoriteSpawn}. The
 * gate, the column and {@code getHeightValue} are the placed feature's {@code rarity_filter} + {@code in_square} +
 * {@code heightmap}; the meteorite is then sunk {@code rand(max_sink)} blocks and built unless the block two below that
 * point is air or liquid. 1.7 read that block at {@code y - 2} but asked {@code isAir} at {@code y}: the block type
 * decides, which is what is checked here.
 */
public class FallenMeteoriteFeature extends Feature<FallenMeteoriteFeature.Config> {

    /** {@code safe}, {@code specials} and {@code damage} are 1.7 {@code Meteorite.generate}'s three flags. */
    public record Config(int maxSink, boolean safe, boolean specials, boolean damage) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                FeatureCodecs.positive("max_sink", 10).forGetter(Config::maxSink),
                field(Codec.BOOL, "safe", false).forGetter(Config::safe),
                field(Codec.BOOL, "specials", false).forGetter(Config::specials),
                field(Codec.BOOL, "damage", false).forGetter(Config::damage)
        ).apply(i, Config::new));
    }

    public FallenMeteoriteFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        Config c = context.config();
        WorldGenLevel level = context.level();
        RandomSource rand = context.random();
        BlockPos top = context.origin();
        int y = top.getY() - rand.nextInt(c.maxSink());
        BlockState b = level.getBlockState(new BlockPos(top.getX(), y - 2, top.getZ()));
        if (b.isAir() || !b.getFluidState().isEmpty() || y <= level.getMinY() + 1) return false;
        new Meteorite().generate(level, rand, new BlockPos(top.getX(), y, top.getZ()), c.safe(), c.specials(), c.damage());
        return true;
    }
}
