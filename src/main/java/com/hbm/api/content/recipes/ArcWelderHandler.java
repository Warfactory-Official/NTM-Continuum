package com.hbm.api.content.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.recipes.ArcWelderRecipes;
import com.hbm.inventory.recipes.ArcWelderRecipes.ArcWelderRecipe;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.api.content.Spec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * e.arcWelder.add('2x hbm:motor', 100, 400, ['2x #c:plates/steel', '2x hbm:wire_dense_mingrade'])
 * e.arcWelder.add('hbm:plate_welded_osmiridium', 6000, 20000000, ['2x #c:plates_cast/osmiridium'], '16000x reformgas')
 * e.arcWelder.removeByOutput('hbm:motor')
 * e.arcWelder.removeByInput('hbm:ingot_fiberglass')
 * e.arcWelder.removeAll()
 * ids: hbm:arc_welder/&lt;first input&gt;
 * </pre>
 * Up to three unordered inputs; duration in ticks, power in HE/tick.
 */
public final class ArcWelderHandler extends TableHandler {

    @Override
    protected Object registry() {
        return ArcWelderRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        ArcWelderRecipes.register();
        return RecipeIds.views(machine(), List.copyOf(ArcWelderRecipes.recipes),
                recipe -> recipe.ingredients.length == 0 ? "" : RecipeIds.bare(RecipeIds.spec(recipe.ingredients[0])),
                recipe -> {
                    List<String> in = new ArrayList<>();
                    for (AStack ingredient : recipe.ingredients) in.add(RecipeIds.spec(ingredient));
                    if (recipe.fluid != null) in.add(RecipeIds.spec(recipe.fluid));
                    return in;
                },
                recipe -> List.of(RecipeIds.spec(recipe.output)));
    }

    @Override
    protected void delete(RecipeView view) {
        ArcWelderRecipe target = (ArcWelderRecipe) view.recipe();
        ArcWelderRecipes.removeIf(recipe -> recipe == target);
    }

    @Override
    public void reset() {
        ArcWelderRecipes.reset();
    }

    public void add(Spec.Item output, int duration, long power, Spec.Ingredient[] inputs) {
        add(output, duration, power, inputs, null);
    }

    public void add(Spec.Item output, int duration, long power, Spec.Ingredient[] inputs, Spec.Fluid fluid) {
        edit("add " + output, () -> ArcWelderRecipes.put(
                new ArcWelderRecipe(ScriptSpecs.stack(output), duration, power,
                        fluid == null ? null : ScriptSpecs.fluid(fluid), ScriptSpecs.ingredients(inputs))));
    }

    public void removeByOutput(Spec.Item item) {
        edit("removeByOutput " + item, () -> {
            Item target = ScriptSpecs.item(item);
            ArcWelderRecipes.removeIf(recipe -> recipe.output.is(target));
        });
    }

    public void removeByInput(Spec.Item item) {
        edit("removeByInput " + item, () -> {
            ItemStack probe = new ItemStack(ScriptSpecs.item(item));
            ArcWelderRecipes.removeIf(recipe -> {
                for (AStack in : recipe.ingredients) if (in.matchesRecipe(probe, true)) return true;
                return false;
            });
        });
    }

    public void removeAll() {
        edit("removeAll", () -> ArcWelderRecipes.removeIf(recipe -> true));
    }
}
