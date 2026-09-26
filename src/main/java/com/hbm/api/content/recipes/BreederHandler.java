package com.hbm.api.content.recipes;

import com.hbm.inventory.recipes.BreederRecipes;
import com.hbm.inventory.recipes.BreederRecipes.BreederRecipe;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.api.content.Spec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * <pre>
 * e.breeder.add('hbm:rod_lithium', 'hbm:rod_tritium', 200)     input may be a #tag; replaces any entry
 * e.breeder.remove('hbm:rod_lithium')
 * e.breeder.removeAll()
 * ids: hbm:breeder/&lt;input item&gt;
 * </pre>
 * Flux: total neutron flux the rod must absorb.
 */
public final class BreederHandler extends TableHandler {

    @Override
    protected Object registry() {
        return BreederRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        return RecipeIds.views(machine(), List.copyOf(BreederRecipes.getAllRecipes().entrySet()),
                entry -> RecipeIds.spec(entry.getKey()),
                entry -> List.of(RecipeIds.spec(entry.getKey())),
                entry -> List.of(RecipeIds.spec(entry.getValue().output())));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void delete(RecipeView view) {
        BreederRecipes.remove(((Map.Entry<Item, BreederRecipe>) view.recipe()).getKey());
    }

    @Override
    public void reset() {
        BreederRecipes.reset();
    }

    public void add(Spec.Ingredient input, Spec.Item output, int flux) {
        edit("add " + input, () -> {
            ItemStack out = ScriptSpecs.stack(output);
            for (Item item : ScriptSpecs.items(input)) BreederRecipes.put(item, new BreederRecipe(out.copy(), flux));
        });
    }

    public void remove(Spec.Ingredient input) {
        edit("remove " + input, () -> {
            for (Item item : ScriptSpecs.items(input)) BreederRecipes.remove(item);
        });
    }

    public void removeAll() {
        edit("removeAll", BreederRecipes::removeAll);
    }
}
