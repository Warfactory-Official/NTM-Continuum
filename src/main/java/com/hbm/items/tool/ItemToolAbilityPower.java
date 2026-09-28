package com.hbm.items.tool;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.items.ModDataComponents;
import com.hbm.util.BobMathUtil;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.ToolTier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 1.7 {@code ItemToolAbilityPower}: an ability tool that runs on HE. Every point of wear the tool would
 * take (a mined block, a hit) costs {@code consumption} HE instead; below that it stops working.
 */
public class ItemToolAbilityPower extends ItemToolAbility implements IBatteryItem {

    public long maxPower = 1;
    public long chargeRate;
    public long consumption;

    public ItemToolAbilityPower(Properties properties, float damage, double movement, ToolTier material,
                                EnumToolType type, long maxPower, long chargeRate, long consumption) {
        super(properties, damage, movement, material, type);
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
        if (stack.getItem() instanceof ItemToolAbilityPower) ModDataComponents.BATTERY_CHARGE.set(stack, i);
    }

    @Override
    public void dischargeBattery(ItemStack stack, long i) {
        if (stack.getItem() instanceof ItemToolAbilityPower) {
            setCharge(stack, Math.max(0, getCharge(stack) - i));
        }
    }

    /** 1.7: a stack that never had a charge written is full. */
    @Override
    public long getCharge(ItemStack stack) {
        if (stack.getItem() instanceof ItemToolAbilityPower power) {
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
    public boolean canOperate(ItemStack stack) {
        return getCharge(stack) >= this.consumption;
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

    /** 1.7 {@code setDamage}: every point of wear is {@code consumption} HE. */
    @Override
    public void damageTool(ItemStack stack, int amount, LivingEntity user) {
        dischargeBattery(stack, amount * consumption);
    }

    @Override
    protected void spendOnHit(ItemStack stack, LivingEntity attacker) {
        if (!attacker.level().isClientSide()) damageTool(stack, 1, attacker);
    }

    /** 1.7 {@code ItemTool.onBlockDestroyed}: one point of wear for any block that is not instant to break. */
    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        if (!level.isClientSide() && state.getDestroySpeed(level, pos) != 0.0F) damageTool(stack, 1, owner);
        return true;
    }
}
