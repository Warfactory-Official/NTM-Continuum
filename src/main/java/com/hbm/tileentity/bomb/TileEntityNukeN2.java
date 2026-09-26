package com.hbm.tileentity.bomb;

import dev.golem.core.machine.ItemHandler;
import com.hbm.inventory.container.ContainerNukeN2;
import com.hbm.items.ModBombItems;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** N²'s twelve independent, one-charge component slots. */
public class TileEntityNukeN2 extends TileEntityNukeBomb {

    public TileEntityNukeN2(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NUKE_N2.get(), pos, state, 12);
    }

    @Override
    protected ItemHandler createInventory(int slotCount) {
        return new ItemHandler(slotCount, 1) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return TileEntityNukeN2.this.isItemValidForSlot(slot, stack);
            }
        };
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot >= 0 && slot < 12 && stack.getItem() == ModBombItems.n2_charge.get();
    }

    /** Counts occupied charge slots, not stack counts, matching the old handler's one-per-slot limit. */
    public int countCharges() {
        int charges = 0;
        for (int i = 0; i < 12; i++) {
            if (inventory.getStackInSlot(i).getItem() == ModBombItems.n2_charge.get()) charges++;
        }
        return charges;
    }

    @Override
    public boolean isReady() {
        return countCharges() > 0;
    }

    @Override
    public String getDefaultName() {
        return "container.nukeN2";
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerNukeN2(containerId, inventory, this);
    }
}
