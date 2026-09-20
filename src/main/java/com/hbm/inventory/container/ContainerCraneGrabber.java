package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.inventory.Slot;
import com.hbm.inventory.ModConveyorMenus;
import com.hbm.items.ModConveyorItems;
import com.hbm.tileentity.network.TileEntityCraneGrabber;

/** 1.7 {@code ContainerCraneGrabber}: nine ghost filters, two upgrades, the player inventory. */
public class ContainerCraneGrabber extends ContainerCraneBase<TileEntityCraneGrabber> {

    public ContainerCraneGrabber(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos, TileEntityCraneGrabber.class));
    }

    public ContainerCraneGrabber(int containerId, Inventory playerInventory, @Nullable TileEntityCraneGrabber tile) {
        super(ModConveyorMenus.CRANE_GRABBER.get(), containerId, tile, 11);

        // filter
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                addSlot(pattern(j + i * 3, 40 + j * 18, 17 + i * 18));
            }
        }

        // upgrades
        addSlot(upgrade(9, 121, 23));
        addSlot(upgrade(10, 121, 47));

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 103);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index < 9) return ItemStack.EMPTY; // filters

            if (index < 11) {
                if (!this.moveItemStackTo(stack, 11, this.slots.size(), true)) return ItemStack.EMPTY;
            } else {
                if (ModConveyorItems.stackTier(result) > 0) {
                    if (!this.moveItemStackTo(stack, 9, 10, false)) return ItemStack.EMPTY;
                } else if (ModConveyorItems.ejectorTier(result) > 0) {
                    if (!this.moveItemStackTo(stack, 10, 11, false)) return ItemStack.EMPTY;
                }
                if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }

        return result;
    }

    @Override
    protected boolean handleClick(int slotId, int button, ClickAction action, Player player) {
        if (slotId < 0 || slotId > 8) return false;

        Slot slot = getSlot(slotId);
        if (button == 1 && action == ClickAction.PICKUP && slot.hasItem()) {
            if (tile != null) tile.nextMode(slotId);
            return true;
        }

        ItemStack held = getCarried();
        machine.setItem(slotId, held.isEmpty() ? ItemStack.EMPTY : held.copyWithCount(1));
        slot.setChanged();
        if (tile != null && tile.getLevel() != null) {
            tile.matcher.initPatternStandard(tile.getLevel(), machine.getItem(slotId), slotId);
            tile.setChanged();
        }
        return true;
    }
}
