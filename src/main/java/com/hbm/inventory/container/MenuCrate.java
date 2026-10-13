package com.hbm.inventory.container;

import com.hbm.inventory.ModMenus;
import com.hbm.items.block.CrateHeldOpening;
import com.hbm.tileentity.machine.storage.CrateType;
import dev.golem.core.gui.CompatContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The crate minecart's menu: ntm-next's {@code MenuCrate} over a live container (the cart) rather than a
 * block entity, laid out exactly like this port's {@link ContainerCrate} for the same {@link CrateType}.
 * Vanilla's container-minecart open path carries no extra data, so the client builds it over a plain
 * container of the type's slot count and the slots sync as usual.
 */
public class MenuCrate extends CompatContainerMenu {

    private final CrateType type;
    private final Container container;

    public MenuCrate(int containerId, Inventory playerInv) {
        this(containerId, playerInv, CrateType.STEEL, new SimpleContainer(CrateType.STEEL.slots));
    }

    public MenuCrate(int containerId, Inventory playerInv, CrateType type, Container container) {
        this(ModMenus.CART_CRATE.get(), containerId, playerInv, type, container, -1);
    }

    /** The hotbar slot of a crate opened from the hand (1.7 {@code InventoryCrate}); -1 for the cart. */
    protected final int heldSlot;

    /**
     * The same layout over another menu type: the held crate ({@code com.hbm.inventory.ModHeldCrateMenus}) whose hotbar
     * slot {@code heldSlot} holds the crate itself and may not be touched (1.7 {@code SlotPlayerCrateLocked}).
     */
    public MenuCrate(MenuType<?> menuType, int containerId, Inventory playerInv,
                     CrateType type, Container container, int heldSlot) {
        super(menuType, containerId);
        this.heldSlot = heldSlot;
        checkContainerSize(container, type.slots);
        this.type = type;
        this.container = container;

        for (int row = 0; row < type.rows; row++) {
            for (int col = 0; col < type.columns; col++) {
                int slot = col + row * type.columns;
                addSlot(new Slot(container, slot, type.crateX + col * 18, type.crateY + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9,
                        type.playerInventoryX + col * 18, type.playerInventoryY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, type.playerInventoryX + col * 18, type.hotbarY));
        }
        container.startOpen(playerInv.player);
    }

    public CrateType getCrateType() {
        return type;
    }

    @Override
    protected boolean handleClick(int slotId, int button, ClickAction action, Player player) {
        return CrateHeldOpening.blocksClick(this, heldSlot, slotId, button, action);
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
        if (container instanceof CrateHeldOpening.HeldCrate held) held.closed();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            original = stack.copy();
            int crateSlots = type.slots;
            int invEnd = slots.size();
            if (index < crateSlots) {
                if (!moveItemStackTo(stack, crateSlots, invEnd, true)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, crateSlots, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return original;
    }
}
