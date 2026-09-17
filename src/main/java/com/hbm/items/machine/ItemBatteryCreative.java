package com.hbm.items.machine;

import com.hbm.api.energymk2.IBatteryItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemBatteryCreative extends Item implements IBatteryItem {

    public ItemBatteryCreative(Properties props) {
        super(props);
    }

    @Override
    public void chargeBattery(ItemStack stack, long amount) {
    }

    @Override
    public void setCharge(ItemStack stack, long amount) {
    }

    @Override
    public void dischargeBattery(ItemStack stack, long amount) {
    }

    @Override
    public long getCharge(ItemStack stack) {
        return Long.MAX_VALUE / 2L;
    }

    @Override
    public long getMaxCharge(ItemStack stack) {
        return Long.MAX_VALUE;
    }

    @Override
    public long getChargeRate(ItemStack stack) {
        return Long.MAX_VALUE / 100L;
    }

    @Override
    public long getDischargeRate(ItemStack stack) {
        return Long.MAX_VALUE / 100L;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }
}
