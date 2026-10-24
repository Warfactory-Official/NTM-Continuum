package com.hbm.world.feature;

import com.hbm.blocks.ModBlocks;
import com.hbm.lib.HbmBlockTags;
import com.hbm.world.gen.ConfigInt;
import com.hbm.world.gen.WorldgenKeys;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

import java.util.Random;

import static com.hbm.world.feature.FeatureCodecs.field;
import static com.hbm.world.feature.FeatureCodecs.lazy;

/**
 * {@code hbm:coltan_deposit}: 1.7's 528 coltan deposit. Its centre is a gaussian drawn from
 * {@code new Random(seed + seed_offset)} (x and z, sigma {@code spread}), so it is not a per-chunk chance and stays in
 * the feature: {@code attempts} times per chunk, for each band {@code r = 1..rings}, a random position
 * ({@code y = min_y + rand(y_range)}) gets a vein if it lies within {@code base_range / r} of the centre.
 */
public class ColtanDepositFeature extends Feature<ColtanDepositFeature.Config> {

    public record Config(BlockState ore, RuleTest host, int size, int attempts, int rings, int baseRange, double spread,
                         long seedOffset, int minY, int yRange, ConfigInt enabled) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                FeatureCodecs.state("ore", ModBlocks.ore_coltan).forGetter(Config::ore),
                FeatureCodecs.rule("host", () -> FeatureCodecs.tag(HbmBlockTags.LEGACY_STONE)).forGetter(Config::host),
                field(Codec.INT, "size", 4).forGetter(Config::size),
                field(Codec.INT, "attempts", 2).forGetter(Config::attempts),
                field(Codec.INT, "rings", 5).forGetter(Config::rings),
                field(Codec.INT, "base_range", 750).forGetter(Config::baseRange),
                field(Codec.DOUBLE, "spread", 1500D).forGetter(Config::spread),
                field(Codec.LONG, "seed_offset", 5L).forGetter(Config::seedOffset),
                field(Codec.INT, "min_y", 15).forGetter(Config::minY),
                FeatureCodecs.positive("y_range", 25).forGetter(Config::yRange),
                lazy(ConfigInt.CODEC, "enabled", () -> ConfigInt.key(WorldgenKeys.COLTAN_DEPOSIT)).forGetter(Config::enabled)
        ).apply(i, Config::new));
    }

    public ColtanDepositFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        Config c = context.config();
        if (c.enabled().get() == 0) return false;
        WorldGenLevel level = context.level();
        RandomSource rand = context.random();
        int i = context.origin().getX() & ~15, j = context.origin().getZ() & ~15;
        Random colRand = new Random(level.getSeed() + c.seedOffset());
        int colX = (int) (colRand.nextGaussian() * c.spread());
        int colZ = (int) (colRand.nextGaussian() * c.spread());
        boolean placed = false;
        for (int k = 0; k < c.attempts(); k++) {
            for (int r = 1; r <= c.rings(); r++) {
                int x = i + rand.nextInt(16);
                int y = rand.nextInt(c.yRange()) + c.minY();
                int z = j + rand.nextInt(16);
                int range = c.baseRange() / r;
                if (x <= colX + range && x >= colX - range && z <= colZ + range && z >= colZ - range) {
                    OreVeinPlacer.placeVein(level, rand, x, y, z, c.size(), state -> c.host().test(state, rand) ? c.ore() : null);
                    placed = true;
                }
            }
        }
        return placed;
    }
}
