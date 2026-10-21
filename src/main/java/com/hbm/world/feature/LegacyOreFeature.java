package com.hbm.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

/**
 * {@code hbm:legacy_ore}: one 1.7 {@code WorldGenMinable} vein at the placed position, through
 * {@link OreVeinPlacer#placeVein} (the vanilla-1.7 ellipsoid, {@code +8} offset included).
 *
 * <p>This is the vein half of 1.7's {@code DungeonToolbox.generateOre}. The loop half ({@code veinCount} times a
 * random in-chunk x/z and {@code y = minHeight + rand.nextInt(variance)}, or a {@code rand.nextInt(n) == 0} gate)
 * is the placed feature's {@code count} / {@code rarity_filter} + {@code in_square} + uniform {@code height_range}
 * (see {@code ModBiomeModifiers}), so every ore is its own placed feature and {@code /place feature hbm:<ore>}
 * places exactly one vein.
 *
 * <p>The configuration is vanilla's {@code ore} shape. A host block becomes the state of the first target whose rule
 * matches it, so an ore with a deepslate twin lists the deepslate rule first. {@code discard_chance_on_air_exposure}
 * is not a 1.7 idea and is ignored. Vanilla's own
 * {@code ore} feature is not used because its ellipsoid differs from 1.7's (no {@code +8}, a different radius
 * formula), which would move and reshape every vein.
 */
public class LegacyOreFeature extends Feature<OreConfiguration> {

    public LegacyOreFeature(Codec<OreConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<OreConfiguration> context) {
        OreConfiguration config = context.config();
        if (config.targetStates.isEmpty() || config.size <= 0) return false;
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        OreVeinPlacer.placeVein(context.level(), random, origin.getX(), origin.getY(), origin.getZ(), config.size,
                state -> {
                    for (OreConfiguration.TargetBlockState target : config.targetStates) {
                        if (target.target.test(state, random)) return target.state;
                    }
                    return null;
                });
        return true;
    }
}
