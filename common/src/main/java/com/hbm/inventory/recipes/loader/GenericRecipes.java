package com.hbm.inventory.recipes.loader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base recipe registry for a machine, ported (trimmed) from HBM's {@code GenericRecipes}. Holds the
 * name→recipe map, the blueprint pools (which template unlocks which recipes) and the auto-switch
 * groups (a machine swaps to the matching recipe in a group based on the inserted ingredient).
 */
public abstract class GenericRecipes {

    /** Pool prefix: lets a single "alt" template cover a family of recipes (e.g. all plates). */
    public static final String POOL_PREFIX_ALT = "alt.";

    public final List<GenericRecipe> recipeOrderedList = new ArrayList<>();
    public final Map<String, GenericRecipe> recipeNameMap = new HashMap<>();
    public final Map<String, List<GenericRecipe>> autoSwitchGroups = new HashMap<>();

    /** pool name → recipe internal names in that pool. */
    public static final Map<String, List<String>> blueprintPools = new HashMap<>();

    /** Populates {@link #recipeNameMap}/{@link #recipeOrderedList} — implemented per machine. */
    public abstract void registerRecipes();

    protected void register(GenericRecipe recipe) {
        recipeOrderedList.add(recipe);
        recipeNameMap.put(recipe.getInternalName(), recipe);
    }

    /** Public registration entry point for the generated recipe classes (com.hbm.inventory.recipes.gen). */
    public void registerGenerated(GenericRecipe recipe) {
        register(recipe);
    }

    public GenericRecipe getRecipe(String name) {
        return recipeNameMap.get(name);
    }

    public static void addToPool(String pool, GenericRecipe recipe) {
        blueprintPools.computeIfAbsent(pool, k -> new ArrayList<>()).add(recipe.getInternalName());
    }

    public void addToGroup(String group, GenericRecipe recipe) {
        autoSwitchGroups.computeIfAbsent(group, k -> new ArrayList<>()).add(recipe);
    }
}
