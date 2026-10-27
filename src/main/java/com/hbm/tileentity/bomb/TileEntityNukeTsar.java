package com.hbm.tileentity.bomb;

import com.hbm.inventory.container.ContainerNukeTsar;
import com.hbm.items.ModBombItems;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Tsar Bomba's four lenses, conventional core, and optional Tsar upgrade core. */
public class TileEntityNukeTsar extends TileEntityNukeBomb {

    public TileEntityNukeTsar(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUKE_TSAR.get(), pos, state, 6);
    }

    /** 1.7 getInventoryStackLimit. */
    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case 0, 1, 2, 3 -> stack.getItem() == ModBombItems.explosive_lenses.get();
            case 4 -> stack.getItem() == ModBombItems.man_core.get();
            case 5 -> stack.getItem() == ModBombItems.tsar_core.get();
            default -> false;
        };
    }

    @Override
    public boolean isReady() {
        return isItemValidForSlot(0, inventory.getStackInSlot(0))
                && isItemValidForSlot(1, inventory.getStackInSlot(1))
                && isItemValidForSlot(2, inventory.getStackInSlot(2))
                && isItemValidForSlot(3, inventory.getStackInSlot(3))
                && isItemValidForSlot(4, inventory.getStackInSlot(4));
    }

    public boolean isFilled() {
        return isReady() && isItemValidForSlot(5, inventory.getStackInSlot(5));
    }

    @Override
    public String getDefaultName() {
        return "container.nukeTsar";
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerNukeTsar(containerId, inventory, this);
    }
}
