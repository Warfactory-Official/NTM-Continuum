package com.hbm.world.feature;

import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** The noise strata's fields, one per seed and octave count (1.7 {@code new NoiseGeneratorPerlin(new Random(seed), n)}). */
final class NoiseCache {

    private NoiseCache() {}

    private record Key(long seed, int octaves) {}

    private static final Map<Key, LegacyPerlinNoise> CACHE = new ConcurrentHashMap<>();

    static LegacyPerlinNoise get(long seed, int octaves) {
        if (CACHE.size() > 256) CACHE.clear();
        return CACHE.computeIfAbsent(new Key(seed, octaves),
                k -> new LegacyPerlinNoise(new WorldgenRandom(new LegacyRandomSource(k.seed())), k.octaves()));
    }
}
