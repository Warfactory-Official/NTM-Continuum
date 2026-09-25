package com.hbm.scripting.recipes;

import com.hbm.inventory.recipes.StorageDrumRecipes;
import com.hbm.inventory.recipes.StorageDrumRecipes.Recipe;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.scripting.Spec;
import net.minecraft.world.item.Item;

import java.util.List;

/**
 * <pre>
 * e.storageDrum.add('hbm:ingot_au198', 'hbm:bottle_mercury', 2160)
 * e.storageDrum.add('hbm:ingot_au198', 'hbm:bottle_mercury', 2160, 500, 500)
 * e.storageDrum.removeByInput('hbm:nuclear_waste_long')
 * e.storageDrum.removeByOutput('hbm:bottle_mercury')
 * e.storageDrum.removeAll()
 * </pre>
 * chance: 1-in-N per tick; liquid/gas: mB of waste released on decay. Input matches item and data exactly.
 * Ids: {@code hbm:storage_drum/<input item>}, data variants suffixed in table order, e.g. {@code hbm:storage_drum/hbm.nuclear_waste_long_2}.
 */
public final class StorageDrumHandler extends TableHandler {

    @Override
    protected Object registry() {
        return StorageDrumRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        return RecipeIds.views(machine(), List.copyOf(StorageDrumRecipes.getAll()),
                recipe -> RecipeIds.bare(RecipeIds.spec(recipe.input())),
                recipe -> List.of(RecipeIds.spec(recipe.input())),
                recipe -> List.of(RecipeIds.spec(recipe.output())));
    }

    @Override
    protected void delete(RecipeView view) {
        Recipe target = (Recipe) view.recipe();
        StorageDrumRecipes.removeIf(recipe -> recipe == target);
    }

    @Override
    public void reset() {
        StorageDrumRecipes.reset();
    }

    public void add(Spec.Item input, Spec.Item output, int chance) {
        add(input, output, chance, 0, 0);
    }

    public void add(Spec.Item input, Spec.Item output, int chance, int liquid, int gas) {
        edit("add " + input, () ->
                StorageDrumRecipes.addRecipe(ScriptSpecs.stack(input), ScriptSpecs.stack(output), chance, liquid, gas));
    }

    public void removeByInput(Spec.Ingredient input) {
        edit("removeByInput " + input, () -> {
            List<Item> items = ScriptSpecs.items(input);
            StorageDrumRecipes.removeIf(recipe -> items.contains(recipe.input().getItem()));
        });
    }

    public void removeByOutput(Spec.Ingredient output) {
        edit("removeByOutput " + output, () -> {
            List<Item> items = ScriptSpecs.items(output);
            StorageDrumRecipes.removeIf(recipe -> items.contains(recipe.output().getItem()));
        });
    }

    public void removeAll() {
        edit("removeAll", () -> StorageDrumRecipes.removeIf(recipe -> true));
    }
}
