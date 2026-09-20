package com.hbm.items.special;

import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

/**
 * The exotic thing a dark fusion core reacts around. Each one sets the multiplier the core applies to
 * the beam energy it converts, and how much heat and fuel that costs.
 */
public class ItemAMSCore extends ItemCustomLore {

    private final int powerBase;
    private final float heatBase;
    private final float fuelBase;
    private final boolean glintsOnPolaroid;

    public ItemAMSCore(int powerBase, float heatBase, float fuelBase, Properties properties) {
        this(powerBase, heatBase, fuelBase, false, properties);
    }

    public ItemAMSCore(int powerBase, float heatBase, float fuelBase, boolean glintsOnPolaroid,
                       Properties properties) {
        super(properties);
        this.powerBase = powerBase;
        this.heatBase = heatBase;
        this.fuelBase = fuelBase;
        this.glintsOnPolaroid = glintsOnPolaroid;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        super.appendCompatHoverText(stack, context, tooltip, flag);

        tooltip.add(Component.literal("[DFC Core]").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.literal(" Power: " + Library.getShortNumber(powerBase))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(percent(" Heat: ", heatBase));
        tooltip.add(percent(" Fuel: ", fuelBase));
    }

    private static Component percent(String label, float base) {
        int tenths = Math.round(base * 1000);
        String value = String.format(Locale.US, "%+.1f%%", tenths * 0.1D - 100D);
        return Component.literal(label).withStyle(ChatFormatting.AQUA)
                .append(Component.literal(value).withStyle(base > 1 ? ChatFormatting.RED : ChatFormatting.GREEN));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return glintsOnPolaroid && MainRegistry.polaroidID == 11;
    }

    public static int getPowerBase(ItemStack stack) {
        return stack.getItem() instanceof ItemAMSCore core ? core.powerBase : 0;
    }

    public static float getHeatBase(ItemStack stack) {
        return stack.getItem() instanceof ItemAMSCore core ? core.heatBase : 1F;
    }

    public static float getFuelBase(ItemStack stack) {
        return stack.getItem() instanceof ItemAMSCore core ? core.fuelBase : 1F;
    }
}
