package com.hbm.items.tool;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.items.ModDataComponents;
import com.hbm.util.BobMathUtil;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.ToolTier;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 1.7 {@code ItemSwordAbilityPower}: the HE-powered sword (Stunstick); each hit costs {@code consumption}. */
public class ItemSwordAbilityPower extends ItemSwordAbility implements IBatteryItem, dev.golem.core.world.HeldItemState {

    /** 1.7 kept the raised item while item and damage matched; the charge lived in NBT and never re-equipped it. */
    @Override
    public boolean compatIsSameHeldItem(ItemStack shown, ItemStack held) {
        return shown.is(held.getItem()) && shown.getDamageValue() == held.getDamageValue();
    }

    public long maxPower = 1;
    public long chargeRate;
    public long consumption;

    public ItemSwordAbilityPower(Properties properties, float damage, double movement, ToolTier material,
                                 long maxPower, long chargeRate, long consumption) {
        super(properties, damage, movement, material);
        this.maxPower = maxPower;
        this.chargeRate = chargeRate;
        this.consumption = consumption;
    }

    @Override
    public void chargeBattery(ItemStack stack, long i) {
        setCharge(stack, getCharge(stack) + i);
    }

    @Override
    public void setCharge(ItemStack stack, long i) {
        if (stack.getItem() instanceof ItemSwordAbilityPower) ModDataComponents.BATTERY_CHARGE.set(stack, i);
    }

    @Override
    public void dischargeBattery(ItemStack stack, long i) {
        if (stack.getItem() instanceof ItemSwordAbilityPower) setCharge(stack, Math.max(0, getCharge(stack) - i));
    }

    /** 1.7: a stack that never had a charge written is full. */
    @Override
    public long getCharge(ItemStack stack) {
        if (stack.getItem() instanceof ItemSwordAbilityPower power) {
            return ModDataComponents.BATTERY_CHARGE.getOr(stack, power.maxPower);
        }
        return 0;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        list.add(Component.literal("Charge: " + BobMathUtil.getShortNumber(getCharge(stack)) + " / "
                + BobMathUtil.getShortNumber(maxPower)));
        super.appendCompatHoverText(stack, context, list, flag);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getCharge(stack) < maxPower;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getCharge(stack) / (float) maxPower);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(Math.max(0.0F, (float) getCharge(stack) / maxPower) / 3.0F, 1.0F, 1.0F);
    }

    @Override
    protected boolean canOperate(ItemStack stack) {
        return getCharge(stack) >= this.consumption;
    }

    @Override
    protected void spendOnHit(ItemStack stack, LivingEntity attacker) {
        if (!attacker.level().isClientSide()) dischargeBattery(stack, consumption);
    }

    @Override
    public long getMaxCharge(ItemStack stack) {
        return maxPower;
    }

    @Override
    public long getChargeRate(ItemStack stack) {
        return chargeRate;
    }

    @Override
    public long getDischargeRate(ItemStack stack) {
        return 0;
    }
}
