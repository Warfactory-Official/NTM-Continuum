package com.hbm.api.content.recipes;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.ElectrolyserMetalRecipes;
import com.hbm.inventory.recipes.ElectrolyserMetalRecipes.ElectrolysisMetalRecipe;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.api.content.Spec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * e.electrolyserMetal.add('hbm:crystal_iron', '432x iron', '144x titanium', '3x hbm:powder_lithium_tiny')
 * e.electrolyserMetal.add('#c:gems/amethyst', '72x iron', null)          either molten output nullable
 * e.electrolyserMetal.remove('hbm:crystal_iron')
 * e.electrolyserMetal.removeAll()
 * ids: hbm:electrolyser_metal/&lt;input item&gt;
 * </pre>
 * Input item or #tag; replaces any entry. Molten in quanta (ingot 72); at most six byproducts.
 */
public final class ElectrolyserMetalHandler extends TableHandler {

    @Override
    protected Object registry() {
        return ElectrolyserMetalRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        ElectrolyserMetalRecipes.register();
        return RecipeIds.views(machine(), List.copyOf(ElectrolyserMetalRecipes.RECIPES),
                recipe -> RecipeIds.bare(RecipeIds.spec(recipe.input)),
                recipe -> List.of(RecipeIds.spec(recipe.input)),
                recipe -> {
                    List<String> out = new ArrayList<>();
                    if (recipe.output1 != null) out.add(RecipeIds.spec(recipe.output1));
                    if (recipe.output2 != null) out.add(RecipeIds.spec(recipe.output2));
                    if (recipe.byproduct != null) for (ItemStack extra : recipe.byproduct) out.add(RecipeIds.spec(extra));
                    return out;
                });
    }

    @Override
    protected void delete(RecipeView view) {
        ElectrolysisMetalRecipe recipe = (ElectrolysisMetalRecipe) view.recipe();
        ElectrolyserMetalRecipes.remove(((ComparableStack) recipe.input).item);
    }

    @Override
    public void reset() {
        ElectrolyserMetalRecipes.reset();
    }

    public void add(Spec.Ingredient input, Spec.Molten molten1, Spec.Molten molten2, Spec.Item... byproducts) {
        edit("add " + input, () -> {
            if (byproducts.length > 6) throw new IllegalArgumentException("at most six byproducts");
            var out1 = molten1 == null ? null : ScriptSpecs.molten(molten1);
            var out2 = molten2 == null ? null : ScriptSpecs.molten(molten2);
            ItemStack[] extra = ScriptSpecs.stacks(byproducts);
            for (Item item : ScriptSpecs.items(input)) {
                ElectrolyserMetalRecipes.put(item, out1, out2, extra);
            }
        });
    }

    public void remove(Spec.Ingredient input) {
        edit("remove " + input, () -> {
            for (Item item : ScriptSpecs.items(input)) ElectrolyserMetalRecipes.remove(item);
        });
    }

    public void removeAll() {
        edit("removeAll", ElectrolyserMetalRecipes::removeAll);
    }
}
