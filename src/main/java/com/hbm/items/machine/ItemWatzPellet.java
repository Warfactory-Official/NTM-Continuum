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
 * Watz Isotropic Fuel, Oxidized — one pellet type of the Watz power plant, either fresh or depleted.
 *
 * <p>1.12.2 packed all twelve types into one item via metadata and recoloured the sprite at runtime;
 * 1.21+ has no metadata, so the port registers <b>one item per type × freshness</b> with a pre-baked
 * texture, the same shape {@code ItemAutogen} uses. Remaining yield rides
 * {@link ModDataComponents#WATZ_YIELD} instead of the old NBT {@code "yield"} double.
 */
public class ItemWatzPellet extends CompatItem {

    public final EnumWatzType type;
    public final boolean depleted;

    public ItemWatzPellet(Properties properties, EnumWatzType type, boolean depleted) {
        super(properties);
        this.type = type;
        this.depleted = depleted;
    }

    /** A full-yield pellet of this type. */
    public ItemStack make() {
        ItemStack stack = new ItemStack(this);
        setYield(stack, type.yield);
        return stack;
    }

    public static double getYield(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemWatzPellet pellet)) return 0D;
        // Absence means "untouched", so a freshly crafted pellet needs no component to be full.
        return ModDataComponents.WATZ_YIELD.getOr(stack, pellet.type.yield);
    }

    public static void setYield(ItemStack stack, double yield) {
        ModDataComponents.WATZ_YIELD.set(stack, yield);
    }

    /** Remaining fraction of the pellet's original yield, 1 = fresh. */
    public static double getEnrichment(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemWatzPellet pellet)) return 0D;
        return getYield(stack) / pellet.type.yield;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return !depleted && getEnrichment(stack) < 1D;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.max(0, Math.round((float) getEnrichment(stack) * 13F));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return type.colorLight;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        if (depleted) return;

        tooltip.add(Component.literal("Depletion: "
                + String.format(Locale.US, "%.1f", (1D - getEnrichment(stack)) * 100D) + "%")
                .withStyle(ChatFormatting.GREEN));

        if (type.passive > 0) {
            tooltip.add(gold("Base fission rate: ", String.valueOf(type.passive)));
            tooltip.add(Component.literal("Self-igniting!").withStyle(ChatFormatting.RED));
        }
        if (type.heatEmission > 0) tooltip.add(gold("Heat per flux: ", type.heatEmission + " TU"));
        if (type.burnFunc != null) {
            tooltip.add(gold("Reaction function: ", type.burnFunc.getLabelForFuel()));
            tooltip.add(gold("Fuel type: ", type.burnFunc.getDangerFromFuel()));
        }
        if (type.heatDiv != null) tooltip.add(gold("Thermal multiplier: ", type.heatDiv.getLabelForFuel() + " TU\u207b\u00b9"));
        if (type.absorbFunc != null) tooltip.add(gold("Flux capture: ", type.absorbFunc.getLabelForFuel()));
    }

    /** A gold label followed by an unstyled value, the 1.12.2 {@code GOLD + label + RESET + value}. */
    private static Component gold(String label, String value) {
        return Component.literal(label).withStyle(ChatFormatting.GOLD)
                .append(Component.literal(value).withStyle(ChatFormatting.RESET));
    }
}
