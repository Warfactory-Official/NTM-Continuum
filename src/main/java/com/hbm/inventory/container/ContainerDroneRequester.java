package com.hbm.inventory.container;

import com.hbm.inventory.container.PneumaticMenuBase.SlotPattern;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import com.hbm.inventory.ModDroneMenus;
import com.hbm.tileentity.network.TileEntityDroneRequester;

/** 1.7 {@code ContainerDroneRequester}: nine ghost filters on the right, the nine-slot delivery bank on the left. */
public class ContainerDroneRequester extends DroneMenuBase<TileEntityDroneRequester> {

    public ContainerDroneRequester(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos, TileEntityDroneRequester.class));
    }

    public ContainerDroneRequester(int containerId, Inventory playerInventory,
                                   @Nullable TileEntityDroneRequester requester) {
        super(ModDroneMenus.DRONE_REQUESTER.get(), containerId, requester, TileEntityDroneRequester.SLOT_COUNT);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                addSlot(new SlotPattern(machine, j + i * 3, 98 + j * 18, 17 + i * 18));
            }
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                addSlot(new Slot(machine, j + i * 3 + 9, 26 + j * 18, 17 + i * 18));
            }
        }

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 103);
    }

    @Override
    protected boolean handleClick(int slotId, int button, ClickAction action, Player player) {
        if (slotId < 0 || slotId > 8) return false;

        Slot slot = getSlot(slotId);

        if (button == 1 && action == ClickAction.PICKUP && slot.hasItem()) {
            if (tile != null) tile.nextMode(slotId);
            return true;
        }

        ItemStack carried = getCarried();
        machine.setItem(slotId, carried.isEmpty() ? ItemStack.EMPTY : carried.copy());
        slot.setChanged();
        if (tile != null && tile.getLevel() != null) {
            tile.matcher.initPatternStandard(tile.getLevel(), machine.getItem(slotId), slotId);
            tile.setChanged();
        }
        return true;
    }

    /** 1.7: the filters are ignored; the delivery bank moves out, the inventory moves into the bank. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < TileEntityDroneRequester.FILTER_COUNT) return ItemStack.EMPTY;

        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < machineSlots) {
                if (!this.moveItemStackTo(stack, machineSlots, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stack, TileEntityDroneRequester.FILTER_COUNT, machineSlots, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
