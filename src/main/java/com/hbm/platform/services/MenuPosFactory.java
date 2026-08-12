package com.hbm.platform.services;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Client-side menu factory that receives the {@link BlockPos} sent from the server when a menu is
 * opened. Used by {@link IPlatformHelper#menuTypeWithPos} to build a loader-appropriate MenuType
 * that carries a block position as its open-data.
 */
@FunctionalInterface
public interface MenuPosFactory<T extends AbstractContainerMenu> {
    T create(int containerId, Inventory playerInventory, BlockPos pos);
}
