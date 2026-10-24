package com.hbm.world.feature;

import com.hbm.config.MobConfig;
import com.hbm.itempool.ItemPools;
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
 * {@code hbm:glyphid_hive}: 1.7 {@code HbmWorldGen}'s surface hive. The {@code rand.nextInt(hiveSpawn) == 0} gate, the
 * random column and {@code y = getHeightValue} are the placed feature's {@code rarity_filter} + {@code in_square} +
 * {@code heightmap}; this then looks from {@code scan_up} above to {@code scan_down} below that surface for the first
 * full cube to stand on and builds a small hive there, infested one time in {@code infested_chance}, with its loot
 * piles drawn from the item pools {@code loot_pool} and {@code bones_pool}.
 *
 * <p>1.7 also skipped the hive when structures ("map features") were switched off for the world; kept.
 */
public class GlyphidHiveFeature extends Feature<GlyphidHiveFeature.Config> {

    public record Config(int infestedChance, int spawnerChance, int scanUp, int scanDown, String lootPool,
                         String bonesPool) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                FeatureCodecs.positive("infested_chance", 10).forGetter(Config::infestedChance),
                FeatureCodecs.positive("spawner_chance", 3).forGetter(Config::spawnerChance),
                field(Codec.INT, "scan_up", 3).forGetter(Config::scanUp),
                field(Codec.INT, "scan_down", 1).forGetter(Config::scanDown),
                field(Codec.STRING, "loot_pool", ItemPools.POOL_PILE_HIVE).forGetter(Config::lootPool),
                field(Codec.STRING, "bones_pool", ItemPools.POOL_PILE_BONES).forGetter(Config::bonesPool)
        ).apply(i, Config::new));
    }

    public GlyphidHiveFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        if (!MobConfig.enableHives) return false;
        WorldGenLevel level = context.level();
        // 1.7 HbmWorldGen enableDungeons (config 2 = the world's Generate Structures option)
        if (!DungeonBlockFeature.dungeonsEnabled(level)) return false;
        Config c = context.config();
        RandomSource rand = context.random();
        BlockPos top = context.origin();
        for (int k = c.scanUp(); k >= -c.scanDown(); k--) {
            BlockPos below = top.above(k - 1);
            BlockState state = level.getBlockState(below);
            // 1.7 isNormalCube: an opaque full cube
            if (state.canOcclude() && state.isCollisionShapeFullBlock(level, below)) {
                GlyphidHive.generateSmall(level, top.getX(), top.getY() + k, top.getZ(), rand,
                        rand.nextInt(c.infestedChance()) == 0, c.spawnerChance(), c.lootPool(), c.bonesPool());
                return true;
            }
        }
        return false;
    }
}
