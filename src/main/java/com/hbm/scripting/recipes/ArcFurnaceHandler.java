package com.hbm.scripting.recipes;

import com.hbm.foundry.ArcFurnaceRecipes;
import com.hbm.foundry.ArcFurnaceRecipes.ArcFurnaceRecipe;
import com.hbm.inventory.material.Mats;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.scripting.Spec;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * e.arcFurnace.add('#minecraft:sand', 'hbm:nugget_silicon', '8x silicon')    solid output, then molten outputs
 * e.arcFurnace.add('minecraft:flint', null, '36x silicon')                     molten only
 * e.arcFurnace.add('minecraft:flint', '4x hbm:nugget_silicon')                 solid only
 * e.arcFurnace.remove('#minecraft:sand')                                       by exact input: item or #tag
 * e.arcFurnace.removeByOutput('hbm:nugget_silicon')                            solid output item
 * e.arcFurnace.removeByMaterial('silicon')                                     any molten output of that material
 * e.arcFurnace.removeAll()
 * ids: hbm:arc_furnace/&lt;input&gt;
 * </pre>
 * An add replaces the solid and/or molten half it states for that input. Molten in quanta (ingot 72).
 */
public final class ArcFurnaceHandler extends TableHandler {

    @Override
    protected Object registry() {
        return ArcFurnaceRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        ArcFurnaceRecipes.register();
        return RecipeIds.views(machine(), List.copyOf(ArcFurnaceRecipes.RECIPES), recipe -> RecipeIds.bare(RecipeIds.spec(recipe.input)),
                recipe -> List.of(RecipeIds.spec(recipe.input)),
                recipe -> {
                    List<String> out = new ArrayList<>();
                    if (recipe.solidOutput != null) out.add(RecipeIds.spec(recipe.solidOutput));
                    for (Mats.MaterialStack molten : recipe.fluidOutput) out.add(RecipeIds.spec(molten));
                    return out;
                });
    }

    @Override
    protected void delete(RecipeView view) {
        ArcFurnaceRecipe target = (ArcFurnaceRecipe) view.recipe();
        ArcFurnaceRecipes.removeIf(recipe -> recipe == target);
    }

    @Override
    public void reset() {
        ArcFurnaceRecipes.reset();
    }

    public void add(Spec.Ingredient input, Spec.Item solidOutput, Spec.Molten... molten) {
        edit("add " + input, () -> ArcFurnaceRecipes.put(
                ScriptSpecs.ingredient(input),
                solidOutput == null ? null : ScriptSpecs.stack(solidOutput),
                ScriptSpecs.molten(molten)));
    }

    public void remove(Spec.Ingredient input) {
        edit("remove " + input, () -> {
            if (!ArcFurnaceRecipes.remove(ScriptSpecs.ingredient(input))) {
                throw new IllegalArgumentException("no arc furnace recipe for " + input);
            }
        });
    }

    public void removeByOutput(Spec.Item item) {
        edit("removeByOutput " + item, () -> {
            Item target = ScriptSpecs.item(item);
            ArcFurnaceRecipes.removeIf(recipe -> recipe.solidOutput != null && recipe.solidOutput.is(target));
        });
    }

    public void removeByMaterial(String material) {
        edit("removeByMaterial " + material, () -> {
            var target = ScriptSpecs.material(material);
            ArcFurnaceRecipes.removeIf(recipe -> {
                for (Mats.MaterialStack out : recipe.fluidOutput) if (out.material == target) return true;
                return false;
            });
        });
    }

    public void removeAll() {
        edit("removeAll", () -> ArcFurnaceRecipes.removeIf(recipe -> true));
    }
}
