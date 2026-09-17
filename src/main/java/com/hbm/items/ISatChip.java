package com.hbm.items;

import net.minecraft.world.item.ItemStack;
import dev.golem.core.data.Tags;
import net.minecraft.nbt.CompoundTag;

/**
 * 1.7 satellite-chip frequency contract. 1.7 "freq" NBT field lives in PERSISTENT_DATA.
 */
public interface ISatChip {

    static int getFreqS(ItemStack stack) {
        return stack.getItem() instanceof ISatChip chip ? chip.getFreq(stack) : 0;
    }

    static void setFreqS(ItemStack stack, int freq) {
        if (stack.getItem() instanceof ISatChip chip) chip.setFreq(stack, freq);
    }

    default int getFreq(ItemStack stack) {
        CompoundTag data = ModDataComponents.PERSISTENT_DATA.get(stack).orElse(null);
        return data == null ? 0 : Tags.getIntOr(data, "freq", 0);
    }

    default void setFreq(ItemStack stack, int freq) {
        ModDataComponents.PERSISTENT_DATA.update(stack, new CompoundTag(), tag -> {
            CompoundTag copy = tag.copy();
            copy.putInt("freq", freq);
            return copy;
        });
    }
}
