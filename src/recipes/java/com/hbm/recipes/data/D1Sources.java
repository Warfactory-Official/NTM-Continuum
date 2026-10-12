package com.hbm.recipes.data;

import dev.golem.core.world.DataSource;
import java.util.List;

/** Data agent D1's sources: 1.7's fluid properties in HBM's namespace (the minecraft ones: {@code MinecraftDataSources}). */
public final class D1Sources {

    private D1Sources() {
    }

    public static List<DataSource<?>> sources() {
        return List.of(new FluidPropertySource("hbm"));
    }
}
