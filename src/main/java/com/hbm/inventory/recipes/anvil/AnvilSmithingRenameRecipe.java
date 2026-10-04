package com.hbm.inventory.recipes.anvil;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import dev.golem.core.world.ItemStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 1.7 {@code AnvilSmithingRenameRecipe}: any item on the left and a named name tag on the right give the left item
 * (one of it) renamed to {@code "§r" + name}, where a typed {@code \&} becomes a formatting code. The name tag's
 * recipe count is 0, so it is never consumed. The iron sword is only the recipe browser's example, as in 1.7.
 */
public class AnvilSmithingRenameRecipe extends AnvilSmithingRecipe {

    public AnvilSmithingRenameRecipe() {
        super(1, new ItemStack(Items.IRON_SWORD), new ComparableStack(Items.IRON_SWORD), new ComparableStack(Items.NAME_TAG, 0));
    }

    @Override
    public boolean matches(ItemStack left, ItemStack right) {
        // 1.7 checks only the right slot; an empty left slot cannot produce anything in the port's container
        return !left.isEmpty() && right.is(Items.NAME_TAG) && ItemStacks.customName(right) != null;
    }

    @Override
    public int matchesInt(ItemStack left, ItemStack right) {
        return matches(left, right) ? 0 : -1;
    }

    @Override
    public ItemStack getOutput(ItemStack left, ItemStack right) {
        ItemStack out = ItemStacks.copyWithCount(left, 1);
        Component name = ItemStacks.customName(right);
        if (name != null) {
            ItemStacks.setCustomName(out, Component.literal("§r" + name.getString().replace("\\&", "§")));
        }
        return out;
    }
}
