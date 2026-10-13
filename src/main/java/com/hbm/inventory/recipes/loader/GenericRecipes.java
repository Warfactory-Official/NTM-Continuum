package com.hbm.inventory.recipes.loader;

import com.hbm.Constants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base recipe registry for a machine, ported (trimmed) from HBM's {@code GenericRecipes}. Holds the
 * name->recipe map, the blueprint pools (which template unlocks which recipes) and the auto-switch
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

    /** pool name -> recipe internal names in that pool. */
    public static final Map<String, List<String>> blueprintPools = new HashMap<>();
    /** recipe internal name -> recipe, for blueprint tooltip display. */
    public static final Map<String, GenericRecipe> pooledBlueprints = new HashMap<>();

    /** Populates {@link #recipeNameMap}/{@link #recipeOrderedList}, implemented per machine. */
    public abstract void registerRecipes();

    /** Adds a recipe built in code and joins its blueprint pools and auto-switch group. */
    protected void register(GenericRecipe recipe) {
        put(recipe);
        if (recipe.blueprintPools != null) for (String pool : recipe.blueprintPools) addToPool(pool, recipe);
        if (recipe.autoSwitchGroup != null) addToGroup(recipe.autoSwitchGroup, recipe);
    }

    private void put(GenericRecipe recipe) {
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

    /**
     * Replaces every row with {@code recipes}, in that order (a datapack table after a reload or a client sync), and
     * keeps this table's blueprint pools and auto-switch groups in step. A second row with an internal name already
     * taken is skipped with a warning, as ntm-next's tables do; the first one in order wins.
     */
    protected synchronized void rebuild(List<GenericRecipe> recipes) {
        for (GenericRecipe old : recipeOrderedList) {
            pooledBlueprints.remove(old.getInternalName(), old);
            // every pool the row could have joined: a config change swaps the pools of the same row object
            leavePools(old, old.blueprintPools);
            leavePools(old, old.normalPools());
            leavePools(old, old.blueprintPools528);
        }
        recipeOrderedList.clear();
        recipeNameMap.clear();
        autoSwitchGroups.clear();
        for (GenericRecipe recipe : recipes) {
            if (recipeNameMap.containsKey(recipe.getInternalName())) {
                Constants.LOG.warn("Recipe name {} appears twice in {}; keeping the first",
                        recipe.getInternalName(), getClass().getSimpleName());
                continue;
            }
            register(recipe);
        }
    }

    private static void leavePools(GenericRecipe recipe, String[] pools) {
        if (pools == null) return;
        for (String pool : pools) {
            List<String> names = blueprintPools.get(pool);
            if (names != null) names.remove(recipe.getInternalName());
        }
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
