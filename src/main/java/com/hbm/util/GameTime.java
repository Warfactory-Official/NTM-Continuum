package com.hbm.util;

import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.function.LongSupplier;

public final class GameTime {

    public static @Nullable LongSupplier CLIENT_MILLIS;
    public static @Nullable LongSupplier CLIENT_NOW;

    private GameTime() {
    }

    public static long millis(Level level) {
        return level.getGameTime() * 50L;
    }

    public static long millis() {
        return CLIENT_MILLIS == null ? 0L : CLIENT_MILLIS.getAsLong();
    }

    public static long now() {
        return CLIENT_NOW == null ? 0L : CLIENT_NOW.getAsLong();
    }

    public static long ticks() {
        return millis() / 50L;
    }
}
