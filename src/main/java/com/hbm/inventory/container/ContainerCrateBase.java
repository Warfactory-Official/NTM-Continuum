package com.hbm.inventory.container;

import com.hbm.items.block.CrateHeldOpening;
import com.hbm.tileentity.machine.storage.TileEntityCrateBase;
import dev.golem.core.gui.CompatContainerMenu;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code ContainerCrateBase} for the lockable crates (tungsten crate, safe, filing cabinet): opening plays
 * the crate's {@code openInventory}, closing its {@code closeInventory}, and shift-clicking moves between the
 * crate slots and the player inventory (1.7 {@code ContainerBase.transferStackInSlot}).
 *
 * <p>The client builds the same slots against an empty stand-in when the position does not resolve, as the
 * other position-bound menus do; the server copy is always bound to the live block entity.
 */
public abstract class ContainerCrateBase extends CompatContainerMenu {

    protected final Container crate;
    protected final @Nullable TileEntityCrateBase tile;
    protected final int crateSlots;

    protected ContainerCrateBase(MenuType<?> type, int containerId, @Nullable TileEntityCrateBase tile, int slots) {
        super(type, containerId);
        this.tile = tile;
        this.crate = tile != null ? tile : new SimpleContainer(slots);
        this.crateSlots = slots;
        this.heldSlot = -1;
        if (tile != null) tile.openInventory();
    }

    /** The hotbar slot holding a crate opened from the hand, which may not be touched; -1 for a placed crate. */
    protected final int heldSlot;

    /** A crate opened from the hand (1.7 {@code InventoryCrate}): {@code held} is the stack-backed inventory. */
    protected ContainerCrateBase(MenuType<?> type, int containerId, Container held, int slots, int heldSlot) {
        super(type, containerId);
        this.tile = null;
        this.crate = held;
        this.crateSlots = slots;
        this.heldSlot = heldSlot;
    }

    @Override
    protected boolean handleClick(int slotId, int button, ClickAction action, Player player) {
        return CrateHeldOpening.blocksClick(this, heldSlot, slotId, button, action);
    }

    protected static @Nullable TileEntityCrateBase resolve(Inventory inventory, BlockPos pos) {
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityCrateBase crate ? crate : null;
    }

    /** 1.7 {@code playerInv(inv, x, y, hotbarY)}; every lockable crate keeps the hotbar 58 below the grid. */
    protected void playerInv(Inventory inventory, int x, int y) {
        Compat.addPlayerInventory(this::addSlot, inventory, x, y);
    }

    public @Nullable TileEntityCrateBase tile() {
        return tile;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (tile != null) tile.closeInventory();
        else if (crate instanceof CrateHeldOpening.HeldCrate held) held.closed();
    }

    @Override
    public boolean stillValid(Player player) {
        if (tile == null) return crate.stillValid(player);
        return tile.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < crateSlots) {
                if (!this.moveItemStackTo(stack, crateSlots, this.slots.size(), true)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stack, 0, crateSlots, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
