package com.hbm.api.content.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.recipes.CoolingRecipes;
import com.hbm.inventory.recipes.CoolingRecipes.CoolingRecipe;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.api.content.Spec;

import java.util.List;

/**
 * <pre>
 * e.cooling.add('10x hotsteam', '100x steam', 2)   heat = TU released per operation
 * e.cooling.remove('steam')
 * e.cooling.removeById('hbm:cooling/hotsteam')     id = input fluid
 * </pre>
 * Turbines only; the fluid's coolable trait is untouched.
 */
public final class CoolingHandler extends TableHandler {

    @Override
    protected Object registry() {
        return CoolingRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        return RecipeIds.views(machine(), List.copyOf(CoolingRecipes.getAll().values()), r -> RecipeIds.spec(r.input()),
                r -> List.of(RecipeIds.spec(new FluidStack(r.input(), r.inputAmount()))),
                r -> List.of(RecipeIds.spec(new FluidStack(r.output(), r.outputAmount()))));
    }

    @Override
    protected void delete(RecipeView view) {
        CoolingRecipes.remove(((CoolingRecipe) view.recipe()).input());
    }

    @Override
    public void reset() {
        CoolingRecipes.reset();
    }

    public void add(Spec.Fluid input, Spec.Fluid output, int heat) {
        edit("add " + input, () -> {
            FluidStack in = ScriptSpecs.fluid(input);
            FluidStack out = ScriptSpecs.fluid(output);
            CoolingRecipes.put(new CoolingRecipe(in.type, in.fill, out.type, out.fill, heat));
        });
    }

    public void remove(Spec.Fluid input) {
        edit("remove " + input, () -> CoolingRecipes.remove(ScriptSpecs.fluidType(input)));
    }
}
