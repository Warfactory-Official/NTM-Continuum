package com.hbm.items.machine;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.items.ModDataComponents;
import com.hbm.lib.Library;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 1.7 {@code ItemBattery}: a plain HE battery item (the potato battery, PotatOS, the Electronium cube). The charge
 * is the {@link ModDataComponents#BATTERY_CHARGE} component, and like 1.7's NBT an absent charge reads as
 * <em>full</em> - a crafted battery is {@code getFullBattery} without having to carry the number. Setting a
 * charge writes it, so an emptied battery stays empty.
 */
public class ItemBattery extends CompatItem implements IBatteryItem {

    protected final long maxCharge;
    protected final long chargeRate;
    protected final long dischargeRate;

    public ItemBattery(Properties properties, long maxCharge, long chargeRate, long dischargeRate) {
        super(properties);
        this.maxCharge = maxCharge;
        this.chargeRate = chargeRate;
        this.dischargeRate = dischargeRate;
    }

    @Override
    public void chargeBattery(ItemStack stack, long i) {
        setCharge(stack, getCharge(stack) + i);
    }

    @Override
    public void setCharge(ItemStack stack, long i) {
        ModDataComponents.BATTERY_CHARGE.set(stack, i);
    }

    @Override
    public void dischargeBattery(ItemStack stack, long i) {
        setCharge(stack, getCharge(stack) - i);
    }

    @Override
    public long getCharge(ItemStack stack) {
        return ModDataComponents.BATTERY_CHARGE.get(stack).orElse(maxCharge);
    }

    @Override
    public long getMaxCharge(ItemStack stack) {
        return maxCharge;
    }

    @Override
    public long getChargeRate(ItemStack stack) {
        return chargeRate;
    }

    @Override
    public long getDischargeRate(ItemStack stack) {
        return dischargeRate;
    }

    /** 1.7 {@code getEmptyBattery}. */
    public ItemStack emptyStack() {
        ItemStack stack = new ItemStack(this);
        setCharge(stack, 0);
        return stack;
    }

    // 1.7 always shows the durability bar as the charge level
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * (float) ((double) getCharge(stack) / (double) maxCharge));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float f = Math.max(0.0F, (float) ((double) getCharge(stack) / (double) maxCharge));
        return net.minecraft.util.Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        tooltip.add(Component.literal("Energy stored: " + Library.getShortNumber(getCharge(stack)) + "/"
                + Library.getShortNumber(maxCharge) + "HE"));
        tooltip.add(Component.literal("Charge rate: " + Library.getShortNumber(chargeRate) + "HE/t"));
        tooltip.add(Component.literal("Discharge rate: " + Library.getShortNumber(dischargeRate) + "HE/t"));
    }
}
