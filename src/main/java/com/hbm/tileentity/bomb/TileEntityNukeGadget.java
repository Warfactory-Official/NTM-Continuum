package com.hbm.tileentity.bomb;

import com.hbm.inventory.container.ContainerNukeGadget;
import com.hbm.items.ModBombItems;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** The Gadget's wiring, four implosion lenses, and plutonium core slots. */
public class TileEntityNukeGadget extends TileEntityNukeBomb {

    public TileEntityNukeGadget(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUKE_GADGET.get(), pos, state, 6);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> stack.getItem() == ModBombItems.gadget_wireing.get();
            case 1, 2, 3, 4 -> stack.getItem() == ModBombItems.early_explosive_lenses.get();
            case 5 -> stack.getItem() == ModBombItems.gadget_core.get();
            default -> false;
        };
    }

    public boolean exp1() { return isLens(1); }
    public boolean exp2() { return isLens(2); }
    public boolean exp3() { return isLens(3); }
    public boolean exp4() { return isLens(4); }

    private boolean isLens(int slot) {
        return inventory.getStackInSlot(slot).getItem() == ModBombItems.early_explosive_lenses.get();
    }

    @Override
    public boolean isReady() {
        return exp1() && exp2() && exp3() && exp4()
                && isItemValidForSlot(0, inventory.getStackInSlot(0))
                && isItemValidForSlot(5, inventory.getStackInSlot(5));
    }

    @Override
    public String getDefaultName() {
        return "container.nukeGadget";
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerNukeGadget(containerId, inventory, this);
    }
}
