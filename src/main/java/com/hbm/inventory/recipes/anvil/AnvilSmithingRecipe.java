package com.hbm.inventory.recipes.anvil;

import com.hbm.inventory.RecipesCommon.AStack;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A two-input anvil smithing recipe (left + right slot → output), ported from 1.12.2. Matching is by
 * {@link AStack} (exact item or tag); {@link #makeShapeless()} also accepts the mirrored arrangement.
 * In LBSM mode with {@code LBSM_recipeUnlockAnvil} every recipe is tier 1, as in 1.7. Subclasses: Mold, Hot, Rename;
 * 1.7's Cyanide special is not ported (it needs a food-eaten hook the port does not have, History §19ej-3).
 */
public class AnvilSmithingRecipe {

    public final int tier;
    protected final ItemStack output;
    protected final AStack left;
    protected final AStack right;
    protected boolean shapeless = false;

    public AnvilSmithingRecipe(int tier, ItemStack output, AStack left, AStack right) {
        // 1.7 LBSM_recipeUnlockAnvil: every smithing recipe at tier 1 in LBSM mode (History §19ej-8)
        this.tier = com.hbm.config.GeneralConfig.enableLBSM && com.hbm.config.GeneralConfig.enableLBSMUnlockAnvil ? 1 : tier;
        this.output = output;
        this.left = left;
        this.right = right;
    }

    public AnvilSmithingRecipe makeShapeless() {
        this.shapeless = true;
        return this;
    }

    public boolean matches(ItemStack left, ItemStack right) {
        return matchesInt(left, right) != -1;
    }

    /** 0 = matches as-is, 1 = matches mirrored (shapeless only), -1 = no match. */
    public int matchesInt(ItemStack left, ItemStack right) {

        if (doesStackMatch(left, this.left) && doesStackMatch(right, this.right)) return 0;

        if (shapeless) {
            return doesStackMatch(right, this.left) && doesStackMatch(left, this.right) ? 1 : -1;
        }

        return -1;
    }

    protected boolean doesStackMatch(ItemStack input, AStack recipe) {
        return recipe.matchesRecipe(input, false);
    }

    public List<ItemStack> getLeft() {
        return left.getDisplayStacks();
    }

    public List<ItemStack> getRight() {
        return right.getDisplayStacks();
    }

    public ItemStack getSimpleOutput() {
        return output.copy();
    }

    public ItemStack getOutput(ItemStack left, ItemStack right) {
        return getSimpleOutput();
    }

    /** How many items the given input slot (0 = left, 1 = right) consumes on take. */
    public int amountConsumed(int index, boolean mirrored) {

        if (index == 0) return mirrored ? right.stacksize : left.stacksize;
        if (index == 1) return mirrored ? left.stacksize : right.stacksize;

        return 0;
    }
}
