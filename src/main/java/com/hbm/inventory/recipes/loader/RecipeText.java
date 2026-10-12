package com.hbm.inventory.recipes.loader;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import dev.golem.core.world.ItemStacks;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * Helpers shared by the item-processing recipe tables (arc furnace, centrifuge, crystallizer, cyclotron, electrolyser,
 * rotary furnace, SILEX, shredder): the display order of rows and a one-line text form of ingredients and stacks, used
 * by the GameTests that prove a converted table equal to the Java table it replaced.
 */
public final class RecipeText {

    private RecipeText() {}

    /**
     * {@code rows} sorted by their {@code order} field, ties in recipe-id order (the order {@link DataRecipeTable#rows}
     * hands them over). A table whose lookup is "first match wins" (1.7 iterated its list) keeps 1.7's order this way;
     * HBM's rows number 0.., a datapack row without {@code order} comes after them.
     */
    public static <R> List<R> ordered(List<R> rows, ToIntFunction<R> order) {
        List<R> sorted = new ArrayList<>(rows);
        sorted.sort(Comparator.comparingInt(order)); // stable
        return sorted;
    }

    /** An ingredient as text: {@code #c:ores/iron x1}, {@code hbm:coke x2}, {@code mat:iron/ingot x1}, or its kind. */
    public static String describe(AStack stack) {
        if (stack == null) return "-";
        String kind;
        if (stack instanceof TagStack tag) kind = "#" + tag.tag.location();
        else if (stack instanceof com.hbm.inventory.RecipesCommon.MaterialStack material) {
            kind = "mat:" + material.material.name + "/" + material.shape;
        } else if (stack instanceof ComparableStack comparable) {
            kind = String.valueOf(BuiltInRegistries.ITEM.getKey(comparable.item));
            List<ItemStack> display = comparable.getDisplayStacks();
            if (!display.isEmpty() && !ItemStacks.sameItemAndData(display.get(0),
                    new ItemStack(comparable.item, display.get(0).getCount()))) kind += "+data";
        } else {
            kind = stack.getClass().getSimpleName() + ":" + describe(stack.getDisplayStack());
        }
        return kind + " x" + stack.stacksize;
    }

    /** A stack as text: {@code hbm:ingot_steel x2}, plus its data when it differs from the item's default. */
    public static String describe(ItemStack stack) {
        if (stack == null) return "null";
        if (stack.isEmpty()) return "empty";
        String text = BuiltInRegistries.ITEM.getKey(stack.getItem()) + " x" + stack.getCount();
        if (!ItemStacks.sameItemAndData(stack, new ItemStack(stack.getItem(), stack.getCount()))) {
            text += " data#" + Integer.toHexString(ItemStacks.hashItemAndData(stack));
        }
        return text;
    }

    public static String describe(ItemStack[] stacks) {
        if (stacks == null) return "null";
        StringBuilder out = new StringBuilder("[");
        for (ItemStack stack : stacks) out.append(describe(stack)).append(',');
        return out.append(']').toString();
    }
}
