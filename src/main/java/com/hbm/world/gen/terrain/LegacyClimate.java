package com.hbm.world.gen.terrain;

import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;

/**
 * The two 1.7 biome climate tests HBM's terrain generators use, read off a modern biome.
 *
 * <p>Modern vanilla kept 1.7's temperature and downfall numbers for almost every biome it carried over, so the
 * base temperature and the precipitation flag answer 1.7's questions directly. The one family that moved across the
 * thresholds used here is the savanna: 1.7 had it at 1.2 (plateau 1.0, the M variants 1.1 / 1.0), 1.18 raised all of
 * them to 2.0. They are answered with 1.7's value, so a savanna is neither "hot and dry" nor sand-bubble country, as
 * in 1.7.
 *
 * <p>{@code rainfall < 0.1} is read as "no precipitation": every modern biome at or above 1.5 without precipitation
 * has downfall 0, and every one with precipitation has more than 0.1. (Downfall itself is private in every
 * supported version.)
 */
public final class LegacyClimate {

    private LegacyClimate() {}

    /** 1.7 {@code biome.temperature}. */
    public static float temperature(Holder<Biome> biome) {
        if (biome.is(BiomeTags.IS_SAVANNA)) return 1.2F;
        return biome.value().getBaseTemperature();
    }

    /** 1.7 {@code biome.rainfall < 0.1F}. */
    public static boolean dry(Holder<Biome> biome) {
        return !biome.value().hasPrecipitation();
    }

    /** 1.7 {@code canSpawnLightningBolt()}: it rains there, and rain does not fall as snow. */
    public static boolean lightning(Holder<Biome> biome) {
        return biome.value().hasPrecipitation() && biome.value().getBaseTemperature() >= 0.15F;
    }

    /** 1.7 {@code MapGenBubble}'s rate divisor test: {@code temperature >= 2 && rainfall < 0.1}. */
    public static boolean hotAndDry(Holder<Biome> biome) {
        return temperature(biome) >= 2.0F && dry(biome);
    }
}
