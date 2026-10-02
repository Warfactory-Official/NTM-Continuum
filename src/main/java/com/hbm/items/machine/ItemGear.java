package com.hbm.items.machine;

import com.hbm.items.ModDataComponents;
import net.minecraft.network.chat.Component;
import dev.golem.core.world.CompatItem;
import net.minecraft.world.item.ItemStack;

/** The two old metadata variants of the large gear, represented by ITEM_VARIANT. */
public class ItemGear extends CompatItem {

    public ItemGear(Properties properties) {
        super(properties);
    }

    public int getVariant(ItemStack stack) {
        return ModDataComponents.ITEM_VARIANT.getOr(stack, 0);
    }

    public ItemStack of(int variant) {
        ItemStack stack = new ItemStack(this);
        ModDataComponents.ITEM_VARIANT.set(stack, variant);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getVariant(stack) == 1
                ? "item.hbm.gear_large_steel"
                : "item.hbm.gear_large");
    }
}
