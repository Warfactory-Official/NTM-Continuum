package com.hbm.recipes;

import com.hbm.recipes.data.FluidPropertySource;
import dev.golem.core.world.DataSource;
import dev.golem.core.world.DataSources;
import java.util.List;

/**
 * HBM's files in the {@code minecraft} namespace: the {@code fluid_property} files of vanilla's empty, water and lava,
 * which are 1.7's NONE, WATER and LAVA (1.7 {@code Fluids.init}).
 */
public final class MinecraftDataSources implements DataSources {

    @Override
    public String namespace() {
        return "minecraft";
    }

    @Override
    public List<DataSource<?>> sources() {
        return List.of(new FluidPropertySource("minecraft"));
    }
}
