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
    /** Discoverable blueprint pools from the legacy recipe catalogue. */
    public static final String POOL_PREFIX_DISCOVER = "discover.";
    /** Secret blueprint pools from the legacy recipe catalogue. */
    public static final String POOL_PREFIX_SECRET = "secret.";
    /** 528-mode blueprint pools from the legacy precision assembler. */
    public static final String POOL_PREFIX_528 = "528.";

    public final List<GenericRecipe> recipeOrderedList = new ArrayList<>();
    public final Map<String, GenericRecipe> recipeNameMap = new HashMap<>();
    public final Map<String, List<GenericRecipe>> autoSwitchGroups = new HashMap<>();

    /** pool name → recipe internal names in that pool. */
    public static final Map<String, List<String>> blueprintPools = new HashMap<>();
    /** recipe internal name → recipe, for blueprint tooltip display. */
    public static final Map<String, GenericRecipe> pooledBlueprints = new HashMap<>();

    private boolean registered;

    /** Builds the set on first use, then applies what startup scripts declared for it. */
    public synchronized void registerRecipes() {
        if (registered && !stale()) return;
        if (registered) clear();
        registered = true;
        populate();
        dev.golem.core.content.Replay.apply(this);
    }

    /** Empties the set; the next {@link #registerRecipes} rebuilds it and re-applies script edits. */
    public synchronized void reset() {
        List.copyOf(recipeOrderedList).forEach(recipe -> remove(recipe.getInternalName()));
        clear();
        registered = false;
    }

    /** The machine's built-in recipes. */
    protected abstract void populate();

    /** A built set that must be rebuilt from scratch. */
    protected boolean stale() {
        return false;
    }

    protected void clear() {
        recipeOrderedList.clear();
        recipeNameMap.clear();
        autoSwitchGroups.clear();
    }

    protected void register(GenericRecipe recipe) {
        if (recipeNameMap.containsKey(recipe.getInternalName())) {
            throw new IllegalStateException("Recipe " + recipe.getInternalName()
                    + " has been registered with a duplicate ID!");
        }
        recipeOrderedList.add(recipe);
        recipeNameMap.put(recipe.getInternalName(), recipe);
    }

    /** Public registration entry point for the generated recipe classes (com.hbm.inventory.recipes.gen). */
    public void registerGenerated(GenericRecipe recipe) {
        register(recipe);
    }

    /** Drops a recipe from the set, its auto-switch group and every blueprint pool. */
    public boolean remove(String name) {
        GenericRecipe recipe = recipeNameMap.remove(name);
        if (recipe == null) return false;
        recipeOrderedList.remove(recipe);
        autoSwitchGroups.values().forEach(group -> group.remove(recipe));
        blueprintPools.values().forEach(pool -> pool.remove(name));
        pooledBlueprints.remove(name);
        return true;
    }

    public GenericRecipe getRecipe(String name) {
        return recipeNameMap.get(name);
    }

    public static void addToPool(String pool, GenericRecipe recipe) {
        blueprintPools.computeIfAbsent(pool, k -> new ArrayList<>()).add(recipe.getInternalName());
        pooledBlueprints.put(recipe.getInternalName(), recipe);
    }

    public void addToGroup(String group, GenericRecipe recipe) {
        autoSwitchGroups.computeIfAbsent(group, k -> new ArrayList<>()).add(recipe);
    }
}
