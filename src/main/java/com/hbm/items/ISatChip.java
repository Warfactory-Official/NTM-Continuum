package com.hbm.items;

import net.minecraft.world.item.ItemStack;

/**
 * An item that carries a satellite frequency. The frequency is what addresses a satellite in orbit, so a
 * chip is how a machine on the ground says which one it is talking to.
 *
 * <p>Upstream keeps the value in the stack's NBT; here it rides {@link ModDataComponents#SAT_FREQUENCY}.
 */
public interface ISatChip {

    static int getFreqS(ItemStack stack) {
        if (stack != null && stack.getItem() instanceof ISatChip chip) return chip.getFreq(stack);
        return 0;
    }

    static void setFreqS(ItemStack stack, int freq) {
        if (stack != null && stack.getItem() instanceof ISatChip chip) chip.setFreq(stack, freq);
    }

    default int getFreq(ItemStack stack) {
        return ModDataComponents.SAT_FREQUENCY.getOr(stack, 0);
    }

    default void setFreq(ItemStack stack, int freq) {
        ModDataComponents.SAT_FREQUENCY.set(stack, freq);
    }
}
