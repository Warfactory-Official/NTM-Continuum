package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import net.minecraft.world.item.Item;

/** Battery-capacity multiplier used by powered armor. */
public final class ItemModBattery extends ItemArmorMod {
    public final double mod;

    public ItemModBattery(double mod, Item.Properties properties) {
        super(ArmorModHandler.battery, true, true, true, true, properties);
        this.mod = mod;
    }
}
