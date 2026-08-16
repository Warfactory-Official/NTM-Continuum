package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import com.hbm.blocks.bomb.ModBombBlocks;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.bomb.TileEntityNukeMan;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public class ContainerNukeMan extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = 6;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityNukeMan nuke;

    /** Client-side constructor — receives the {@link BlockPos} sent by the server. */
    public ContainerNukeMan(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    /** Server-side constructor — bound to the live BlockEntity. */
    public ContainerNukeMan(int containerId, Inventory playerInventory, TileEntityNukeMan nuke) {
        this(containerId, playerInventory, nuke,
                ContainerLevelAccess.create(nuke.getLevel(), nuke.getBlockPos()));
    }

    private ContainerNukeMan(int containerId, Inventory playerInventory, @Nullable TileEntityNukeMan nuke, ContainerLevelAccess access) {
        super(ModMenus.NUKE_MAN.get(), containerId);
        this.nuke = nuke;
        this.machine = nuke != null ? nuke : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        // Original layout: igniter (centre-left), four lenses (corners), core (centre-right). Each slot
        // is validated (only its part type may be placed), so shift-clicking routes parts to the right
        // slot and the bomb actually reaches the armed/ready state — the original used SlotItemHandler
        // with isItemValid; plain Slots have no such check (mayPlace defaults to true), which scattered
        // shift-clicked parts into the wrong slots so isReady() never became true.
        this.addSlot(new ValidatedSlot(this.machine, 0, 26, 35));
        this.addSlot(new ValidatedSlot(this.machine, 1, 8, 17));
        this.addSlot(new ValidatedSlot(this.machine, 2, 44, 17));
        this.addSlot(new ValidatedSlot(this.machine, 3, 8, 53));
        this.addSlot(new ValidatedSlot(this.machine, 4, 44, 53));
        this.addSlot(new ValidatedSlot(this.machine, 5, 98, 35));

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 84);
    }

    private static @Nullable TileEntityNukeMan resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityNukeMan nuke ? nuke : null;
    }

    // --- arming state, read by the screen straight off the (client-synced) BlockEntity ---

    public boolean exp1() { return nuke != null && nuke.exp1(); }
    public boolean exp2() { return nuke != null && nuke.exp2(); }
    public boolean exp3() { return nuke != null && nuke.exp3(); }
    public boolean exp4() { return nuke != null && nuke.exp4(); }
    public boolean isReady() { return nuke != null && nuke.isReady(); }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBombBlocks.nuke_man.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int invStart = MACHINE_SLOTS;
            int invEnd = MACHINE_SLOTS + 36;
            if (index < invStart) {
                if (!this.moveItemStackTo(stack, invStart, invEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Player inventory -> bomb component slots (respects per-slot validity).
                if (!this.moveItemStackTo(stack, 0, invStart, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    /** A slot that only accepts the part its index is meant to hold (via {@link Container#canPlaceItem}). */
    private static final class ValidatedSlot extends Slot {
        ValidatedSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.container.canPlaceItem(this.getContainerSlot(), stack);
        }
    }
}
