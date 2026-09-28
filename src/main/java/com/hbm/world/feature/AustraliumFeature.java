package com.hbm.world.feature;

import com.hbm.blocks.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * {@code hbm:australium}: 1.7's hardcoded australium pocket, verbatim — {@code rand.nextInt(4)} attempts a chunk, each a
 * random position ({@code y = 15 + rand(15)}) that gets a size-50 vein only inside the fixed square
 * x, z in [-450, -350]. A fixed world location is not a chance, so it stays in the feature.
 */
public class AustraliumFeature extends Feature<NoneFeatureConfiguration> {

    public AustraliumFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        RandomSource rand = context.random();
        int i = context.origin().getX() & ~15, j = context.origin().getZ() & ~15;
        boolean placed = false;
        // 1.7 re-evaluates rand.nextInt(4) as the loop bound on every iteration; kept.
        for (int k = 0; k < rand.nextInt(4); k++) {
            int x = i + rand.nextInt(16);
            int y = rand.nextInt(15) + 15;
            int z = j + rand.nextInt(16);
            if (x <= -350 && x >= -450 && z <= -350 && z >= -450) {
                OreVeinPlacer.placeVein(context.level(), rand, x, y, z, 50,
                        ModBlocks.ore_australium.get().defaultBlockState(), OreVeinPlacer.STONE);
                placed = true;
            }
        }
        return placed;
    }
}
