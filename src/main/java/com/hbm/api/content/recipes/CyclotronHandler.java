package com.hbm.api.content.recipes;

import com.hbm.inventory.recipes.CyclotronRecipes;
import com.hbm.inventory.recipes.CyclotronRecipes.CyclotronRecipe;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.api.content.Spec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * <pre>
 * e.cyclotron.add('hbm:part_lithium', '#c:dusts/lithium', 'hbm:powder_beryllium', 50)   antimatter in mB
 * e.cyclotron.removeByInput('hbm:powder_strontium')
 * e.cyclotron.removeByParticle('hbm:part_carbon')
 * e.cyclotron.removeByOutput('hbm:powder_gold')
 * e.cyclotron.removeAll()
 * ids: hbm:cyclotron/&lt;input&gt;
 * </pre>
 * Particle: exact item. First matching recipe wins; an add takes precedence over every earlier recipe.
 */
public final class CyclotronHandler extends TableHandler {

    @Override
    protected Object registry() {
        return CyclotronRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        CyclotronRecipes.register();
        return RecipeIds.views(machine(), List.copyOf(CyclotronRecipes.recipes), recipe -> RecipeIds.bare(RecipeIds.spec(recipe.input)),
                recipe -> List.of(RecipeIds.spec(recipe.input), RecipeIds.spec(recipe.particle)),
                recipe -> List.of(RecipeIds.spec(recipe.output)));
    }

    @Override
    protected void delete(RecipeView view) {
        CyclotronRecipe target = (CyclotronRecipe) view.recipe();
        CyclotronRecipes.removeIf(recipe -> recipe == target);
    }

    @Override
    public void reset() {
        CyclotronRecipes.reset();
    }

    public void add(Spec.Item particle, Spec.Ingredient input, Spec.Item output, int antimatter) {
        edit("add " + output, () -> CyclotronRecipes.putFirst(
                ScriptSpecs.item(particle), ScriptSpecs.ingredient(input), ScriptSpecs.stack(output), antimatter));
    }

    public void removeByInput(Spec.Item item) {
        edit("removeByInput " + item, () -> {
            ItemStack probe = new ItemStack(ScriptSpecs.item(item));
            CyclotronRecipes.removeIf(recipe -> recipe.input.matchesRecipe(probe, true));
        });
    }

    public void removeByParticle(Spec.Item item) {
        edit("removeByParticle " + item, () -> {
            Item target = ScriptSpecs.item(item);
            CyclotronRecipes.removeIf(recipe -> recipe.particle.item == target);
        });
    }

    public void removeByOutput(Spec.Item item) {
        edit("removeByOutput " + item, () -> {
            Item target = ScriptSpecs.item(item);
            CyclotronRecipes.removeIf(recipe -> recipe.output.is(target));
        });
    }

    public void removeAll() {
        edit("removeAll", () -> CyclotronRecipes.removeIf(recipe -> true));
    }
}
