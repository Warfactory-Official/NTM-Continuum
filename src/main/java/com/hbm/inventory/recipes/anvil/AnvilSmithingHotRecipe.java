package com.hbm.inventory.recipes.anvil;

import com.hbm.inventory.RecipesCommon;
import com.hbm.items.special.ItemHot;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

/** Tiered anvil smithing that accepts only sufficiently hot ItemHot inputs and averages their heat. */
public final class AnvilSmithingHotRecipe extends AnvilSmithingRecipe {

    public AnvilSmithingHotRecipe(int tier, ItemStack output, RecipesCommon.AStack left,
                                  RecipesCommon.AStack right) {
        super(tier, output, left, right);
    }

    @Override
    protected boolean doesStackMatch(ItemStack input, RecipesCommon.AStack recipe) {
        if (input != null && input.getItem() instanceof ItemHot && ItemHot.getHeat(input) < 0.5D) return false;
        if (input != null && input.getItem() instanceof ItemHot) {
            // Heat is the transient state this recipe transforms; stage/components other than heat
            // still participate in the exact ComparableStack match.
            ItemStack withoutHeat = input.copy();
            ItemHot.setHeat(withoutHeat, 0);
            return recipe.matchesRecipe(withoutHeat, false);
        }
        return recipe.matchesRecipe(input, false);
    }

    @Override
    public ItemStack getOutput(ItemStack left, ItemStack right) {
        if (left.getItem() instanceof ItemHot && right.getItem() instanceof ItemHot
                && output.getItem() instanceof ItemHot) {
            ItemStack result = output.copy();
            ItemHot.heatUp(result, (ItemHot.getHeat(left) + ItemHot.getHeat(right)) / 2D);
            return result;
        }
        return output.copy();
    }

    @Override
    public List<ItemStack> getLeft() {
        ItemStack first = left.getDisplayStack();
        if (first.getItem() instanceof ItemHot) ItemHot.heatUp(first);
        return Collections.singletonList(first);
    }

    @Override
    public List<ItemStack> getRight() {
        ItemStack first = right.getDisplayStack();
        if (first.getItem() instanceof ItemHot) ItemHot.heatUp(first);
        return Collections.singletonList(first);
    }
}
