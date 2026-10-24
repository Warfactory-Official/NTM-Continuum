package com.hbm.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

import static com.hbm.world.feature.FeatureCodecs.field;

/**
 * {@code hbm:crust_column}: the body of 1.7 {@code HbmWorldGen.generateNether}'s two surface loops: for a column and a
 * depth {@code d}, every {@code host} block in {@code d - depth .. d} with air directly above becomes {@code state}.
 * 1.7 ran it 30 times per chunk with {@code ore_nether_smoldering} and once with {@code geysir_nether}; the column is the
 * placed feature's {@code in_square}, {@code d = 16 + rand.nextInt(96)} its uniform {@code height_range}.
 */
public class CrustColumnFeature extends Feature<CrustColumnFeature.Config> {

    public record Config(BlockState state, RuleTest host, int depth) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockState.CODEC.fieldOf("state").forGetter(Config::state),
                FeatureCodecs.rule("host", () -> FeatureCodecs.block(Blocks.NETHERRACK)).forGetter(Config::host),
                field(Codec.INT, "depth", 5).forGetter(Config::depth)
        ).apply(i, Config::new));
    }

    public CrustColumnFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        Config c = context.config();
        return scan(context.level(), context.random(), context.origin(), c.state(), c.host(), c.depth()) > 0;
    }

    /** One default 1.7 column pass at {@code top} = (x, d, z); returns how many blocks it converted. */
    public static int scan(WorldGenLevel level, BlockPos top, BlockState state) {
        return scan(level, level.getRandom(), top, state, FeatureCodecs.block(Blocks.NETHERRACK), 5);
    }

    private static int scan(WorldGenLevel level, RandomSource rand, BlockPos top, BlockState state, RuleTest host, int depth) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int placed = 0;
        for (int y = top.getY() - depth; y <= top.getY(); y++) {
            if (y < level.getMinY() || y >= level.getMaxY()) continue;
            pos.set(top.getX(), y, top.getZ());
            if (!host.test(level.getBlockState(pos), rand)) continue;
            if (!level.getBlockState(pos.above()).isAir()) continue;
            level.setBlock(pos, state, 2 | 16);
            placed++;
        }
        return placed;
    }
}
