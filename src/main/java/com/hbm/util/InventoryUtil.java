package com.hbm.util;

import com.hbm.inventory.NTMItemHandler;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilOutput;
import dev.golem.core.data.Inventories;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Player-inventory ingredient helpers, ported from the 1.12.2 {@code InventoryUtil} (only the members the
 * anvil needs so far; add more as content needs it). Works on the player's 36 main slots
 * through {@link dev.golem.core.data.Inventories#main}.
 */
public final class InventoryUtil {

    private InventoryUtil() {}

    /**
     * 1.7 {@code mergeItemStack}: vanilla's shift-click merge, except that a stack also tops up an occupied slot only if
     * the slot would accept it.
     */
    public static boolean mergeItemStack(List<Slot> slots, ItemStack stack, int start, int end, boolean reverse) {
        boolean success = false;
        if (stack.isStackable()) {
            for (int n = 0; n < end - start && !stack.isEmpty(); n++) {
                Slot slot = slots.get(reverse ? end - 1 - n : start + n);
                ItemStack current = slot.getItem();
                if (current.isEmpty() || !ItemStacks.sameItemAndData(stack, current)) continue;
                int max = Math.min(stack.getMaxStackSize(), slot.getMaxStackSize());
                if (!slot.mayPlace(stack.copyWithCount(Math.min(stack.getCount(), max)))) continue;
                int moved = Math.min(stack.getCount(), max - current.getCount());
                if (moved <= 0) continue;
                current.grow(moved);
                stack.shrink(moved);
                slot.setChanged();
                success = true;
            }
        }
        for (int n = 0; n < end - start && !stack.isEmpty(); n++) {
            Slot slot = slots.get(reverse ? end - 1 - n : start + n);
            if (slot.hasItem()) continue;
            int max = Math.min(stack.getMaxStackSize(), slot.getMaxStackSize());
            if (!slot.mayPlace(stack.copyWithCount(Math.min(stack.getCount(), max)))) continue;
            slot.set(stack.split(max));
            success = true;
        }
        return success;
    }

    /**
     * Inserts {@code stack} into machine-inventory slots {@code start..end} (inclusive), merging into
     * existing stacks first, then empty slots. Returns the remainder ({@link ItemStack#EMPTY} when fully
     * absorbed), the 1.12.2 {@code tryAddItemToInventory} (which returned null for "absorbed").
     */
    public static ItemStack tryAddItemToInventory(NTMItemHandler inventory, int start, int end, ItemStack stack) {
        ItemStack rest = stack;
        for (int i = start; i <= end && !rest.isEmpty(); i++) {
            rest = inventory.insertItem(i, rest, false);
        }
        return rest;
    }

    public static boolean doesPlayerHaveAStacks(Player player, List<AStack> stacks, boolean shouldRemove) {

        NonNullList<ItemStack> original = Inventories.main(player.getInventory());

        // Work on copies so the availability check never mutates the real inventory.
        ItemStack[] inventory = new ItemStack[original.size()];
        for (int i = 0; i < original.size(); i++) inventory[i] = original.get(i).copy();

        for (AStack stack : stacks) {
            int remaining = stack.stacksize;
            for (int j = 0; j < inventory.length && remaining > 0; j++) {
                ItemStack inv = inventory[j];
                if (inv.isEmpty()) continue;
                if (stack.matchesRecipe(inv, true)) {
                    int size = Math.min(remaining, inv.getCount());
                    remaining -= size;
                    inv.shrink(size);
                }
            }
            if (remaining > 0) return false;
        }

        if (shouldRemove) {
            for (int i = 0; i < original.size(); i++) {
                var current = original.get(i);
                var remaining = inventory[i].getCount();
                if (current.getCount() == remaining) continue;
                if (remaining == 0) original.set(i, ItemStack.EMPTY);
                else current.setCount(remaining);
            }
        }

        return true;
    }

    /** Gives each output to the player (rolling its chance), dropping what doesn't fit. Server-side only. */
    public static void giveChanceStacksToPlayer(Player player, List<AnvilOutput> stacks) {
        for (AnvilOutput out : stacks) {
            if (out.chance >= 1.0F || player.getRandom().nextFloat() < out.chance) {
                ItemStack copy = out.stack.copy();
                if (!player.getInventory().add(copy)) {
                    player.drop(copy, false);
                }
            }
        }
    }
}
