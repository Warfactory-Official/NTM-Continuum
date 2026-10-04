package com.hbm.world.feature;

import com.hbm.config.MobConfig;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * {@code hbm:glyphid_hive}: 1.7 {@code HbmWorldGen}'s surface hive. The {@code rand.nextInt(hiveSpawn) == 0} gate,
 * the random column and {@code y = getHeightValue} are the placed feature's {@code rarity_filter} + {@code in_square}
 * + {@code heightmap}; this then looks from 3 above to 1 below that surface for the first full cube to stand on and
 * builds a small hive there, infested one time in ten, with hive loot.
 *
 * <p>1.7 also skipped the hive when structures ("map features") were switched off for the world; kept.
 */
public class GlyphidHiveFeature extends Feature<NoneFeatureConfiguration> {

    public GlyphidHiveFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!MobConfig.enableHives) return false;
        WorldGenLevel level = context.level();
        // 1.7 HbmWorldGen enableDungeons (config 2 = the world's Generate Structures option)
        if (!DungeonBlockFeature.dungeonsEnabled(level)) return false;
        RandomSource rand = context.random();
        BlockPos top = context.origin();
        for (int k = 3; k >= -1; k--) {
            BlockPos below = top.above(k - 1);
            BlockState state = level.getBlockState(below);
            // 1.7 isNormalCube: an opaque full cube
            if (state.canOcclude() && state.isCollisionShapeFullBlock(level, below)) {
                GlyphidHive.generateSmall(level, top.getX(), top.getY() + k, top.getZ(), rand, rand.nextInt(10) == 0, true);
                return true;
            }
        }
        return false;
    }
}
