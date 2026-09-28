package com.hbm.world.feature;

import com.hbm.blocks.ModBlocks;
import com.hbm.config.GeneralConfig;
import com.mojang.serialization.Codec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.Random;

/**
 * {@code hbm:coltan_deposit}: 1.7's 528 coltan deposit, verbatim. Its centre is a gaussian drawn from
 * {@code new Random(seed + 5)} (x and z, sigma 1500), so it is not a per-chunk chance and stays in the feature: twice per
 * chunk, for each band {@code r = 1..5}, a random position ({@code y = 15 + rand(25)}) gets a size-4 vein if it lies
 * within {@code 750 / r} of the centre. Density therefore climbs in five steps towards the centre (up to ten veins a
 * chunk there).
 *
 * <p>This corrects the earlier port, which tested the <i>chunk</i> against each band and then placed five veins per
 * band, up to 25 a chunk.
 */
public class ColtanDepositFeature extends Feature<NoneFeatureConfiguration> {

    public ColtanDepositFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!GeneralConfig.enable528ColtanDeposit) return false;
        WorldGenLevel level = context.level();
        RandomSource rand = context.random();
        int i = context.origin().getX() & ~15, j = context.origin().getZ() & ~15;
        Random colRand = new Random(level.getSeed() + 5);
        int colX = (int) (colRand.nextGaussian() * 1500);
        int colZ = (int) (colRand.nextGaussian() * 1500);
        int colRange = 750;
        BlockState coltan = ModBlocks.ore_coltan.get().defaultBlockState();
        boolean placed = false;
        for (int k = 0; k < 2; k++) {
            for (int r = 1; r <= 5; r++) {
                int x = i + rand.nextInt(16);
                int y = rand.nextInt(25) + 15;
                int z = j + rand.nextInt(16);
                int range = colRange / r;
                if (x <= colX + range && x >= colX - range && z <= colZ + range && z >= colZ - range) {
                    OreVeinPlacer.placeVein(level, rand, x, y, z, 4, coltan, OreVeinPlacer.STONE);
                    placed = true;
                }
            }
        }
        return placed;
    }
}
