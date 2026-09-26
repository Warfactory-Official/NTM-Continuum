package com.hbm.scripting;

import com.hbm.api.content.RecipesEvent;
import com.hbm.api.content.recipes.RecipeView;
import com.hbm.api.content.recipes.TableHandler;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * The {@code NTMRecipes} global in server and client scripts: machine recipe tables as built, after
 * every script edit.
 *
 * <pre>
 * NTMRecipes.machines()                                  ['assembly', 'shredder', ...]
 * NTMRecipes.list('centrifuge')                          RecipeView[]: id, machine, inputs, outputs, recipe
 * NTMRecipes.ids('assembly')
 * NTMRecipes.get('hbm:assembly/ass.plateiron')           null when absent
 * NTMRecipes.find(r => r.hasOutput('hbm:ingot_steel'))    across every machine
 * </pre>
 */
public final class NTMRecipes {

    public static final NTMRecipes INSTANCE = new NTMRecipes();

    private final Map<String, TableHandler> tables = RecipesEvent.handlers();

    private NTMRecipes() {}

    public List<String> machines() {
        return List.copyOf(tables.keySet());
    }

    public List<RecipeView> list(String machine) {
        TableHandler table = tables.get(machine);
        if (table == null) throw new IllegalArgumentException("Unknown machine " + machine + "; see machines()");
        return table.views();
    }

    public List<String> ids(String machine) {
        return list(machine).stream().map(RecipeView::id).toList();
    }

    public @Nullable RecipeView get(String id) {
        return list(RecipesEvent.machineOf(id)).stream().filter(view -> view.id().equals(id)).findFirst().orElse(null);
    }

    public List<RecipeView> find(java.util.function.Predicate<RecipeView> filter) {
        return tables.values().stream().flatMap(table -> table.views().stream()).filter(filter).toList();
    }
}
