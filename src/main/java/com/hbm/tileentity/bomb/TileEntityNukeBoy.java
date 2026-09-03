package com.hbm.tileentity.bomb;

import com.hbm.inventory.container.ContainerNukeBoy;
import com.hbm.items.ModBombItems;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Little Boy's ordered shielding, target, projectile, propellant, and igniter slots. */
public class TileEntityNukeBoy extends TileEntityNukeBomb {

    public TileEntityNukeBoy(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUKE_BOY.get(), pos, state, 5);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> stack.getItem() == ModBombItems.boy_shielding.get();
            case 1 -> stack.getItem() == ModBombItems.boy_target.get();
            case 2 -> stack.getItem() == ModBombItems.boy_bullet.get();
            case 3 -> stack.getItem() == ModBombItems.boy_propellant.get();
            case 4 -> stack.getItem() == ModBombItems.boy_igniter.get();
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

    @Override
    public String getDefaultName() {
        return "container.nukeBoy";
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerNukeBoy(containerId, inventory, this);
    }
}
