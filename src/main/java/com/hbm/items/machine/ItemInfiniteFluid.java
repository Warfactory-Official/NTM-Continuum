package com.hbm.items.machine;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IInfiniteFluidItem;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * The 1.7 {@code ItemInfiniteFluid}: {@code inf_water} (50 mB/t of water), {@code inf_water_mk2}
 * (500 mB/t), {@code chlorine_pinwheel} (1 mB of chlorine one tick in two) and
 * {@code fluid_barrel_infinite} (a billion mB of anything, and the only one that also drains a
 * pressurised tank). See {@link IInfiniteFluidItem} for how a tank reads it.
 *
 * <p>Also an {@link IFillableItem} that provides its fluid without ever emptying, so the machine slot
 * validators that only know fillables (the fluid tank's load slot) accept it. The infinite barrel names
 * no fluid, so those validators still turn it away; it works from the barrel and the container-IO path.
 */
public class ItemInfiniteFluid extends CompatItem implements IInfiniteFluidItem, IFillableItem {

    private final @Nullable Supplier<FluidType> type;
    private final int amount;
    private final int chance;
    private final boolean anyPressure;

    public ItemInfiniteFluid(Properties properties, @Nullable Supplier<FluidType> type, int amount, int chance, boolean anyPressure) {
        super(properties);
        this.type = type;
        this.amount = amount;
        this.chance = chance;
        this.anyPressure = anyPressure;
    }

    @Override public @Nullable FluidType infiniteType() { return type == null ? null : type.get(); }
    @Override public int infiniteAmount() { return amount; }
    @Override public int infiniteChance() { return chance; }
    @Override public boolean allowPressure(int pressure) { return anyPressure || pressure == 0; }

    // --- IFillableItem: a bottomless source that never fills ---

    @Override public boolean acceptsFluid(FluidType type, ItemStack stack) { return false; }
    @Override public int tryFill(FluidType type, int amount, ItemStack stack) { return amount; }

    @Override
    public boolean providesFluid(FluidType type, ItemStack stack) {
        FluidType mine = infiniteType();
        return type != null && type != Fluids.NONE && (mine == null || mine == type);
    }

    @Override
    public int tryEmpty(FluidType type, int amount, ItemStack stack) {
        return providesFluid(type, stack) ? Math.min(amount, this.amount) : 0;
    }

    @Override
    public int getFill(ItemStack stack) {
        return amount;
    }

    @Override
    public FluidType getFirstFluidType(ItemStack stack) {
        FluidType mine = infiniteType();
        return mine == null ? Fluids.NONE : mine;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FluidType mine = infiniteType();
        String rate = chance <= 1 ? amount + " mB/t" : amount + " mB every " + chance + " ticks";
        tooltip.add(Component.literal((mine == null ? "Any fluid" : mine.getDisplayNameString()) + ", " + rate).withStyle(ChatFormatting.GRAY));
    }
}
