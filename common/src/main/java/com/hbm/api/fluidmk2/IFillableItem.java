package com.hbm.api.fluidmk2;

import com.hbm.inventory.fluid.FluidType;
import net.minecraft.world.item.ItemStack;

/** An item stack that can hold/transfer HBM fluids (canisters, barrels, etc.). */
public interface IFillableItem {

    /** Whether this stack can be filled with this type. */
    boolean acceptsFluid(FluidType type, ItemStack stack);

    /** Tries to fill the stack, returns the remainder that couldn't be added. */
    int tryFill(FluidType type, int amount, ItemStack stack);

    /** Whether this stack can fill tiles with this type. */
    boolean providesFluid(FluidType type, ItemStack stack);

    /** Provides fluid with the maximum being the requested amount. */
    int tryEmpty(FluidType type, int amount, ItemStack stack);

    /** Returns the first (or only) currently held type, may return null. */
    FluidType getFirstFluidType(ItemStack stack);

    /** Returns the fill state for the specified fluid. */
    int getFill(ItemStack stack);
}
