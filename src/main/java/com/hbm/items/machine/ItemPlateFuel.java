package com.hbm.items.machine;

import com.hbm.api.data.ReactorDataMaps;
import com.hbm.items.ModDataComponents;
import com.hbm.lib.Library;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Research-reactor plate fuel, ported from the 1.12.2 ItemPlateFuel family. */
public class ItemPlateFuel extends CompatItem {

    public final int lifeTime;
    public final FunctionEnum function;
    public final int reactivity;

    public ItemPlateFuel(Properties properties, int lifeTime, FunctionEnum function, int reactivity) {
        super(properties);
        this.lifeTime = lifeTime;
        this.function = function;
        this.reactivity = reactivity;
    }

    /** The plate values: the {@code hbm:plate_fuel} entry, else the constructor's. */
    public ReactorDataMaps.PlateFuel stats() {
        ReactorDataMaps.PlateFuel data = ReactorDataMaps.PLATE_FUEL.get(this);
        return data != null ? data : codeStats();
    }

    /** The code defaults (constructor or enum values) that the data map entry overrides. */
    public ReactorDataMaps.PlateFuel codeStats() {
        return new ReactorDataMaps.PlateFuel(lifeTime, function, reactivity);
    }

    public int lifeTime() { return stats().lifeTime(); }

    public static void setLifeTime(ItemStack stack, int time) {
        ModDataComponents.plateFuelLife(stack, time);
    }

    public static int getLifeTime(ItemStack stack) {
        if (!ModDataComponents.PLATE_FUEL_LIFE.has(stack)) {
            // The old NBT getter created an empty tag as a side effect; retain that observable default.
            ModDataComponents.plateFuelLife(stack, 0);
        }
        return ModDataComponents.plateFuelLife(stack);
    }

    public double getDurabilityForDisplay(ItemStack stack) {
        return (double) getLifeTime(stack) / (double) lifeTime();
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getDurabilityForDisplay(stack) > 0D;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.max(0, Math.min(13, (int) Math.round(13D * getDurabilityForDisplay(stack))));
    }

    /** Applies one research-reactor flux sample, including the passive-source lifetime increment. */
    public int react(Level level, ItemStack stack, int flux) {
        var stats = stats();
        FunctionEnum function = stats.function();
        int reactivity = stats.reactivity();
        if (function != FunctionEnum.PASSIVE) {
            setLifeTime(stack, getLifeTime(stack) + flux);
        }

        return switch (function) {
            case LOGARITHM -> (int) (Math.log10(flux + 1D) * 0.5D * reactivity);
            case SQUARE_ROOT -> (int) (Math.sqrt(flux) * reactivity / 10D);
            case NEGATIVE_QUADRATIC -> (int) (Math.max((flux - (flux * flux / 10000D)) / 100D * reactivity, 0D));
            case LINEAR -> (int) (flux / 100D * reactivity);
            case PASSIVE -> {
                setLifeTime(stack, getLifeTime(stack) + reactivity);
                yield reactivity;
            }
        };
    }

    public String getFunctionDesc() {
        var stats = stats();
        int reactivity = stats.reactivity();
        return switch (stats.function()) {
            case LOGARITHM -> "f(x) = log10(x + 1) * 0.5 * " + reactivity;
            case SQUARE_ROOT -> "f(x) = sqrt(x) * " + reactivity + " / 10";
            case NEGATIVE_QUADRATIC -> "f(x) = [x - (x² / 10000)] / 100 * " + reactivity;
            case LINEAR -> "f(x) = x / 100 * " + reactivity;
            case PASSIVE -> "f(x) = " + reactivity;
        };
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                          List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("[Research Reactor Plate Fuel]").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("   " + getFunctionDesc()).withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.literal("   Yield of " + Library.getShortNumber(lifeTime()) + " events")
                .withStyle(ChatFormatting.DARK_AQUA));
    }

    public enum FunctionEnum {
        LOGARITHM,
        SQUARE_ROOT,
        NEGATIVE_QUADRATIC,
        LINEAR,
        PASSIVE
    }
}
