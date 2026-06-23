package com.hbm.items.machine;

import com.hbm.inventory.fluid.FluidType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public interface IItemFluidIdentifier {

    FluidType getType(@Nullable Level world, int x, int y, int z, ItemStack stack);
}
