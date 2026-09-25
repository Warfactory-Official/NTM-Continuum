package com.hbm.scripting.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.HydrotreatingRecipes;
import com.hbm.inventory.recipes.HydrotreatingRecipes.HydrotreatingRecipe;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.scripting.Spec;

import java.util.List;
import java.util.Map;

/**
 * <pre>
 * e.hydrotreater.add('oil', '5x hydrogen@1', '90x oil_ds', '15x sourgas')   per 100 mB input
 * e.hydrotreater.remove('gas')
 * e.hydrotreater.removeById('hbm:hydrotreater/oil')                        id = input fluid
 * </pre>
 */
public final class HydrotreatingHandler extends TableHandler {

    @Override
    protected Object registry() {
        return HydrotreatingRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        return RecipeIds.views(machine(), List.copyOf(HydrotreatingRecipes.getAll().entrySet()), e -> RecipeIds.spec(e.getKey()),
                e -> List.of(RecipeIds.spec(new FluidStack(e.getKey(), 100)), RecipeIds.spec(e.getValue().hydrogen())),
                e -> List.of(RecipeIds.spec(e.getValue().out1()), RecipeIds.spec(e.getValue().out2())));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void delete(RecipeView view) {
        HydrotreatingRecipes.remove(((Map.Entry<FluidType, HydrotreatingRecipe>) view.recipe()).getKey());
    }

    @Override
    public void reset() {
        HydrotreatingRecipes.reset();
    }

    public void add(Spec.Fluid input, Spec.Fluid hydrogen, Spec.Fluid output1, Spec.Fluid output2) {
        edit("add " + input, () -> HydrotreatingRecipes.put(ScriptSpecs.fluidType(input), new HydrotreatingRecipe(
                ScriptSpecs.fluid(hydrogen), ScriptSpecs.fluid(output1), ScriptSpecs.fluid(output2))));
    }

    public void remove(Spec.Fluid input) {
        edit("remove " + input, () -> HydrotreatingRecipes.remove(ScriptSpecs.fluidType(input)));
    }
}
