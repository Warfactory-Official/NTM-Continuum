package com.hbm.api.energymk2;

import net.minecraft.world.item.ItemStack;

/**
 * An item that stores HE (HBM Energy) and can be charged/discharged in a battery socket or by a
 * portable charger. Ported from 1.12.2; the only structural change is storage: the old mod kept the
 * charge in an NBT {@code "charge"} long, while the 26.1 port uses the
 * {@link com.hbm.items.ModDataComponents#BATTERY_CHARGE} data component (implemented by
 * {@link com.hbm.items.machine.ItemBatteryPack}).
 *
 * <p>Do not use {@code instanceof} checks on this interface directly in hot paths. The 1.12.2
 * guidance was to route through {@code Library.isBattery}; a plain {@code instanceof} on the item is fine
 * here since there is a single implementation.
 */
public interface IBatteryItem {

    /** Adds energy to the battery item, clamped to {@link #getMaxCharge(ItemStack)}. */
    void chargeBattery(ItemStack stack, long i);

    /** Sets the stored energy to an absolute value, clamped to {@link #getMaxCharge(ItemStack)}. */
    void setCharge(ItemStack stack, long i);

    /** Removes energy from the battery item, clamped to 0. */
    void dischargeBattery(ItemStack stack, long i);

    /** Current stored energy in HE. */
    long getCharge(ItemStack stack);

    /** Maximum energy this item can store, in HE. */
    long getMaxCharge(ItemStack stack);

    /** Maximum rate at which this item can receive energy, in HE/tick. */
    long getChargeRate(ItemStack stack);

    /** Maximum rate at which this item can provide energy, in HE/tick. */
    long getDischargeRate(ItemStack stack);
}
