package com.hbm.scripting.recipes;

import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.inventory.recipes.PressRecipes.PressRecipe;
import com.hbm.items.machine.ItemStamp.StampType;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.scripting.Spec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

/**
 * <pre>
 * e.press.add('plate', '#c:ingots/iron', 'hbm:plate_iron')      stamp: flat, plate, wire, circuit, c357, ...
 * e.press.removeByInput('#c:ingots/iron')
 * e.press.removeByOutput('hbm:plate_iron')
 * e.press.removeAll()
 * </pre>
 * Ids: {@code hbm:press/<stamp>/<input>}, e.g. {@code hbm:press/plate/tag.c.ingots/iron}.
 */
public final class PressHandler extends TableHandler {

    @Override
    protected Object registry() {
        return PressRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        return RecipeIds.views(machine(), List.copyOf(PressRecipes.getRecipes()),
                recipe -> recipe.type().name().toLowerCase(Locale.US) + "/" + RecipeIds.bare(RecipeIds.spec(recipe.input())),
                recipe -> List.of(RecipeIds.spec(recipe.input())),
                recipe -> List.of(RecipeIds.spec(recipe.output())));
    }

    @Override
    protected void delete(RecipeView view) {
        PressRecipe target = (PressRecipe) view.recipe();
        PressRecipes.removeIf(recipe -> recipe == target);
    }

    @Override
    public void reset() {
        PressRecipes.reset();
    }

    public void add(String stamp, Spec.Ingredient input, Spec.Item output) {
        edit("add " + output, () ->
                PressRecipes.addRecipe(ScriptSpecs.constant(StampType.class, stamp),
                        ScriptSpecs.ingredient(input), ScriptSpecs.stack(output)));
    }

    public void removeByInput(Spec.Ingredient input) {
        edit("removeByInput " + input, () -> {
            List<Item> items = ScriptSpecs.items(input);
            PressRecipes.removeIf(recipe -> {
                for (Item item : items) if (recipe.input().matchesRecipe(new ItemStack(item), true)) return true;
                return false;
            });
        });
    }

    public void removeByOutput(Spec.Ingredient output) {
        edit("removeByOutput " + output, () -> {
            List<Item> items = ScriptSpecs.items(output);
            PressRecipes.removeIf(recipe -> {
                for (Item item : items) if (recipe.output().is(item)) return true;
                return false;
            });
        });
    }

    public void removeAll() {
        edit("removeAll", () -> PressRecipes.removeIf(recipe -> true));
    }
}
