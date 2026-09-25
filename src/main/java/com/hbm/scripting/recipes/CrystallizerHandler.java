package com.hbm.scripting.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.CrystallizerRecipes.CrystallizerRecipe;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.scripting.Spec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * <pre>
 * e.crystallizer.add('#c:ores/iron', 'hbm:crystal_iron', '500x peroxide', 600)
 * e.crystallizer.add('4x #c:dusts/copper', '4x hbm:chemical_dye_green', '100x woodoil', 400, 0.15)   productivity
 * e.crystallizer.remove('#c:ores/iron', 'peroxide')        by exact input (item or #tag) and acid
 * e.crystallizer.removeByInput('minecraft:iron_ore')
 * e.crystallizer.removeByOutput('hbm:crystal_iron')
 * e.crystallizer.removeAll()
 * ids: hbm:crystallizer/&lt;acid&gt;/&lt;input&gt;
 * </pre>
 * An add replaces the recipe with the same input and acid. Input count = required stack; duration in ticks.
 */
public final class CrystallizerHandler extends TableHandler {

    @Override
    protected Object registry() {
        return CrystallizerRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        CrystallizerRecipes.register();
        return RecipeIds.views(machine(), List.copyOf(CrystallizerRecipes.recipes),
                recipe -> RecipeIds.spec(recipe.fluid) + "/" + RecipeIds.bare(RecipeIds.spec(recipe.input)),
                recipe -> List.of(RecipeIds.spec(recipe.input), RecipeIds.spec(new FluidStack(recipe.fluid, recipe.acidAmount))),
                recipe -> List.of(RecipeIds.spec(recipe.output)));
    }

    @Override
    protected void delete(RecipeView view) {
        CrystallizerRecipe target = (CrystallizerRecipe) view.recipe();
        CrystallizerRecipes.removeIf(recipe -> recipe == target);
    }

    @Override
    public void reset() {
        CrystallizerRecipes.reset();
    }

    public void add(Spec.Ingredient input, Spec.Item output, Spec.Fluid acid, int duration) {
        add(input, output, acid, duration, 0.05F);
    }

    public void add(Spec.Ingredient input, Spec.Item output, Spec.Fluid acid, int duration, float productivity) {
        edit("add " + input, () -> {
            FluidStack fluid = ScriptSpecs.fluid(acid);
            CrystallizerRecipes.put(ScriptSpecs.ingredient(input), ScriptSpecs.stack(output), duration, productivity,
                    fluid.type, fluid.fill);
        });
    }

    public void remove(Spec.Ingredient input, Spec.Fluid acid) {
        edit("remove " + input, () -> {
            if (!CrystallizerRecipes.remove(ScriptSpecs.ingredient(input), ScriptSpecs.fluidType(acid))) {
                throw new IllegalArgumentException("no crystallizer recipe for " + input + " in " + acid);
            }
        });
    }

    public void removeByInput(Spec.Item item) {
        edit("removeByInput " + item, () -> {
            ItemStack probe = new ItemStack(ScriptSpecs.item(item));
            CrystallizerRecipes.removeIf(recipe -> recipe.input.matchesRecipe(probe, true));
        });
    }

    public void removeByOutput(Spec.Item item) {
        edit("removeByOutput " + item, () -> {
            Item target = ScriptSpecs.item(item);
            CrystallizerRecipes.removeIf(recipe -> recipe.output.is(target));
        });
    }

    public void removeAll() {
        edit("removeAll", () -> CrystallizerRecipes.removeIf(recipe -> true));
    }
}
