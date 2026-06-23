package com.hbm.platform.services;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

@FunctionalInterface
public interface MenuPosFactory<T extends AbstractContainerMenu> {
    T create(int containerId, Inventory playerInventory, BlockPos pos);
}
