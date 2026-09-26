package com.hbm.api.content.recipes;

import com.hbm.inventory.recipes.CombinationRecipes;
import com.hbm.inventory.recipes.CombinationRecipes.CombinationRecipe;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.api.content.Spec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * e.combinationOven.add('#minecraft:logs', 'minecraft:charcoal', '250x woodoil')
 * e.combinationOven.add('minecraft:clay', 'minecraft:bricks', null)          item and fluid output each nullable
 * e.combinationOven.removeByInput('minecraft:sugar_cane')
 * e.combinationOven.removeByOutput('minecraft:charcoal')
 * e.combinationOven.removeAll()
 * ids: hbm:combination_oven/&lt;input&gt;
 * </pre>
 * First matching recipe wins; an add takes precedence over every earlier recipe.
 */
public final class CombinationHandler extends TableHandler {

    @Override
    protected Object registry() {
        return CombinationRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        return RecipeIds.views(machine(), List.copyOf(CombinationRecipes.getRecipes()), recipe -> RecipeIds.bare(RecipeIds.spec(recipe.input())),
                recipe -> List.of(RecipeIds.spec(recipe.input())),
                recipe -> {
                    List<String> out = new ArrayList<>();
                    if (recipe.itemOutput() != null) out.add(RecipeIds.spec(recipe.itemOutput()));
                    if (recipe.fluidOutput() != null) out.add(RecipeIds.spec(recipe.fluidOutput()));
                    return out;
                });
    }

    @Override
    protected void delete(RecipeView view) {
        Object target = view.recipe();
        CombinationRecipes.removeIf(recipe -> recipe == target);
    }

    @Override
    public void reset() {
        CombinationRecipes.reset();
    }

    public void add(Spec.Ingredient input, Spec.Item itemOutput, Spec.Fluid fluidOutput) {
        edit("add " + input, () -> CombinationRecipes.putFirst(
                new CombinationRecipe(ScriptSpecs.ingredient(input),
                        itemOutput == null ? null : ScriptSpecs.stack(itemOutput),
                        fluidOutput == null ? null : ScriptSpecs.fluid(fluidOutput))));
    }

    public void removeByInput(Spec.Item item) {
        edit("removeByInput " + item, () -> {
            ItemStack probe = new ItemStack(ScriptSpecs.item(item));
            CombinationRecipes.removeIf(recipe -> recipe.input().matchesRecipe(probe, true));
        });
    }

    public void removeByOutput(Spec.Item item) {
        edit("removeByOutput " + item, () -> {
            Item target = ScriptSpecs.item(item);
            CombinationRecipes.removeIf(recipe -> recipe.itemOutput() != null && recipe.itemOutput().is(target));
        });
    }

    public void removeAll() {
        edit("removeAll", () -> CombinationRecipes.removeIf(recipe -> true));
    }
}
