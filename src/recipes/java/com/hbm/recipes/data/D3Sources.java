package com.hbm.recipes.data;

import dev.golem.core.world.DataSource;
import java.util.List;

/** Data agent D3's sources: the 1.7 item pools, the structure loot tables made from them, and the mob loot tables. */
public final class D3Sources {

    private D3Sources() {
    }

    public static List<DataSource<?>> sources() {
        return List.of(new ItemPoolSource(), new ChestLootSource(), new EntityLootSource());
    }
}
