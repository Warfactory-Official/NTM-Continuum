package com.hbm.world.feature;

import com.hbm.blocks.ModDecoCrateBlocks;
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
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockStateMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

/**
 * {@code hbm:pink_tree}: 1.7 {@code HbmWorldGen}'s pink tree, one chunk in 1000. In a random column, every block from the
 * bottom of the world to the top that matches {@code target} (1.7 {@code Blocks.log} metadata 0: an upright oak log)
 * becomes {@code state}. It must see the trees, so it is added after vanilla's vegetation (see
 * {@code ModBiomeModifiers}).
 */
public class PinkTreeFeature extends Feature<PinkTreeFeature.Config> {

    public record Config(RuleTest target, BlockState state) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                FeatureCodecs.rule("target", () -> new BlockStateMatchTest(Blocks.OAK_LOG.defaultBlockState())).forGetter(Config::target),
                FeatureCodecs.state("state", ModDecoCrateBlocks.pink_log).forGetter(Config::state)
        ).apply(i, Config::new));
    }

    public PinkTreeFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        // 1.7 HbmWorldGen: the pink tree sits inside the enableDungeons block
        if (!DungeonBlockFeature.dungeonsEnabled(level)) return false;
        Config c = context.config();
        RandomSource rand = context.random();
        BlockPos origin = context.origin();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean done = false;
        for (int y = level.getMinY(); y < level.getMaxY(); y++) {
            pos.set(origin.getX(), y, origin.getZ());
            if (c.target().test(level.getBlockState(pos), rand)) {
                level.setBlock(pos, c.state(), 2);
                done = true;
            }
        }
        return done;
    }
}
