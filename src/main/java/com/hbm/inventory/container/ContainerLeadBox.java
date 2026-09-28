package com.hbm.inventory.container;

import com.hbm.items.tool.ItemLeadBox.InventoryLeadBox;
import dev.golem.core.gui.CompatContainerMenu;
import dev.golem.core.world.Compat;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code ContainerLeadBox}: the box's four rows of five at (43, 18), the player inventory at (8, 104) and
 * the hotbar at y 162. Box slots defer to the box ({@code SlotNonRetarded}); the open box itself can be
 * neither picked up (1.7 index {@code currentItem + 47}) nor number-key swapped.
 */
public class ContainerLeadBox extends CompatContainerMenu {

    private final InventoryLeadBox box;

    public ContainerLeadBox(MenuType<?> type, int containerId, Inventory invPlayer, InventoryLeadBox box) {
        super(type, containerId);
        this.box = box;

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 5; j++) {
                addSlot(new Slot(box, j + i * 5, 43 + j * 18, 18 + i * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return container.canPlaceItem(getContainerSlot(), stack);
                    }
                });
            }
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 104 + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(invPlayer, i, 8 + i * 18, 162));
        }
    }

    /** Client side: Core's extended menu payload names the hand holding the box. */
    public ContainerLeadBox(MenuType<?> type, int containerId, Inventory invPlayer, InteractionHand hand) {
        this(type, containerId, invPlayer, new InventoryLeadBox(invPlayer.player, invPlayer.player.getItemInHand(hand)));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack var3 = ItemStack.EMPTY;
        Slot var4 = slots.get(index);

        if (var4.hasItem()) {
            ItemStack var5 = var4.getItem();
            var3 = var5.copy();

            if (index <= box.getContainerSize() - 1) {
                if (!moveItemStackTo(var5, box.getContainerSize(), slots.size(), true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(var5, 0, box.getContainerSize(), false)) {
                return ItemStack.EMPTY;
            }

            if (var5.isEmpty()) var4.setByPlayer(ItemStack.EMPTY);
            else var4.setChanged();

            var4.onTake(player, var5);
        }
        return var3;
    }

    @Override
    protected boolean handleClick(int slotId, int button, ClickAction action, Player player) {
        int current = Compat.selectedSlot(player);
        // prevents the player from moving around the currently open box
        if (action == ClickAction.SWAP && button == current) return true;
        return slotId == current + 47;
    }

    @Override
    public boolean stillValid(Player player) {
        return box.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        player.inventoryMenu.broadcastChanges();
    }
}
