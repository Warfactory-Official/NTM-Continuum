package com.hbm.api.conveyor;

import net.minecraft.world.item.ItemStack;

import java.util.List;

// 1.7 api.hbm.conveyor.IConveyorPackage; the ItemStack[] became a List, 26.1 having no null stacks to hole it.
public interface IConveyorPackage {

    List<ItemStack> getItemStacks();
}
