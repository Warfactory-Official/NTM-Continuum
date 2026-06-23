package com.hbm.inventory.recipes.loader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class GenericRecipes {

    public static final String POOL_PREFIX_ALT = "alt.";

    public final List<GenericRecipe> recipeOrderedList = new ArrayList<>();
    public final Map<String, GenericRecipe> recipeNameMap = new HashMap<>();
    public final Map<String, List<GenericRecipe>> autoSwitchGroups = new HashMap<>();

    public static final Map<String, List<String>> blueprintPools = new HashMap<>();

    public abstract void registerRecipes();

    protected void register(GenericRecipe recipe) {
        recipeOrderedList.add(recipe);
        recipeNameMap.put(recipe.getInternalName(), recipe);
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
