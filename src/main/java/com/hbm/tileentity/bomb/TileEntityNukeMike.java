package com.hbm.tileentity.bomb;

import com.hbm.inventory.container.ContainerNukeMike;
import com.hbm.items.ModBombItems;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Ivy Mike's four lenses, Fat Man core, secondary vessel, deuterium vessel, and cooling unit. */
public class TileEntityNukeMike extends TileEntityNukeBomb {

    public TileEntityNukeMike(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUKE_MIKE.get(), pos, state, 8);
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
            case 5 -> stack.getItem() == ModBombItems.mike_core.get();
            case 6 -> stack.getItem() == ModBombItems.mike_deut.get();
            case 7 -> stack.getItem() == ModBombItems.mike_cooling_unit.get();
            default -> false;
        };
    }

    @Override
    public boolean isReady() {
        for (int slot = 0; slot <= 4; slot++) {
            if (!isItemValidForSlot(slot, inventory.getStackInSlot(slot))) return false;
        }
        return true;
    }

    public boolean isFilled() {
        if (!isReady()) return false;
        for (int slot = 5; slot < 8; slot++) {
            if (!isItemValidForSlot(slot, inventory.getStackInSlot(slot))) return false;
        }
        return true;
    }

    @Override
    public String getDefaultName() {
        return "container.nukeMike";
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerNukeMike(containerId, inventory, this);
    }
}
