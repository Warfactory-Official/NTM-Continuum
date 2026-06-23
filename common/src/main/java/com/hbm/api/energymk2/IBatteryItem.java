package com.hbm.api.energymk2;

import net.minecraft.world.item.ItemStack;

public interface IBatteryItem {

    void chargeBattery(ItemStack stack, long i);

    void setCharge(ItemStack stack, long i);

    void dischargeBattery(ItemStack stack, long i);

    long getCharge(ItemStack stack);

    long getMaxCharge(ItemStack stack);

    long getChargeRate(ItemStack stack);

    long getDischargeRate(ItemStack stack);
}
