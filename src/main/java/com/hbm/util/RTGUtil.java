package com.hbm.util;

import com.hbm.config.MachineConfig;
import com.hbm.inventory.NTMItemHandler;
import com.hbm.items.machine.ItemRTGPellet;
import net.minecraft.world.item.ItemStack;

/** Shared pellet-output and per-tick depletion operations from the legacy RTG utility. */
public final class RTGUtil {
    private RTGUtil() {}

    public enum HalfLifeType { LONG, MEDIUM, SHORT }

    /** 1.7 RTG lifespan conversion; its float arithmetic is part of the authored pellet lifetime. */
    public static long getLifespan(float halfLife, HalfLifeType type, boolean realYears) {
        float life = switch (type) {
            case LONG -> (48000 * (realYears ? 365 : 100) * 100) * halfLife;
            case MEDIUM -> (48000 * (realYears ? 365 : 100)) * halfLife;
            case SHORT -> 48000 * halfLife;
        };
        return (long) life;
    }

    public static short getPower(ItemRTGPellet fuel, ItemStack stack) {
        // 1.7: the output only falls with decay when 9.00_scaleRTGPower (or 528 mode) is on; the default is flat heat
        return MachineConfig.scaleRTGPowerActive() ? ItemRTGPellet.getScaledPower(fuel, stack) : fuel.getHeat();
    }

    public static boolean hasHeat(NTMItemHandler inventory) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (inventory.getStackInSlot(slot).getItem() instanceof ItemRTGPellet) return true;
        }
        return false;
    }

    public static int updateRTGs(NTMItemHandler inventory, int[] slots) {
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
