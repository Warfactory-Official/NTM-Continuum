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
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

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

    private static final List<SawmillRecipe> SCRIPTED = new ArrayList<>();
    private static final List<AStack> REMOVED = new ArrayList<>();
    private static final Set<SawmillRecipe> HIDDEN = new HashSet<>();
    private static boolean initialized;

    private static synchronized void init() {
        if (initialized) return;
        initialized = true;
        com.hbm.scripting.ScriptedRecipes.apply(SawmillRecipes.class);
    }

    public static List<SawmillRecipe> getRecipes() {
        init();
        List<SawmillRecipe> all = new ArrayList<>(SCRIPTED);
        for (SawmillRecipe recipe : RECIPES) if (!HIDDEN.contains(recipe)) all.add(recipe);
        return all;
    }

    public static void addRecipe(SawmillRecipe recipe) {
        init();
        SCRIPTED.add(0, recipe);
    }

    public static void removeInput(AStack input) {
        init();
        SCRIPTED.removeIf(recipe -> overlaps(recipe.input(), input)
                || recipe.input().getDisplayStacks().stream().anyMatch(stack -> input.matchesRecipe(stack, true)));
        REMOVED.add(input);
        for (SawmillRecipe recipe : RECIPES) if (overlaps(recipe.input(), input)) HIDDEN.add(recipe);
    }

    public static synchronized void reset() {
        SCRIPTED.clear();
        REMOVED.clear();
        HIDDEN.clear();
        initialized = false;
    }

    public static void removeIf(Predicate<SawmillRecipe> filter) {
        init();
        SCRIPTED.removeIf(filter);
        for (SawmillRecipe recipe : RECIPES) if (filter.test(recipe)) HIDDEN.add(recipe);
    }

    private static boolean overlaps(AStack recipe, AStack removed) {
        if (recipe instanceof TagStack a && removed instanceof TagStack b) return a.tag.equals(b.tag);
        return false;
    }

    private static @Nullable SawmillRecipe scripted(ItemStack input) {
        for (SawmillRecipe recipe : SCRIPTED) if (recipe.input().matchesRecipe(input, true)) return recipe;
        return null;
    }

    private static int builtinRow(ItemStack input) {
        int row = -1;
        if (input.is(WOODEN_RODS) || input.is(Items.STICK)) row = 2;
        else if (input.is(ItemTags.LOGS)) row = 0;
        else if (input.is(ItemTags.PLANKS)) row = 1;
        else if (input.is(ItemTags.SAPLINGS)) row = 3;
        if (row < 0 || HIDDEN.contains(RECIPES.get(row))) return -1;
        for (AStack removed : REMOVED) if (removed.matchesRecipe(input, true)) return -1;
        return row;
    }

    /**
     * Resolves a sawmill input on the server. Log output deliberately comes from the active vanilla
     * crafting table, preserving modded-log plank conversions instead of hardcoding wood species.
     */
    public static ItemStack getOutput(Level level, ItemStack input) {
        if (input.isEmpty()) return ItemStack.EMPTY;
        init();
        SawmillRecipe scripted = scripted(input);
        if (scripted != null) return scripted.output().copy();
        int row = builtinRow(input);
        if (row == 2) return new ItemStack(ModItems.powder_sawdust.get());
        if (row == 0) {
            return Recipes.craftingResult(level, input)
                    .filter(result -> result.is(ItemTags.PLANKS))
                    .map(result -> {
                        result.setCount(result.getCount() * 6 / 4);
                        return result;
                    })
                    .orElse(ItemStack.EMPTY);
        }
        if (row == 1) return new ItemStack(Items.STICK, 6);
        if (row == 3) return new ItemStack(Items.STICK);
        return ItemStack.EMPTY;
    }

    public static ItemStack rollSecondary(ItemStack input, ItemStack result, RandomSource random) {
        init();
        SawmillRecipe scripted = scripted(input);
        if (scripted != null) {
            if (scripted.secondary().isEmpty() || random.nextInt(100) >= scripted.secondaryChance()) return ItemStack.EMPTY;
            return scripted.secondary().copy();
        }
        if (result.is(ModItems.powder_sawdust.get())) return ItemStack.EMPTY;
        int chance = result.is(Items.STICK) ? 10 : 50;
        return random.nextInt(100) < chance ? new ItemStack(ModItems.powder_sawdust.get()) : ItemStack.EMPTY;
    }

    public static boolean isValidInput(ItemStack input) {
        if (input.isEmpty()) return false;
        init();
        return scripted(input) != null || builtinRow(input) >= 0;
    }
}
