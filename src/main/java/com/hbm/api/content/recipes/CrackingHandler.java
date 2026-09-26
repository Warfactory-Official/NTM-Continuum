package com.hbm.api.content.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.CrackingRecipes;
import com.hbm.inventory.recipes.CrackingRecipes.CrackingRecipe;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.api.content.Spec;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * <pre>
 * e.cracking.add('oil', '80x crackoil', '20x petroleum')   outputs per 100 mB input
 * e.cracking.add('kerosene', '60x petroleum')
 * e.cracking.remove('gas')
 * e.cracking.removeById('hbm:cracking/oil')                id = input fluid
 * </pre>
 * Radiolysis copies this table when it builds.
 */
public final class CrackingHandler extends TableHandler {

    @Override
    protected Object registry() {
        return CrackingRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        return RecipeIds.views(machine(), List.copyOf(CrackingRecipes.getAll().entrySet()), e -> RecipeIds.spec(e.getKey()),
                e -> List.of(RecipeIds.spec(new FluidStack(e.getKey(), 100))),
                e -> Stream.of(e.getValue().left(), e.getValue().right()).filter(f -> f.type != Fluids.NONE)
                        .map(RecipeIds::spec).toList());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void delete(RecipeView view) {
        CrackingRecipes.remove(((Map.Entry<FluidType, CrackingRecipe>) view.recipe()).getKey());
    }

    @Override
    public void reset() {
        CrackingRecipes.reset();
    }

    public void add(Spec.Fluid input, Spec.Fluid left) {
        add(input, left, null);
    }

    public void add(Spec.Fluid input, Spec.Fluid left, Spec.Fluid right) {
        edit("add " + input, () ->
                CrackingRecipes.put(ScriptSpecs.fluidType(input), new CrackingRecipe(
                        ScriptSpecs.fluid(left), right == null ? new FluidStack(Fluids.NONE, 0) : ScriptSpecs.fluid(right))));
    }

    public void remove(Spec.Fluid input) {
        edit("remove " + input, () -> CrackingRecipes.remove(ScriptSpecs.fluidType(input)));
    }
}
