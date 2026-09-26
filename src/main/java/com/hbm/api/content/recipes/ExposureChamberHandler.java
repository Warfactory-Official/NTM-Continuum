package com.hbm.api.content.recipes;

import com.hbm.inventory.recipes.ExposureChamberRecipes;
import com.hbm.inventory.recipes.ExposureChamberRecipes.ExposureChamberRecipe;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.api.content.Spec;
import net.minecraft.world.item.Item;

import java.util.List;

/**
 * <pre>
 * e.exposureChamber.add('kubejs.gold', 'hbm:particle_higgs', '#c:ingots/gold', 'hbm:ingot_schrabidium')
 * e.exposureChamber.remove('exposure.uranium')
 * e.exposureChamber.removeByOutput('hbm:ingot_euphemium')
 * e.exposureChamber.removeAll()
 * ids: hbm:exposure_chamber/&lt;name&gt;
 * </pre>
 * Particle and ingredient: item or #tag; one of each per cycle.
 */
public final class ExposureChamberHandler extends TableHandler {

    private static final ExposureChamberRecipes SET = ExposureChamberRecipes.INSTANCE;

    @Override
    protected Object registry() {
        return SET;
    }

    @Override
    public List<RecipeView> views() {
        SET.registerRecipes();
        return RecipeIds.views(machine(), List.copyOf(ExposureChamberRecipes.recipes), recipe -> recipe.name,
                recipe -> List.of(RecipeIds.spec(recipe.particle), RecipeIds.spec(recipe.ingredient)),
                recipe -> List.of(RecipeIds.spec(recipe.output)));
    }

    @Override
    protected void delete(RecipeView view) {
        SET.remove(((ExposureChamberRecipe) view.recipe()).name);
    }

    @Override
    public void reset() {
        SET.reset();
    }

    public void add(String name, Spec.Ingredient particle, Spec.Ingredient ingredient, Spec.Item output) {
        edit("add " + name, () -> ExposureChamberRecipes.put(name,
                ScriptSpecs.ingredient(particle), ScriptSpecs.ingredient(ingredient), ScriptSpecs.stack(output)));
    }

    public void remove(String name) {
        edit("remove " + name, () -> {
            if (!SET.remove(name)) throw new IllegalArgumentException("no recipe named " + name);
        });
    }

    public void removeByOutput(Spec.Item item) {
        edit("removeByOutput " + item, () -> {
            Item target = ScriptSpecs.item(item);
            for (ExposureChamberRecipe recipe : List.copyOf(ExposureChamberRecipes.recipes)) {
                if (recipe.output.is(target)) SET.remove(recipe.name);
            }
        });
    }

    public void removeAll() {
        edit("removeAll", () -> {
            for (ExposureChamberRecipe recipe : List.copyOf(ExposureChamberRecipes.recipes)) SET.remove(recipe.name);
        });
    }
}
