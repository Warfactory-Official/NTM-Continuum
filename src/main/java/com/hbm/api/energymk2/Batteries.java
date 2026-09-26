package com.hbm.api.energymk2;

import com.hbm.lib.Library;
import net.minecraft.world.item.ItemStack;

/** {@link IBatteryItem} stacks as a machine's power source or sink; rates are the battery's own. */
public final class Batteries {

    private Batteries() {}

    public static boolean isBattery(ItemStack stack) {
        return Library.isBattery(stack);
    }

    /** Takes up to {@code wanted} HE out of {@code battery}; returns what it gave. */
    public static long discharge(ItemStack battery, long wanted) {
        return Library.dischargeBatteryIfValid(battery, wanted, false);
    }

    /** Puts up to {@code offered} HE into {@code battery}; returns what it took. */
    public static long charge(ItemStack battery, long offered) {
        return Library.chargeBatteryIfValid(battery, offered, false);
    }
}
