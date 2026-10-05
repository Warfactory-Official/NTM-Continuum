package com.hbm.inventory.recipes.anvil;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModMedicineItems;
import dev.golem.core.world.ItemStacks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 1.7 {@code AnvilSmithingCyanideRecipe}: any food on the left and a Plan C (or a red pill) on the right give one of
 * that food laced: {@code ntmCyanide} (Plan C) or {@code ntmRedPill} (red pill). Eating it runs
 * {@link com.hbm.main.ModEventHandlerHooks#onFoodEaten}: ten 1000-point euthanasia hits for cyanide, ten hour-long
 * death effects for the red pill. Tier 1; the bread is only the recipe browser's example. The flags are the
 * {@code hbm:ntm_cyanide} / {@code hbm:ntm_red_pill} components (1.7 stored them as root NBT booleans).
 */
public class AnvilSmithingCyanideRecipe extends AnvilSmithingRecipe {

    public AnvilSmithingCyanideRecipe() {
        super(1, laced(new ItemStack(Items.BREAD), false), new ComparableStack(Items.BREAD),
                new ComparableStack(ModMedicineItems.plan_c.get()));
    }

    private static ItemStack laced(ItemStack stack, boolean redPill) {
        if (redPill) ModDataComponents.NTM_RED_PILL.set(stack, true);
        else ModDataComponents.NTM_CYANIDE.set(stack, true);
        return stack;
    }

    @Override
    public boolean matches(ItemStack left, ItemStack right) {
        // 1.7: left.getItem() instanceof ItemFood - every item with food properties
        return (doesStackMatch(right, this.right) || right.is(ModMedicineItems.pill_red.get()))
                && !left.isEmpty() && ItemStacks.foodProperties(left) != null;
    }

    @Override
    public int matchesInt(ItemStack left, ItemStack right) {
        return matches(left, right) ? 0 : -1;
    }

    @Override
    public ItemStack getOutput(ItemStack left, ItemStack right) {
        return laced(ItemStacks.copyWithCount(left, 1), right.is(ModMedicineItems.pill_red.get()));
    }
}
