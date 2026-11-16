package com.hbm.items.machine;

import dev.golem.core.world.CompatItem;

/** One stable modern item identity for each old depleted-pellet metadata value. */
public final class ItemRTGPelletDepleted extends CompatItem {
    public final DepletedRTGMaterial material;

    public ItemRTGPelletDepleted(Properties properties, DepletedRTGMaterial material) {
        super(properties);
        this.material = material;
    }
}
