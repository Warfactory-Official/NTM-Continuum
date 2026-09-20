package com.hbm.tileentity.network.pneumatic;

import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.blocks.network.PneumoStorageClutter;
import com.hbm.inventory.container.ContainerPneumoStorageClutter;
import com.hbm.tileentity.ModPneumaticBlockEntities;

/**
 * 1.7 {@code TileEntityPneumoStorageClutter}: a 54-slot crate whose every slot is a network monitor. Like the
 * storage crates, its contents travel with the dropped item (1.7's {@code CRATE_KEEP_CONTENTS} default) through
 * the container component and the block's loot table.
 */
public class TileEntityPneumoStorageClutter extends TileEntityPneumaticStorageBase {

    public static final int SLOT_COUNT = 6 * 9;

    public TileEntityPneumoStorageClutter(BlockPos pos, BlockState state) {
        super(ModPneumaticBlockEntities.PNEUMATIC_STORAGE_CLUTTER.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public String getDefaultName() {
        return "container.pneumoStorageClutter";
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerPneumoStorageClutter(containerId, playerInventory, this);
    }

    @Override
    public long getAmountAt(int index) {
        return getSlotAt(index).getCount();
    }

    @Override
    public boolean allowTypeSetting() {
        return true;
    }

    @Override
    public long useUpItem(int index, long amount) {
        ItemStack stack = inventory.getStackInSlot(index);
        if (!stack.isEmpty()) {
            int toRemove = (int) Math.min(stack.getCount(), amount);
            this.removeItem(index, toRemove);
            return amount - toRemove;
        }
        return amount;
    }

    @Override
    public long addItem(int index, long amount) {
        ItemStack stack = inventory.getStackInSlot(index);
        if (!stack.isEmpty()) {
            int capacity = Math.min(stack.getMaxStackSize(), this.getMaxStackSize());
            int toAdd = (int) Math.min(amount, capacity - stack.getCount());
            stack.grow(toAdd);
            setChanged();
            return amount - toAdd;
        }
        return amount;
    }

    @Override
    public long setupType(int index, ItemStack zeroStack, long amount) {
        int capacity = Math.min(zeroStack.getMaxStackSize(), this.getMaxStackSize());
        int finalSize = (int) Math.min(amount, capacity);
        inventory.setStackInSlot(index, zeroStack.copyWithCount(finalSize));
        return amount - finalSize;
    }

    // --- 1.7: crate contents travel with it (the PneumoStorageClutter.removedByPlayer NBT drop) ---

    @Override
    protected NonNullList<ItemStack> carriedItems() {
        return inventory.getStacks();
    }

    /** Don't spill the inventory on removal: the contents ride the dropped item through the loot table. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    }

    @Override
    protected void removeSavedComponents(DataOut output) {
        super.removeSavedComponents(output);
        output.discard("inventory");
        output.discard("customName");
    }
}
