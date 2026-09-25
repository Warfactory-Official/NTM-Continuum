package com.hbm.scripting.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.ReformingRecipes;
import com.hbm.inventory.recipes.ReformingRecipes.ReformingRecipe;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.scripting.Spec;

import java.util.List;
import java.util.Map;

/**
 * <pre>
 * e.reformer.add('naphtha', '50x reformate', '15x petroleum', '10x hydrogen')   per 100 mB input
 * e.reformer.remove('cholesterol')
 * e.reformer.removeById('hbm:reformer/naphtha')                               id = input fluid
 * </pre>
 */
public final class ReformingHandler extends TableHandler {

    @Override
    protected Object registry() {
        return ReformingRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        return RecipeIds.views(machine(), List.copyOf(ReformingRecipes.getAll().entrySet()), e -> RecipeIds.spec(e.getKey()),
                e -> List.of(RecipeIds.spec(new FluidStack(e.getKey(), 100))),
                e -> List.of(RecipeIds.spec(e.getValue().out1()), RecipeIds.spec(e.getValue().out2()),
                        RecipeIds.spec(e.getValue().out3())));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void delete(RecipeView view) {
        ReformingRecipes.remove(((Map.Entry<FluidType, ReformingRecipe>) view.recipe()).getKey());
    }

    @Override
    public void reset() {
        ReformingRecipes.reset();
    }

    public void add(Spec.Fluid input, Spec.Fluid output1, Spec.Fluid output2, Spec.Fluid output3) {
        edit("add " + input, () -> ReformingRecipes.put(ScriptSpecs.fluidType(input), new ReformingRecipe(
                ScriptSpecs.fluid(output1), ScriptSpecs.fluid(output2), ScriptSpecs.fluid(output3))));
    }

    public void remove(Spec.Fluid input) {
        edit("remove " + input, () -> ReformingRecipes.remove(ScriptSpecs.fluidType(input)));
    }
}
