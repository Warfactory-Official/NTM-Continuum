package com.hbm.items.machine;

import com.hbm.api.fluid.FluidType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/** An item that names a {@link FluidType}, used to set a tank's/pipe's fluid type. */
public interface IItemFluidId {

    FluidType getType(@Nullable Level world, int x, int y, int z, ItemStack stack);
}
