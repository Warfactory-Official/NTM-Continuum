package com.hbm.util;

import dev.golem.core.machine.ItemHandler;
import com.hbm.items.machine.ItemRTGPellet;
import net.minecraft.world.item.ItemStack;

/** Shared pellet-output and per-tick depletion operations from the legacy RTG utility. */
public final class RTGUtil {
    private RTGUtil() {}

    public static short getPower(ItemRTGPellet fuel, ItemStack stack) {
        return ItemRTGPellet.getScaledPower(fuel, stack);
    }

    public static boolean hasHeat(ItemHandler inventory) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (inventory.getStackInSlot(slot).getItem() instanceof ItemRTGPellet) return true;
        }
        return false;
    }

    public static int updateRTGs(ItemHandler inventory, int[] slots) {
        int heat = 0;
        for (int slot : slots) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!(stack.getItem() instanceof ItemRTGPellet pellet)) continue;
            heat += getPower(pellet, stack);
            inventory.setStackInSlot(slot, ItemRTGPellet.handleDecay(stack, pellet));
        }
        return heat;
    }
}
