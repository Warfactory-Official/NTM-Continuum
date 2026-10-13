package com.hbm.inventory.container;

import com.hbm.entity.cart.EntityMinecartDestroyer;
import com.hbm.inventory.ModMenus;
import dev.golem.core.gui.CompatContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code inventory/container/ContainerCartDestroyer}: two 3x3 ghost grids - the first nine match the exact
 * stack, the last nine the bare item. The slots are a filter, never storage.
 */
public class MenuCartDestroyer extends CompatContainerMenu {

    private final Container container;

    public MenuCartDestroyer(int containerId, Inventory playerInv) {
        this(containerId, playerInv, new SimpleContainer(EntityMinecartDestroyer.FILTER_SLOTS));
    }

    public MenuCartDestroyer(int containerId, Inventory playerInv, Container container) {
        super(ModMenus.CART_DESTROYER.get(), containerId);
        checkContainerSize(container, EntityMinecartDestroyer.FILTER_SLOTS);
        this.container = container;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                addSlot(new SlotPattern(container, j + i * 3, 10 + j * 18, 17 + i * 18));
                addSlot(new SlotPattern(container, j + i * 3 + 9, 114 + j * 18, 17 + i * 18));
            }
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInv, i, 8 + i * 18, 142));
        }
    }

    // 1.7 slotClick: a filter slot takes a single-item ghost of the carried stack, the cursor keeps its stack
    @Override
    protected boolean handleClick(int slotId, int button, ClickAction action, Player player) {
        if (slotId >= 0 && slotId < EntityMinecartDestroyer.FILTER_SLOTS) {
            slots.get(slotId).set(getCarried());
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    /** 1.7 SlotPattern: a filter slot that never takes or gives items (ntm-next {@code inventory/slot/SlotPattern}). */
    public static class SlotPattern extends Slot {
        public SlotPattern(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }

    // 1.7 transferStackInSlot: no shift-move at all
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
