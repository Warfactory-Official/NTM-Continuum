package com.hbm.api.fluidmk2;

import com.hbm.inventory.fluid.FluidType;
import net.minecraft.world.item.ItemStack;

public interface IFillableItem {

    boolean acceptsFluid(FluidType type, ItemStack stack);

    int tryFill(FluidType type, int amount, ItemStack stack);

    boolean providesFluid(FluidType type, ItemStack stack);

    int tryEmpty(FluidType type, int amount, ItemStack stack);

    FluidType getFirstFluidType(ItemStack stack);

    int getFill(ItemStack stack);
}
