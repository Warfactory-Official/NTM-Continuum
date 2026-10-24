package com.hbm.items.food;

import net.minecraft.world.item.ItemStack;

/** 1.7 {@code ItemAppleEuphemium}: always edible, always glinting, epic, and effectively invulnerability. */
public class ItemAppleEuphemium extends ItemModFood {

    public ItemAppleEuphemium(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
