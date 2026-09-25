package com.hbm.scripting.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.recipes.AmmoPressRecipes;
import com.hbm.inventory.recipes.AmmoPressRecipes.AmmoPressRecipe;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.scripting.Spec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * <pre>
 * e.ammoPress.add('16x hbm:ammo_standard', [null, '2x #c:ingots/lead', null, null, 'minecraft:gunpowder', null, null, 'hbm:casing', null])
 * e.ammoPress.removeByOutput('hbm:ammo_standard')
 * e.ammoPress.removeByInput('minecraft:gunpowder')
 * e.ammoPress.removeAll()
 * ids: hbm:ammo_press/&lt;first grid input&gt;
 * </pre>
 * Grid: exactly nine cells, row-major, null for empty. Selection is by list position, so removals renumber.
 */
public final class AmmoPressHandler extends TableHandler {

    @Override
    protected Object registry() {
        return AmmoPressRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        AmmoPressRecipes.register();
        return RecipeIds.views(machine(), List.copyOf(AmmoPressRecipes.recipes),
                recipe -> Arrays.stream(recipe.input).filter(Objects::nonNull).findFirst().map(in -> RecipeIds.bare(RecipeIds.spec(in))).orElse(""),
                recipe -> Arrays.stream(recipe.input).filter(Objects::nonNull).map(RecipeIds::spec).toList(),
                recipe -> List.of(RecipeIds.spec(recipe.output)));
    }

    @Override
    protected void delete(RecipeView view) {
        AmmoPressRecipe target = (AmmoPressRecipe) view.recipe();
        AmmoPressRecipes.removeIf(recipe -> recipe == target);
    }

    @Override
    public void reset() {
        AmmoPressRecipes.reset();
    }

    public void add(Spec.Item output, Spec.Ingredient... grid) {
        edit("add " + output, () -> {
            AStack[] input = new AStack[grid.length];
            for (int i = 0; i < grid.length; i++) input[i] = grid[i] == null ? null : ScriptSpecs.ingredient(grid[i]);
            AmmoPressRecipes.put(ScriptSpecs.stack(output), input);
        });
    }

    public void removeByOutput(Spec.Item item) {
        edit("removeByOutput " + item, () -> {
            Item target = ScriptSpecs.item(item);
            AmmoPressRecipes.removeIf(recipe -> recipe.output.is(target));
        });
    }

    public void removeByInput(Spec.Item item) {
        edit("removeByInput " + item, () -> {
            ItemStack probe = new ItemStack(ScriptSpecs.item(item));
            AmmoPressRecipes.removeIf(recipe -> {
                for (AStack cell : recipe.input) if (cell != null && cell.matchesRecipe(probe, true)) return true;
                return false;
            });
        });
    }

    public void removeAll() {
        edit("removeAll", () -> AmmoPressRecipes.removeIf(recipe -> true));
    }
}
