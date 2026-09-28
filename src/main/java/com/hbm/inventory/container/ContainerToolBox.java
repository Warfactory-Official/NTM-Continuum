package com.hbm.inventory.container;

import com.hbm.items.tool.ItemToolBox.InventoryToolBox;
import dev.golem.core.gui.CompatContainerMenu;
import dev.golem.core.world.Compat;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code ContainerToolBox}: the box's three rows of eight at (17, 49), the player inventory at (8, 129) and the
 * hotbar at y 187. The box slots only refuse another toolbox ({@code SlotNonRetarded} deferring to the inventory).
 * The box being held can be neither picked up (1.7 index {@code currentItem + 51}) nor number-key swapped.
 */
public class ContainerToolBox extends CompatContainerMenu {

    private final InventoryToolBox box;

    public ContainerToolBox(MenuType<?> type, int containerId, Inventory invPlayer, InventoryToolBox box) {
        super(type, containerId);
        this.box = box;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 8; j++) {
                addSlot(new Slot(box, j + i * 8, 17 + j * 18, 49 + i * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return container.canPlaceItem(getContainerSlot(), stack);
                    }
                });
            }
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 129 + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(invPlayer, i, 8 + i * 18, 187));
        }
    }

    /** Client side: Core's extended menu payload names the hand holding the box. */
    public ContainerToolBox(MenuType<?> type, int containerId, Inventory invPlayer, InteractionHand hand) {
        this(type, containerId, invPlayer, new InventoryToolBox(invPlayer.player, invPlayer.player.getItemInHand(hand)));
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
        return slotId == current + 51;
    }

    @Override
    public boolean stillValid(Player player) {
        return box.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        box.close();
        player.inventoryMenu.broadcastChanges();
    }
}
