package com.hbm.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * {@code hbm:depth_deposit}: one 1.7 {@code DepthDeposit.generate} sphere at the placed position: the ore core
 * ({@code len + rand(2) < size * fill}) wrapped in depth rock, eating {@code host} and bedrock alike.
 *
 * <p>1.7's {@code generateCondition*} wrapper ({@code rand.nextInt(chance) == 0}, a random in-chunk x/z and
 * {@code y = yMin + rand.nextInt(3)}) is the placed feature's {@code rarity_filter} + {@code in_square} +
 * {@code height_range}.
 */
public class DepthDepositFeature extends Feature<DepthDepositFeature.Config> {

    /** {@code ore} inside {@code fill}, {@code filler} out to {@code size}, replacing {@code host} (and bedrock). */
    public record Config(BlockState ore, BlockState filler, BlockState host, int size, float fill) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockState.CODEC.fieldOf("ore").forGetter(Config::ore),
                BlockState.CODEC.fieldOf("filler").forGetter(Config::filler),
                BlockState.CODEC.fieldOf("host").forGetter(Config::host),
                Codec.INT.fieldOf("size").forGetter(Config::size),
                Codec.FLOAT.fieldOf("fill").forGetter(Config::fill)
        ).apply(i, Config::new));
    }

    public DepthDepositFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        Config c = context.config();
        BlockPos o = context.origin();
        DepthDepositPlacer.generateSphere(context.level(), context.random(), o.getX(), o.getY(), o.getZ(),
                c.size(), c.fill(), c.ore(), c.host(), c.filler());
        return true;
    }
}
