package com.hbm.api.content.recipes;

import com.hbm.inventory.recipes.CentrifugeRecipes;
import com.hbm.inventory.recipes.CentrifugeRecipes.CentrifugeRecipe;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.api.content.Spec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;

/**
 * <pre>
 * e.centrifuge.add('#c:ores/iron', 'hbm:powder_iron', 'hbm:powder_iron', 'hbm:powder_iron', 'minecraft:gravel')
 * e.centrifuge.removeByInput('minecraft:iron_ore')
 * e.centrifuge.removeByOutput('hbm:powder_iron')
 * e.centrifuge.removeAll()
 * ids: hbm:centrifuge/&lt;input&gt;
 * </pre>
 * Up to four outputs. First matching recipe wins; an add takes precedence over every earlier recipe.
 */
public final class CentrifugeHandler extends TableHandler {

    @Override
    protected Object registry() {
        return CentrifugeRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        CentrifugeRecipes.register();
        return RecipeIds.views(machine(), List.copyOf(CentrifugeRecipes.recipes), recipe -> RecipeIds.bare(RecipeIds.spec(recipe.input)),
                recipe -> List.of(RecipeIds.spec(recipe.input)),
                recipe -> Arrays.stream(recipe.output).map(RecipeIds::spec).toList());
    }

    @Override
    protected void delete(RecipeView view) {
        CentrifugeRecipe target = (CentrifugeRecipe) view.recipe();
        CentrifugeRecipes.removeIf(recipe -> recipe == target);
    }

    @Override
    public void reset() {
        CentrifugeRecipes.reset();
    }

    public void add(Spec.Ingredient input, Spec.Item... outputs) {
        edit("add " + input, () -> {
            if (outputs.length == 0 || outputs.length > 4) throw new IllegalArgumentException("centrifuge takes one to four outputs");
            CentrifugeRecipes.putFirst(ScriptSpecs.ingredient(input), ScriptSpecs.stacks(outputs));
        });
    }

    public void removeByInput(Spec.Item item) {
        edit("removeByInput " + item, () -> {
            ItemStack probe = new ItemStack(ScriptSpecs.item(item));
            CentrifugeRecipes.removeIf(recipe -> recipe.input.matchesRecipe(probe, true));
        });
    }

    public void removeByOutput(Spec.Item item) {
        edit("removeByOutput " + item, () -> {
            Item target = ScriptSpecs.item(item);
            CentrifugeRecipes.removeIf(recipe -> {
                for (ItemStack out : recipe.output) if (out.is(target)) return true;
                return false;
            });
        });
    }

    public void removeAll() {
        edit("removeAll", () -> CentrifugeRecipes.removeIf(recipe -> true));
    }
}
