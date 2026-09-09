package com.hbm.items;

import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * An item whose subtypes are the constants of an enum. 1.12.2 selects them with item damage; here the
 * selector is {@link ModDataComponents#ITEM_VARIANT}, so the whole family stays one registered item
 * instead of becoming one item per variant.
 */
public class ItemEnumMulti extends dev.golem.core.world.CompatItem {

    public final Class<? extends Enum<?>> theEnum;
    public final boolean multiName;
    public final boolean multiTexture;

    public ItemEnumMulti(Class<? extends Enum<?>> theEnum, boolean multiName, boolean multiTexture,
                         Properties properties) {
        super(properties);
        this.theEnum = theEnum;
        this.multiName = multiName;
        this.multiTexture = multiTexture;
    }

    public int getVariant(ItemStack stack) {
        return ModDataComponents.ITEM_VARIANT.getOr(stack, 0);
    }

    public Enum<?> getType(ItemStack stack) {
        Enum<?>[] values = theEnum.getEnumConstants();
        return values[Math.abs(getVariant(stack)) % values.length];
    }

    public ItemStack of(Enum<?> type, int count) {
        ItemStack stack = new ItemStack(this, count);
        ModDataComponents.ITEM_VARIANT.set(stack, type.ordinal());
        return stack;
    }

    public ItemStack of(Enum<?> type) {
        return of(type, 1);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (multiName) {
            return Component.translatable(this.getDescriptionId() + "." + getType(stack).name().toLowerCase(Locale.US));
        }
        return super.getName(stack);
    }
}
