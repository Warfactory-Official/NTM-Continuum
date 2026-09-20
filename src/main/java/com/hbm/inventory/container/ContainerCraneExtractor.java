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
import com.hbm.tileentity.network.TileEntityCraneExtractor;

/** 1.7 {@code ContainerCraneExtractor}: nine ghost filters, a 3x3 buffer, two upgrades, the player inventory. */
public class ContainerCraneExtractor extends ContainerCraneBase<TileEntityCraneExtractor> {

    public ContainerCraneExtractor(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos, TileEntityCraneExtractor.class));
    }

    public ContainerCraneExtractor(int containerId, Inventory playerInventory, @Nullable TileEntityCraneExtractor tile) {
        super(ModConveyorMenus.CRANE_EXTRACTOR.get(), containerId, tile, 20);

        // filter
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                addSlot(pattern(j + i * 3, 71 + j * 18, 17 + i * 18));
            }
        }

        // buffer
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                addSlot(plain(9 + j + i * 3, 8 + j * 18, 17 + i * 18));
            }
        }

        // upgrades
        addSlot(upgrade(18, 152, 23));
        addSlot(upgrade(19, 152, 47));

        Compat.addPlayerInventory(this::addSlot, playerInventory, 26, 103);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index < 9) return ItemStack.EMPTY; // filters

            if (index < 20) {
                if (!this.moveItemStackTo(stack, 20, this.slots.size(), true)) return ItemStack.EMPTY;
            } else {
                if (ModConveyorItems.stackTier(result) > 0) {
                    if (!this.moveItemStackTo(stack, 18, 19, false)) return ItemStack.EMPTY;
                } else if (ModConveyorItems.ejectorTier(result) > 0) {
                    if (!this.moveItemStackTo(stack, 19, 20, false)) return ItemStack.EMPTY;
                } else if (!this.moveItemStackTo(stack, 9, 18, false)) {
                    return ItemStack.EMPTY;
                }
                // 1.7 returned null after a successful player-to-machine move
                if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }

        return result;
    }

    /** 1.7 {@code slotClick} on a filter: right click cycles the mode, anything else sets the ghost to the carried item. */
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
