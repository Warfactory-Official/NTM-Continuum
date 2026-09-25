package com.hbm.scripting.recipes;

import com.hbm.Constants;
import com.hbm.scripting.ScriptedRecipes;

import java.util.List;
import java.util.function.Predicate;

/**
 * One machine's recipe table, addressable by recipe id.
 *
 * <pre>
 * e.&lt;machine&gt;.removeById('hbm:centrifuge/minecraft.iron_ore')
 * e.&lt;machine&gt;.removeIf(r => r.hasOutput('minecraft:gold_nugget'))    r: RecipeView (id, inputs, outputs)
 * </pre>
 *
 * Subclasses: {@link #registry} is the {@code ScriptedRecipes} key, {@link #views} builds the table if
 * needed, {@link #delete} drops one view's recipe ({@code UnsupportedOperationException}: the entry
 * cannot go, {@link #removeIf} keeps it), {@link #reset} empties the table so it rebuilds.
 */
public abstract class TableHandler {

    private String machine;

    public final String machine() {
        return machine;
    }

    public final void bind(String machine) {
        this.machine = machine;
    }

    protected abstract Object registry();

    public abstract List<RecipeView> views();

    protected abstract void delete(RecipeView view);

    public abstract void reset();

    protected final void edit(String description, Runnable op) {
        ScriptedRecipes.enqueue(registry(), machine + " " + description, op);
    }

    public void removeById(String id) {
        edit("removeById " + id, () -> {
            RecipeView view = views().stream().filter(v -> v.id().equals(id)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("no recipe " + id));
            delete(view);
        });
    }

    public void removeIf(Predicate<RecipeView> filter) {
        edit("removeIf", () -> {
            for (RecipeView view : List.copyOf(views())) {
                if (!filter.test(view)) continue;
                try {
                    delete(view);
                } catch (UnsupportedOperationException e) {
                    Constants.LOG.warn("[scripts] {} kept: {}", view.id(), e.getMessage());
                }
            }
        });
    }
}
