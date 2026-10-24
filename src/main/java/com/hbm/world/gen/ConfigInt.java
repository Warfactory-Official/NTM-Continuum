package com.hbm.world.gen;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;

/**
 * A number in a feature configuration that is either a literal ({@code 200}) or an {@code hbm.cfg} key
 * ({@code "2.21_oilSpawnRate"}) read when the chunk generates, as {@link ConfigPlacement} reads it. A boolean key
 * reads 1 or 0; an unknown key reads 0 and logs once.
 */
public record ConfigInt(Either<Integer, String> value) {

    public static final Codec<ConfigInt> CODEC = Codec.either(Codec.INT, Codec.STRING).xmap(ConfigInt::new, ConfigInt::value);

    public static ConfigInt of(int value) {
        return new ConfigInt(Either.left(value));
    }

    public static ConfigInt key(String key) {
        if (WorldgenKeys.get(key) == null) throw new IllegalStateException("no WorldgenKeys binding for " + key);
        return new ConfigInt(Either.right(key));
    }

    public int get() {
        return value.map(i -> i, ConfigPlacement::value);
    }
}
