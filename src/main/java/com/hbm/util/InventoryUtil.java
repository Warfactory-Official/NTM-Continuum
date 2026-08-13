package com.hbm.util;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilOutput;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Player-inventory ingredient helpers, ported from the 1.12.2 {@code InventoryUtil} (only the members the
 * anvil needs so far — grow as content is ported). Works on the player's 36 main slots
 * ({@link net.minecraft.world.entity.player.Inventory#getNonEquipmentItems()}).
 */
public final class InventoryUtil {

    private InventoryUtil() {}

    /**
     * Checks whether the player's main inventory can satisfy every {@link AStack} ingredient (counts drawn
     * across any number of slots), and if {@code shouldRemove} also consumes them. All-or-nothing: nothing
     * is consumed unless every ingredient is satisfiable.
     */
    /**
     * Inserts {@code stack} into machine-inventory slots {@code start..end} (inclusive), merging into
     * existing stacks first, then empty slots. Returns the remainder ({@link ItemStack#EMPTY} when fully
     * absorbed) — the 1.12.2 {@code tryAddItemToInventory} (which returned null for "absorbed").
     */
    public static ItemStack tryAddItemToInventory(com.hbm.inventory.NTMItemHandler inventory, int start, int end, ItemStack stack) {
        ItemStack rest = stack;
        for (int i = start; i <= end && !rest.isEmpty(); i++) {
            rest = inventory.insertItem(i, rest, false);
        }
        return rest;
    }

    public static boolean doesPlayerHaveAStacks(Player player, List<AStack> stacks, boolean shouldRemove) {

        //? if >=26.1 {
        NonNullList<ItemStack> original = player.getInventory().getNonEquipmentItems();
        //?} else {
        /*NonNullList<ItemStack> original = player.getInventory().items;
        *///?}

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
                original.set(i, inventory[i].isEmpty() ? ItemStack.EMPTY : inventory[i]);
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
