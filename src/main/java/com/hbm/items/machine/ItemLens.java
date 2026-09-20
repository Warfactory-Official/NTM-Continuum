package com.hbm.items.machine;

import com.hbm.items.ModDataComponents;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

/**
 * The stabiliser's focusing lens. It burns through at the rate the stabiliser is run, and the wear is a
 * whole number of ticks rather than vanilla durability, so it lives on the stack as its own component
 * and drives the bar directly.
 */
public class ItemLens extends CompatItem {

    public final long maxDamage;

    public ItemLens(long maxDamage, Properties properties) {
        super(properties);
        this.maxDamage = maxDamage;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        long damage = getLensDamage(stack);
        double percent = (int) ((maxDamage - damage) * 100_000_000D / maxDamage) / 1_000_000D;

        tooltip.add(Component.translatable("desc.durticks")
                .append(Component.literal(" " + (maxDamage - damage) + " / " + maxDamage))
                .withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("desc.durpercents")
                .append(Component.literal(" " + String.format(Locale.US, "%s", percent) + "%"))
                .withStyle(ChatFormatting.DARK_AQUA));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getLensDamage(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) Math.round(13D * (maxDamage - getLensDamage(stack)) / maxDamage);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x00FFFF;
    }

    public static long getLensDamage(ItemStack stack) {
        return ModDataComponents.AMS_LENS_DAMAGE.getOr(stack, 0L);
    }

    public static void setLensDamage(ItemStack stack, long damage) {
        ModDataComponents.AMS_LENS_DAMAGE.set(stack, damage);
    }
}
