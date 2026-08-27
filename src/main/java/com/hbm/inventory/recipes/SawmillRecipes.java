package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.items.ModItems;
import dev.golem.core.Ids;
import dev.golem.core.world.Recipes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.List;

/** The four input classes and outputs of the 1.12.2 sawmill. */
public final class SawmillRecipes {

    private SawmillRecipes() {}

    /** The modern equivalent of the old {@code stickWood} ore-dictionary entry. */
    public static final TagKey<net.minecraft.world.item.Item> WOODEN_RODS = TagKey.create(
            Registries.ITEM, Ids.of("c", "rods/wooden"));

    public record SawmillRecipe(AStack input, ItemStack output, ItemStack secondary, int secondaryChance) {}

    private static final List<SawmillRecipe> RECIPES = List.of(
            new SawmillRecipe(new TagStack(ItemTags.LOGS), new ItemStack(Items.OAK_PLANKS, 6),
                    new ItemStack(ModItems.powder_sawdust.get()), 50),
            new SawmillRecipe(new TagStack(ItemTags.PLANKS), new ItemStack(Items.STICK, 6),
                    new ItemStack(ModItems.powder_sawdust.get()), 10),
            new SawmillRecipe(new TagStack(WOODEN_RODS), new ItemStack(ModItems.powder_sawdust.get()),
                    ItemStack.EMPTY, 0),
            new SawmillRecipe(new TagStack(ItemTags.SAPLINGS), new ItemStack(Items.STICK),
                    new ItemStack(ModItems.powder_sawdust.get()), 10));

    public static List<SawmillRecipe> getRecipes() {
        return RECIPES;
    }

    /**
     * Resolves a sawmill input on the server. Log output deliberately comes from the active vanilla
     * crafting table, preserving modded-log plank conversions instead of hardcoding wood species.
     */
    public static ItemStack getOutput(Level level, ItemStack input) {
        if (input.isEmpty()) return ItemStack.EMPTY;
        if (input.is(WOODEN_RODS) || input.is(Items.STICK)) {
            return new ItemStack(ModItems.powder_sawdust.get());
        }
        if (input.is(ItemTags.LOGS)) {
            return Recipes.craftingResult(level, input)
                    .filter(result -> result.is(ItemTags.PLANKS))
                    .map(result -> {
                        result.setCount(result.getCount() * 6 / 4);
                        return result;
                    })
                    .orElse(ItemStack.EMPTY);
        }
        if (input.is(ItemTags.PLANKS)) return new ItemStack(Items.STICK, 6);
        if (input.is(ItemTags.SAPLINGS)) return new ItemStack(Items.STICK);
        return ItemStack.EMPTY;
    }

    public static boolean isValidInput(ItemStack input) {
        return !input.isEmpty() && (input.is(ItemTags.LOGS) || input.is(ItemTags.PLANKS)
                || input.is(WOODEN_RODS) || input.is(Items.STICK) || input.is(ItemTags.SAPLINGS));
    }
}
