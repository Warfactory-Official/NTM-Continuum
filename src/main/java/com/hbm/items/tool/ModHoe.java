package com.hbm.items.tool;

import dev.golem.core.world.CompatHoeItem;
import dev.golem.core.world.ToolTier;
import dev.golem.core.world.Tools;

/**
 * 1.7 {@code ModHoe}: a plain hoe of an HBM tool material. 1.7 hoes carried no attack modifiers, so the
 * main-hand attack modifier is zero; the swing speed is vanilla's hoe speed for the tier.
 */
public class ModHoe extends CompatHoeItem {

    public ModHoe(Properties properties, ToolTier material) {
        super(properties, material, 0.0F, Tools.hoeSpeed(material.level()));
    }
}
