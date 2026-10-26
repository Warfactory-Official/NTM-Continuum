package com.hbm.inventory.recipes.loader;

import dev.golem.core.world.RecipeSource;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The {@link RecipeSource} of a {@link GenericRecipe} table (HBM's tables in {@code src/recipes}, and an addon's rows for
 * them). 1.7's {@code this.register(new GenericRecipe("ass.plateiron")...)} lines
 * read the same here; each row takes 1.7's position as its {@code order} (the machine's recipe list order) and is
 * written as {@code <name without its prefix>.json} ({@code ass.plateiron} → {@code plateiron.json}), the ids the tables
 * have had since they became data.
 *
 * @param <T> the row type ({@link GenericRecipe}, or a subclass with fields of its own)
 */
public abstract class GenericTableSource<T extends GenericRecipe> extends RecipeSource<T> {

    private final Map<String, Integer> slotsAfter = new HashMap<>();

    protected GenericTableSource(DataGenericRecipes<T> recipes) {
        super(recipes.table);
    }

    /** 1.7 {@code register(recipe)}. */
    public void register(T recipe) {
        register(id(recipe), recipe);
    }

    /** A row under an id of its own (two rows of one name, told apart by their {@code requires}). */
    @Override
    public void register(String id, T recipe) {
        if (ordered()) recipe.order = Math.multiplyExact(nextOrder(), orderStride());
        add(id, recipe);
    }

    /**
     * Adds an authored addon row immediately after a loaded base anchor, retaining registration order within
     * that anchor's reserved stride. The base source opts into the stride through {@link #orderStride}.
     */
    protected final void registerAfter(String anchor, String id, T recipe, int stride) {
        var baseOrder = orderOf(anchor);
        if (baseOrder % stride != 0) {
            throw new IllegalStateException("Base anchor does not reserve addon positions: " + anchor
                    + "; regenerate the base table with its ordered source");
        }
        var slot = slotsAfter.merge(anchor, 1, Integer::sum);
        if (slot >= stride) throw new IllegalStateException(slot + " addon rows after " + anchor);
        recipe.order = Math.addExact(baseOrder, slot);
        add(id, recipe);
    }

    /** A table reserving positions for addon insertion overrides this with its runtime ordering stride. */
    protected int orderStride() { return 1; }

    /**
     * The {@code order} of a loaded row ({@link #loaded}), e.g. {@code hbm:press/plate_titanium}: an addon row that
     * follows or replaces a base row takes that row's position, and sorts right after it (ties sort by recipe id).
     */
    protected final int orderOf(String recipeId) {
        return loaded(recipeId).order;
    }

    /** Whether rows carry {@code order}; false for a table looked up by input, where position means nothing. */
    protected boolean ordered() {
        return true;
    }

    /** The file id of a row: its name after the first dot, lower case. */
    protected String id(T recipe) {
        String name = recipe.getInternalName();
        int dot = name.indexOf('.');
        return (dot < 0 ? name : name.substring(dot + 1)).toLowerCase(Locale.ROOT);
    }
}
