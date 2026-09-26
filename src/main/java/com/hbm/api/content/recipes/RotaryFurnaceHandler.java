package com.hbm.api.content.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.RotaryFurnaceRecipes;
import com.hbm.inventory.recipes.RotaryFurnaceRecipes.RotaryFurnaceRecipe;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.api.content.Spec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * e.rotaryFurnace.add('72x steel', 100, 100, ['#c:ingots/iron', '#c:gems/coal'])
 * e.rotaryFurnace.add('144x saturnite', 200, 400, '250x reformgas', ['4x #c:dusts/dura_steel', '#c:dusts/copper'])
 * e.rotaryFurnace.removeByInput('hbm:powder_flux')
 * e.rotaryFurnace.removeByOutput('steel')
 * e.rotaryFurnace.removeAll()
 * </pre>
 * Output in quanta (ingot 72); steam in mB per tick; at most three item inputs.
 * Ids: {@code hbm:rotary_furnace/<first item input, else fluid>}, e.g. {@code hbm:rotary_furnace/tag.c.ingots/iron}.
 */
public final class RotaryFurnaceHandler extends TableHandler {

    @Override
    protected Object registry() {
        return RotaryFurnaceRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        RotaryFurnaceRecipes.register();
        return RecipeIds.views(machine(), List.copyOf(RotaryFurnaceRecipes.recipes),
                recipe -> RecipeIds.bare(RecipeIds.spec(recipe.ingredients.length > 0 ? recipe.ingredients[0] : recipe.fluid)),
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
        RotaryFurnaceRecipe target = (RotaryFurnaceRecipe) view.recipe();
        RotaryFurnaceRecipes.removeIf(recipe -> recipe == target);
    }

    @Override
    public void reset() {
        RotaryFurnaceRecipes.reset();
    }

    public void add(Spec.Molten output, int duration, int steam, Spec.Ingredient[] inputs) {
        add(output, duration, steam, null, inputs);
    }

    public void add(Spec.Molten output, int duration, int steam, Spec.Fluid fluid, Spec.Ingredient[] inputs) {
        if (inputs.length > 3) throw new IllegalArgumentException("rotary furnace takes at most 3 item inputs");
        edit("add " + output, () ->
                RotaryFurnaceRecipes.addRecipe(new RotaryFurnaceRecipe(ScriptSpecs.molten(output), duration, steam,
                        fluid == null ? null : ScriptSpecs.fluid(fluid), ScriptSpecs.ingredients(inputs))));
    }

    public void removeByInput(Spec.Ingredient input) {
        edit("removeByInput " + input, () -> {
            List<Item> items = ScriptSpecs.items(input);
            RotaryFurnaceRecipes.removeIf(recipe -> {
                for (AStack ingredient : recipe.ingredients) {
                    for (Item item : items) if (ingredient.matchesRecipe(new ItemStack(item), true)) return true;
                }
                return false;
            });
        });
    }

    public void removeByOutput(Spec.Molten material) {
        edit("removeByOutput " + material, () -> {
            var target = ScriptSpecs.molten(material).material;
            RotaryFurnaceRecipes.removeIf(recipe -> recipe.output.material == target);
        });
    }

    public void removeAll() {
        edit("removeAll", () -> RotaryFurnaceRecipes.removeIf(recipe -> true));
    }
}
