package com.hbm.world.feature;

import com.hbm.blocks.ModBlocks;
import com.hbm.config.WorldConfig;
import com.mojang.serialization.Codec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import static com.hbm.world.feature.OverworldOreFeature.state;

/**
 * The End half of 1.12.2's {@code HbmWorldGen.generateOres}: trixite ({@code ore_tikite}) in end stone,
 * 8 veins of size 6 across the full 0–126 height band, plus the dimension-agnostic limestone.
 *
 * <p>The spelling drift is upstream's: the config key is {@code 03.E01_tixiteSpawnrate}, the mirrored
 * field is {@code endTikiteSpawn}, and the block is {@code ore_tikite}.
 */
public class EndOreFeature extends Feature<NoneFeatureConfiguration> {

    public EndOreFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource rand = context.random();
        int cx = context.origin().getX();
        int cz = context.origin().getZ();

        OreVeinPlacer.generateOre(level, rand, cx, cz, WorldConfig.endTikiteSpawn, 6, 0, 127,
                state(ModBlocks.ore_tikite), OreVeinPlacer.END_STONE);
        OverworldOreFeature.limestone(level, rand, cx, cz);
        return true;
    }
}
