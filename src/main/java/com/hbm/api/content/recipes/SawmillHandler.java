package com.hbm.api.content.recipes;

import com.hbm.inventory.recipes.SawmillRecipes;
import com.hbm.inventory.recipes.SawmillRecipes.SawmillRecipe;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.api.content.Spec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * e.sawmill.add('minecraft:bamboo_block', '4x minecraft:bamboo_planks')
 * e.sawmill.add('#minecraft:logs', '6x minecraft:oak_planks', 'hbm:powder_sawdust', 50)     chance in percent
 * e.sawmill.removeByInput('#minecraft:saplings')
 * e.sawmill.removeByOutput('minecraft:stick')
 * e.sawmill.removeAll()
 * </pre>
 * Scripted entries win over the built-in wood classes. Built-in log output follows the crafting table,
 * so removeByOutput on a built-in class matches only its JEI output.
 * Ids: {@code hbm:sawmill/<input>}, scripted rows first, e.g. {@code hbm:sawmill/tag.minecraft.logs}.
 */
public final class SawmillHandler extends TableHandler {

    @Override
    protected Object registry() {
        return SawmillRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        return RecipeIds.views(machine(), SawmillRecipes.getRecipes(),
                recipe -> RecipeIds.bare(RecipeIds.spec(recipe.input())),
                recipe -> List.of(RecipeIds.spec(recipe.input())),
                recipe -> {
                    List<String> out = new ArrayList<>();
                    out.add(RecipeIds.spec(recipe.output()));
                    if (!recipe.secondary().isEmpty()) out.add(RecipeIds.spec(recipe.secondary()));
                    return out;
                });
    }

    @Override
    protected void delete(RecipeView view) {
        SawmillRecipe target = (SawmillRecipe) view.recipe();
        SawmillRecipes.removeIf(recipe -> recipe == target);
    }

    @Override
    public void reset() {
        SawmillRecipes.reset();
    }

    public void add(Spec.Ingredient input, Spec.Item output) {
        add(input, output, null, 0);
    }

    public void add(Spec.Ingredient input, Spec.Item output, Spec.Item secondary, int chance) {
        edit("add " + input, () ->
                SawmillRecipes.addRecipe(new SawmillRecipe(ScriptSpecs.ingredient(input), ScriptSpecs.stack(output),
                        secondary == null ? ItemStack.EMPTY : ScriptSpecs.stack(secondary), chance)));
    }

    public void removeByInput(Spec.Ingredient input) {
        edit("removeByInput " + input, () -> SawmillRecipes.removeInput(ScriptSpecs.ingredient(input)));
    }

    public void removeByOutput(Spec.Ingredient output) {
        edit("removeByOutput " + output, () -> {
            List<Item> items = ScriptSpecs.items(output);
            SawmillRecipes.removeIf(recipe -> items.contains(recipe.output().getItem()));
        });
    }

    public void removeAll() {
        edit("removeAll", () -> SawmillRecipes.removeIf(recipe -> true));
    }
}
