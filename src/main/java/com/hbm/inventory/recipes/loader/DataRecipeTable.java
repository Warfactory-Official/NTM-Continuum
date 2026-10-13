package com.hbm.inventory.recipes.loader;

import com.hbm.Constants;
import com.mojang.serialization.MapCodec;
import dev.golem.core.hook.Hook;
import dev.golem.core.world.RecipeFields;
import dev.golem.core.world.RecipeTables;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A machine's recipes as a datapack table whose rows are a record of the machine's own shape (for tables that are not
 * {@link GenericRecipe}s; those use {@link DataGenericRecipes}). Rows are recipe files of type {@code <ns>:<name>}
 * ({@code hbm}, or an addon's namespace) in {@code data/<ns>/recipe/<name>/}; datapacks, KubeJS and addons add and
 * replace them, and remove HBM's with {@code data/<own ns>/recipe_removals} lists. The owning class keeps its lookup API and rebuilds its index on
 * {@link #changed}.
 *
 * <p>Row records must not hold an {@code ItemStack} built while decoding (26.1 decodes recipes before item components
 * exist): use {@link GenericRecipeCodec.ItemOut} and build the stack when the machine needs it.
 */
public final class DataRecipeTable<R> {

    public final RecipeTables.Table<R> table;
    private volatile List<R> rows = List.of();
    private volatile List<RecipeTables.Entry<R>> entries = List.of();
    private volatile boolean loaded;

    /** Fires after the rows changed (server reload, client sync); owners rebuild their indexes here. */
    public final Hook<Runnable> changed = Hook.notifying();

    /** The table {@code hbm:<name>}. */
    public DataRecipeTable(String name, Supplier<MapCodec<R>> codec) {
        this(Constants.MOD_ID, name, codec);
    }

    /**
     * The table {@code <namespace>:<name>}: an addon declares its own record-shaped tables with its mod id (rows in
     * {@code data/<ns>/recipe/<name>/}, type {@code <namespace>:<name>}).
     */
    public DataRecipeTable(String namespace, String name, Supplier<MapCodec<R>> codec) {
        this.table = RecipeTables.table(namespace, name, codec);
        table.changed.register(this::reload);
    }

    /**
     * States the row's fields for scripts ({@link RecipeTables.Table#fields(RecipeFields)}; shapes for HBM's JSON forms
     * are in {@link RecipeShapes}). Returns this table.
     */
    public DataRecipeTable<R> fields(RecipeFields fields) {
        table.fields(fields);
        return this;
    }

    private synchronized void reload() {
        List<R> next = new ArrayList<>();
        List<RecipeTables.Entry<R>> current = table.entries();
        for (RecipeTables.Entry<R> entry : current) next.add(entry.value());
        entries = current;
        rows = List.copyOf(next);
        loaded = true;
        for (Runnable listener : changed) listener.run();
    }

    /** The current rows, in recipe-id order. */
    public List<R> rows() {
        if (!loaded) reload();
        return rows;
    }

    /** The current rows with their recipe ids ({@code hbm:ammo_press/m357_bp}), in recipe-id order. */
    public List<RecipeTables.Entry<R>> entries() {
        if (!loaded) reload();
        return entries;
    }
}
