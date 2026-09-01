package com.hbm.items.machine;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The spent metadata variants emitted when a fresh arc electrode is exhausted. */
public class ItemArcElectrodeBurnt extends dev.golem.core.world.CompatItem {

    public final ItemArcElectrode.ElectrodeType type;

    public ItemArcElectrodeBurnt(Item.Properties properties, ItemArcElectrode.ElectrodeType type) {
        super(properties);
        this.type = type;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("Molten " + type.displayName);
    }
}
