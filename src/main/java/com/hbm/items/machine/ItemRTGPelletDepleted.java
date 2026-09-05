package com.hbm.items.machine;

import dev.golem.core.world.CompatItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** One stable modern item identity for each old depleted-pellet metadata value. */
public final class ItemRTGPelletDepleted extends CompatItem {
    public final DepletedRTGMaterial material;

    public ItemRTGPelletDepleted(Properties properties, DepletedRTGMaterial material) {
        super(properties);
        this.material = material;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(material.displayName);
    }
}
