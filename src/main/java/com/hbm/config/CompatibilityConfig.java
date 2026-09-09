package com.hbm.config;

import dev.golem.core.Ids;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.level.Level;

/** Dimension safety switches shared by destructive world effects. */
public final class CompatibilityConfig {
    private CompatibilityConfig() {}

    /** Old default: the listed dimensions are peaceful exclusions. */
    public static boolean peaceDimensionsIsWhitelist = true;
    public static final Set<String> peaceDimensions = new HashSet<>();

    public static boolean isWarDim(Level level) {
        return level != null && isWarDim(Ids.keyId(level.dimension()).toString());
    }

    public static boolean isWarDim(String dimension) {
        boolean listed = peaceDimensions.contains(dimension);
        return peaceDimensionsIsWhitelist ? !listed : listed;
    }
}
