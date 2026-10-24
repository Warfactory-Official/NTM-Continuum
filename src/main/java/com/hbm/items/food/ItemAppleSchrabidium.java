package com.hbm.items.food;

import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code ItemAppleSchrabidium}, which served both the schrabidium and the lead apple: three metadata tiers
 * (crafted from nuggets, ingots or blocks) that brand alike - uncommon/rare/epic, the block tier glinting - and
 * then do entirely different things. The port registers one item per tier ({@code <id>}, {@code <id>_ingot},
 * {@code <id>_block}); meta 0 keeps the bare id so {@code new ItemStack(apple)} references still resolve. Each tier's
 * effects are its {@code hbm:consumable_treatment}.
 */
public class ItemAppleSchrabidium extends ItemModFood {

    private final int tier;

    public ItemAppleSchrabidium(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return tier == 2;
    }
}
