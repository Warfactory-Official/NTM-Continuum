package com.hbm.items.machine;

import com.hbm.items.ModDataComponents;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A single active, component-backed ZIRNOX fuel variant. */
public class ItemZirnoxRod extends CompatItem {

    private final ZirnoxFuel fuel;

    public ItemZirnoxRod(Properties properties, ZirnoxFuel fuel) {
        super(properties);
        this.fuel = fuel;
    }

    public ZirnoxFuel fuel() {
        return fuel;
    }

    public int getLife(ItemStack stack) {
        return ModDataComponents.zirnoxLife(stack);
    }

    public void setLife(ItemStack stack, int life) {
        ModDataComponents.zirnoxLife(stack, Math.max(0, life));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getLife(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.max(0, Math.round(13F * (1F - Math.min(1F, (float) getLife(stack) / fuel.maxLife))));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x53C653;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         java.util.List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.hbm.zirnox_rod.life", getLife(stack), fuel.maxLife)
                .withStyle(ChatFormatting.GRAY));
        if (fuel.breeding) {
            tooltip.add(Component.translatable("desc.item.zirnoxBreedingRod").withStyle(ChatFormatting.GOLD));
        }
    }
}
