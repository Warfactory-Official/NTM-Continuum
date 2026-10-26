package com.hbm.inventory.recipes.loader;

import com.hbm.api.data.FluidDataMaps;
import com.hbm.config.GeneralConfig;
import com.mojang.serialization.MapCodec;
import dev.golem.core.world.RecipeFields;
import dev.golem.core.world.RecipeTables;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/**
 * A machine's recipes as a datapack table (ntm-next's approach): every row is a recipe file of type
 * {@code <namespace>:<name>} in the {@link GenericRecipeCodec} schema, so datapacks, KubeJS and addons add, replace
 * and remove rows like any recipe. The lists inherited from {@link GenericRecipes} always hold the current rows in
 * display order ({@code order}, then recipe id): rebuilt after every server reload, on a client after every sync, and
 * whenever a {@link RecipeConditions} flag changed since the last build.
 *
 * <p>A rebuild keeps the rows whose {@code requires} hold (1.7's config branches at registration), puts each row's
 * config-mode variants in effect ({@link GenericRecipe#applyModes}), then lets the table add rule rows and derive its
 * own indexes ({@link #arrange}, {@link #rebuilt}).
 *
 * @param <T> the row type ({@link GenericRecipe}, or a subclass with fields of its own)
 */
public abstract class DataGenericRecipes<T extends GenericRecipe> extends GenericRecipes {

    public final RecipeTables.Table<T> table;
    /** {@link RecipeConditions#fingerprint} of the last build; null before the first. */
    private volatile String builtFlags;

    /**
     * A table of {@code codec}'s rows; its script fields ({@link RecipeTables.Table#fields}) are the generic schema's
     * ({@link GenericRecipeCodec#FIELDS}), so every generic table, an addon's too, is writable from scripts with typed
     * arguments. A subclass's own keys are offered as raw JSON; state them with the overload taking extra fields.
     */
    protected DataGenericRecipes(String namespace, String name, Supplier<MapCodec<T>> codec) {
        this(namespace, name, codec, RecipeFields.of());
    }

    /** As {@link #DataGenericRecipes(String, String, Supplier)}, with {@code extras} (the subclass's keys) after the generic fields. */
    protected DataGenericRecipes(String namespace, String name, Supplier<MapCodec<T>> codec, RecipeFields extras) {
        this.table = RecipeTables.table(namespace, name, codec);
        table.fields(GenericRecipeCodec.FIELDS.addAll(extras));
        table.changed.register(this::reload);
        // icons and outputs that are filled containers take their tint from this map, which a datapack load (an addon's
        // patch) may apply after the recipes: rebuild the stacks when it changes
        FluidDataMaps.FLUID_CONTAINERS.changed.register(() -> {
            if (!recipeOrderedList.isEmpty()) reload();
        });
    }

    @SuppressWarnings("unchecked")
    protected DataGenericRecipes(String namespace, String name) {
        this(namespace, name, () -> (MapCodec<T>) (MapCodec<?>) GenericRecipeCodec.codec());
    }

    private synchronized void reload() {
        boolean expensive = GeneralConfig.enableExpensiveMode;
        boolean mode528 = GeneralConfig.enable528;
        String flags = RecipeConditions.fingerprint();
        List<T> recipes = new ArrayList<>();
        for (RecipeTables.Entry<T> entry : table.entries()) {
            T recipe = entry.value();
            if (!present(recipe)) continue;
            recipe.bindRecipeId(entry.id());
            recipe.bindStacks();
            recipe.applyModes(expensive, mode528);
            recipes.add(recipe);
        }
        recipes.sort(Comparator.comparingInt(recipe -> recipe.order)); // stable: ties stay in recipe-id order
        List<GenericRecipe> rows = new ArrayList<>(recipes);
        arrange(rows);
        rebuild(rows);
        builtFlags = flags;
        rebuilt();
    }

    /** Whether a row's {@link GenericRecipe#requires} hold now ({@link RecipeConditions}). */
    public static boolean present(GenericRecipe recipe) {
        return RecipeConditions.holds(recipe.requires, recipe.getInternalName());
    }

    /**
     * The rows of this build in display order, before they become the table: a table adds its rule rows here (rows 1.7
     * derived from other data at registration, such as one per fluid; a data row of the same name, earlier in the
     * list, wins) or replaces rows with the form it shows. The default changes nothing.
     */
    protected void arrange(List<GenericRecipe> rows) {}

    /** Called after every rebuild, for indexes a table derives from its rows. */
    protected void rebuilt() {}

    /** The current rows of this table's type, in display order. */
    @SuppressWarnings("unchecked")
    public List<T> typedRows() {
        registerRecipes();
        List<T> rows = new ArrayList<>();
        for (GenericRecipe recipe : recipeOrderedList) rows.add((T) recipe);
        return rows;
    }

    @Override
    public GenericRecipe getRecipe(String name) {
        registerRecipes();
        return super.getRecipe(name);
    }

    /**
     * Loads the current rows if nothing has yet (a test or a machine before the first reload notification), or again
     * when a {@link RecipeConditions} flag (expensive mode, 528 mode, LBSM, ...) changed since the last build.
     */
    @Override
    public void registerRecipes() {
        if (recipeOrderedList.isEmpty() || !RecipeConditions.fingerprint().equals(builtFlags)) reload();
    }
}
