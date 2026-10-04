package com.hbm.inventory.recipes;

import dev.golem.core.world.ComponentIngredient;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * Shapeless matching for the Java-decided crafting recipes: every occupied slot is used by exactly one ingredient and
 * every ingredient by exactly one slot (backtracking, so overlapping ingredients cannot steal each other's slot).
 */
final class ShapelessMatch {

    private ShapelessMatch() {}

    static ComponentIngredient of(String itemOrTag) {
        return ComponentIngredient.of(itemOrTag);
    }

    static List<ComponentIngredient> of(String... itemsOrTags) {
        List<ComponentIngredient> list = new ArrayList<>(itemsOrTags.length);
        for (String entry : itemsOrTags) list.add(ComponentIngredient.of(entry));
        return list;
    }

    /** Whether the occupied slots of {@code input} are exactly {@code ingredients}, one each. */
    static boolean matches(List<ItemStack> input, List<ComponentIngredient> ingredients) {
        return assign(input, ingredients) != null;
    }

    /** The ingredient used for each grid slot (null for an empty slot), or null when the grid does not match. */
    static ComponentIngredient[] assign(List<ItemStack> input, List<ComponentIngredient> ingredients) {
        List<Integer> slots = new ArrayList<>();
        for (int slot = 0; slot < input.size(); slot++) if (!input.get(slot).isEmpty()) slots.add(slot);
        if (slots.size() != ingredients.size()) return null;
        ComponentIngredient[] used = new ComponentIngredient[input.size()];
        boolean[] taken = new boolean[ingredients.size()];
        return place(input, ingredients, slots, 0, taken, used) ? used : null;
    }

    private static boolean place(List<ItemStack> input, List<ComponentIngredient> ingredients, List<Integer> slots,
                                 int index, boolean[] taken, ComponentIngredient[] used) {
        if (index == slots.size()) return true;
        int slot = slots.get(index);
        for (int i = 0; i < ingredients.size(); i++) {
            if (taken[i] || !ingredients.get(i).test(input.get(slot))) continue;
            taken[i] = true;
            used[slot] = ingredients.get(i);
            if (place(input, ingredients, slots, index + 1, taken, used)) return true;
            taken[i] = false;
            used[slot] = null;
        }
        return false;
    }
}
