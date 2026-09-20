package com.hbm.inventory.container;

import com.hbm.items.machine.IItemFluidId;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import com.hbm.inventory.ModDroneMenus;
import com.hbm.tileentity.network.TileEntityDroneCrate;

/** 1.7 {@code ContainerDroneCrate}: 18 cargo slots, the fluid identifier slot and the player inventory. */
public class ContainerDroneCrate extends DroneMenuBase<TileEntityDroneCrate> {

    public ContainerDroneCrate(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos, TileEntityDroneCrate.class));
    }

    public ContainerDroneCrate(int containerId, Inventory playerInventory, @Nullable TileEntityDroneCrate crate) {
        super(ModDroneMenus.DRONE_CRATE.get(), containerId, crate, TileEntityDroneCrate.SLOT_COUNT);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 6; j++) {
                addSlot(new Slot(machine, j + i * 6, 8 + j * 18, 17 + i * 18));
            }
        }

        addSlot(new Slot(machine, TileEntityDroneCrate.SLOT_FLUID_ID, 125, 53));

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 103);
    }

    /** 1.7: an identifier shift-clicked in goes to its own slot, anything else to the cargo. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index < machineSlots) {
                if (!this.moveItemStackTo(stack, machineSlots, this.slots.size(), true)) return ItemStack.EMPTY;
            } else {
                if (result.getItem() instanceof IItemFluidId) {
                    if (!this.moveItemStackTo(stack, TileEntityDroneCrate.SLOT_FLUID_ID,
                            TileEntityDroneCrate.SLOT_COUNT, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(stack, 0, TileEntityDroneCrate.CARGO_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
