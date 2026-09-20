package com.hbm.tileentity.network.pneumatic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.inventory.container.ContainerPneumoStorageImporter;
import com.hbm.tileentity.ModPneumaticBlockEntities;

/** 1.7 {@code TileEntityPneumoStorageImporter}: pushes whatever lands in its nine slots into the network. */
public class TileEntityPneumoStorageImporter extends TileEntityPneumaticMachineBase implements WorldlyContainer {

    public static final int SLOT_COUNT = 9;
    private static final int[] SLOT_ACCESS = {0, 1, 2, 3, 4, 5, 6, 7, 8};

    /** 1.7 declares no NBT half at all, so the per-slot cooldown is transient. */
    public final int[] delay = new int[SLOT_COUNT];

    public TileEntityPneumoStorageImporter(BlockPos pos, BlockState state) {
        super(ModPneumaticBlockEntities.PNEUMATIC_STORAGE_IMPORTER.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public String getDefaultName() {
        return "container.pneumoStorageImporter";
    }

    @Override
    protected void onSlotContentsChanged(int slot, ItemStack stack) {
        super.onSlotContentsChanged(slot, stack);
        // 1.7 setInventorySlotContents: a freshly inserted stack waits a tick before it is pushed
        if (!stack.isEmpty()) this.delay[slot] = Math.max(this.delay[slot], 1);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOT_ACCESS;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return SLOT_ACCESS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public void serverTick() {
        super.serverTick();

        if (this.cache != null && !this.cache.hasExpired) for (int i = 0; i < SLOT_COUNT; i++) {
            if (this.delay[i] > 0) {
                this.delay[i]--;
                continue;
            }
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            int leftover = (int) this.cache.addItemsAndReturnQuantity(stack, stack.getCount());
            if (leftover == stack.getCount()) {
                this.delay[i] = 100;
            } else {
                this.removeItem(i, stack.getCount() - leftover);
            }
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerPneumoStorageImporter(containerId, playerInventory, this);
    }
}
