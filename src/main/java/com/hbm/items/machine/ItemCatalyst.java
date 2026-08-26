package com.hbm.items.machine;

import java.util.List;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** AMS reaction catalyst data from the 1.12.2 {@code ItemCatalyst}. */
public class ItemCatalyst extends CompatItem {

    public final int color;
    public final long powerAbs;
    public final float powerMod;
    public final float heatMod;
    public final float fuelMod;

    public ItemCatalyst(Item.Properties properties, int color, long powerAbs, float powerMod,
                        float heatMod, float fuelMod) {
        super(properties);
        this.color = color;
        this.powerAbs = powerAbs;
        this.powerMod = powerMod;
        this.heatMod = heatMod;
        this.fuelMod = fuelMod;
    }

    public int getColor() {
        return color;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Adds spice to the core."));
        tooltip.add(Component.literal("Look at all those colors!"));
    }

    public static long getPowerAbs(ItemStack stack) {
        return stack.getItem() instanceof ItemCatalyst catalyst ? catalyst.powerAbs : 0L;
    }

    public static float getPowerMod(ItemStack stack) {
        return stack.getItem() instanceof ItemCatalyst catalyst ? catalyst.powerMod : 1F;
    }

    public static float getHeatMod(ItemStack stack) {
        return stack.getItem() instanceof ItemCatalyst catalyst ? catalyst.heatMod : 1F;
    }

    public static float getFuelMod(ItemStack stack) {
        return stack.getItem() instanceof ItemCatalyst catalyst ? catalyst.fuelMod : 1F;
    }
}
