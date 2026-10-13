package com.hbm.tileentity.machine;

import com.hbm.tileentity.machine.TileEntityMachineAssemblyFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.WorldlyContainer;

/**
 * Carries the old proxy-relative inventory access rule into the vanilla
 * {@link WorldlyContainer} API, which only supplies a side and not the accessor block position.
 */
public final class AssemblyFactorySidedContainer implements WorldlyContainer {

    private final TileEntityMachineAssemblyFactory machine;
    private final int[] slots;

    public AssemblyFactorySidedContainer(TileEntityMachineAssemblyFactory machine, BlockPos accessor) {
        this.machine = machine;
        this.slots = machine.getAccessibleSlotsFor(accessor);
    }

    @Override public int[] getSlotsForFace(Direction side) { return slots; }
    @Override public int getContainerSize() { return machine.getContainerSize(); }
    @Override public boolean isEmpty() { return machine.isEmpty(); }
    @Override public ItemStack getItem(int slot) { return machine.getItem(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { return machine.removeItem(slot, amount); }
    @Override public ItemStack removeItemNoUpdate(int slot) { return machine.removeItemNoUpdate(slot); }
    @Override public void setItem(int slot, ItemStack stack) { machine.setItem(slot, stack); }
    @Override public void setChanged() { machine.setChanged(); }
    @Override public boolean stillValid(Player player) { return machine.stillValid(player); }
    @Override public void clearContent() { machine.clearContent(); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return machine.canPlaceItem(slot, stack); }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return contains(slot) && machine.isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return contains(slot) && machine.canExtractItem(slot, stack, 1);
    }

    private boolean contains(int slot) {
        for (int allowed : slots) if (allowed == slot) return true;
        return false;
    }
}
