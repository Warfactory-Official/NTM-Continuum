package com.hbm.inventory;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.Inventories;
import dev.golem.core.data.DataOut;

/**
 * Vanilla-only replacement for Forge's {@code ItemStackHandler} / {@code IItemHandler}, which common
 * cannot use (they live in the loader APIs). Backed by a {@link NonNullList} of {@link ItemStack} and
 * persisted with {@link ContainerHelper} (the {@link ItemStack} codec).
 *
 * <p>The public method surface deliberately mirrors the old {@code ItemStackHandler} so ported machine
 * code needs minimal edits: {@link #getSlots()}, {@link #getStackInSlot(int)},
 * {@link #setStackInSlot(int, ItemStack)}, {@link #insertItem(int, ItemStack, boolean)},
 * {@link #extractItem(int, int, boolean)}, {@link #getSlotLimit(int)}, plus the
 * {@link #onContentsChanged(int)} / {@link #isItemValid(int, ItemStack)} hooks.
 *
 * <p>Save/load is via {@link #save(DataOut)} / {@link #load(DataIn)} (26.1's
 * {@code DataOut}/{@code DataIn}) rather than the old {@code serializeNBT}/{@code deserializeNBT}.
 */
public class NTMItemHandler {

    protected NonNullList<ItemStack> stacks;
    protected final int slotLimit;

    public NTMItemHandler(int size) {
        this(size, 64);
    }

    public NTMItemHandler(int size, int slotLimit) {
        this.stacks = NonNullList.withSize(size, ItemStack.EMPTY);
        this.slotLimit = slotLimit;
    }

    public int getSlots() {
        return stacks.size();
    }

    public ItemStack getStackInSlot(int slot) {
        return stacks.get(slot);
    }

    public void setStackInSlot(int slot, ItemStack stack) {
        stacks.set(slot, stack);
        onContentsChanged(slot);
    }

    /** Per-slot stack cap (clamped against the item's own max stack size by callers). */
    public int getSlotLimit(int slot) {
        return slotLimit;
    }

    protected int getStackLimit(int slot, ItemStack stack) {
        return Math.min(getSlotLimit(slot), stack.getMaxStackSize());
    }

    /** Override to gate which items may live in a slot. */
    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }

    /** Called whenever a slot's contents change. Override to {@code setChanged()} the owning tile. */
    protected void onContentsChanged(int slot) {}

    /**
     * Inserts as much of {@code stack} into {@code slot} as the limits allow; returns the remainder.
     * Mirrors {@code ItemStackHandler.insertItem}.
     */
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (!isItemValid(slot, stack)) return stack;

        ItemStack existing = stacks.get(slot);
        int limit = getStackLimit(slot, stack);

        if (!existing.isEmpty()) {
            if (!dev.golem.core.world.ItemStacks.sameItemAndData(stack, existing)) return stack;
            limit -= existing.getCount();
        }
        if (limit <= 0) return stack;

        int toInsert = Math.min(limit, stack.getCount());
        if (!simulate) {
            if (existing.isEmpty()) {
                stacks.set(slot, dev.golem.core.world.ItemStacks.copyWithCount(stack, toInsert));
            } else {
                existing.grow(toInsert);
            }
            onContentsChanged(slot);
        }

        int remainder = stack.getCount() - toInsert;
        return remainder <= 0 ? ItemStack.EMPTY : dev.golem.core.world.ItemStacks.copyWithCount(stack, remainder);
    }

    /** Extracts up to {@code amount} from {@code slot}. Mirrors {@code ItemStackHandler.extractItem}. */
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;
        ItemStack existing = stacks.get(slot);
        if (existing.isEmpty()) return ItemStack.EMPTY;

        int toExtract = Math.min(amount, existing.getMaxStackSize());
        if (existing.getCount() <= toExtract) {
            if (!simulate) {
                stacks.set(slot, ItemStack.EMPTY);
                onContentsChanged(slot);
            }
            return simulate ? existing.copy() : existing;
        } else {
            if (!simulate) {
                stacks.set(slot, dev.golem.core.world.ItemStacks.copyWithCount(existing, existing.getCount() - toExtract));
                onContentsChanged(slot);
            }
            return dev.golem.core.world.ItemStacks.copyWithCount(existing, toExtract);
        }
    }

    public NonNullList<ItemStack> getStacks() {
        return stacks;
    }

    // --- persistence ---

    /** Writes the inventory under the {@code "Items"} list (the {@link ContainerHelper} format). */
    public void save(DataOut output) {
        Inventories.save(output, stacks, true);
    }

    public void load(DataIn input) {
        Inventories.load(input, stacks);
    }

    // Kept for parity with future code that uses the registry-aware helpers; HolderLookup is unused for
    // the plain ItemStack codec path but several call sites pass it through, so accept it explicitly.
    public void save(DataOut output, HolderLookup.Provider registries) {
        save(output);
    }
}
