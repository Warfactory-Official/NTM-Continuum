package com.hbm.api.content.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.RadiolysisRecipes;
import com.hbm.inventory.recipes.RadiolysisRecipes.RadiolysisRecipe;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.api.content.Spec;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * <pre>
 * e.radiolysis.add('water', '80x peroxide', '20x hydrogen')   outputs per 100 mB input
 * e.radiolysis.add('kerosene', '60x petroleum')
 * e.radiolysis.remove('oil')
 * e.radiolysis.removeById('hbm:radiolysis/water')             id = input fluid
 * </pre>
 * Cracking edits reach radiolysis too; these apply after them.
 */
public final class RadiolysisHandler extends TableHandler {

    @Override
    protected Object registry() {
        return RadiolysisRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        return RecipeIds.views(machine(), List.copyOf(RadiolysisRecipes.getAll().entrySet()), e -> RecipeIds.spec(e.getKey()),
                e -> List.of(RecipeIds.spec(new FluidStack(e.getKey(), 100))),
                e -> Stream.of(e.getValue().left(), e.getValue().right()).filter(f -> f.type != Fluids.NONE)
                        .map(RecipeIds::spec).toList());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void delete(RecipeView view) {
        RadiolysisRecipes.remove(((Map.Entry<FluidType, RadiolysisRecipe>) view.recipe()).getKey());
    }

    @Override
    public void reset() {
        RadiolysisRecipes.reset();
    }

    public void add(Spec.Fluid input, Spec.Fluid left) {
        add(input, left, null);
    }

    public void add(Spec.Fluid input, Spec.Fluid left, Spec.Fluid right) {
        edit("add " + input, () ->
                RadiolysisRecipes.put(ScriptSpecs.fluidType(input), new RadiolysisRecipe(
                        ScriptSpecs.fluid(left), right == null ? new FluidStack(Fluids.NONE, 0) : ScriptSpecs.fluid(right))));
    }

    public void remove(Spec.Fluid input) {
        edit("remove " + input, () -> RadiolysisRecipes.remove(ScriptSpecs.fluidType(input)));
    }
}
