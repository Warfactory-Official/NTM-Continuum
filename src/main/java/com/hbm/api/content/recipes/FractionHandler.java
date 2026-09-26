package com.hbm.api.content.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.recipes.FractionRecipes;
import com.hbm.inventory.recipes.FractionRecipes.FractionPair;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.api.content.Spec;

import java.util.List;
import java.util.Map;

/**
 * <pre>
 * e.fractionTower.add('heavyoil', '30x bitumen', '70x smear')   outputs per 100 mB input
 * e.fractionTower.remove('egg')
 * e.fractionTower.removeById('hbm:fraction_tower/heavyoil')     id = input fluid
 * </pre>
 */
public final class FractionHandler extends TableHandler {

    @Override
    protected Object registry() {
        return FractionRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        return RecipeIds.views(machine(), List.copyOf(FractionRecipes.getAll().entrySet()), e -> RecipeIds.spec(e.getKey()),
                e -> List.of(RecipeIds.spec(new FluidStack(e.getKey(), 100))),
                e -> List.of(RecipeIds.spec(e.getValue().left()), RecipeIds.spec(e.getValue().right())));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void delete(RecipeView view) {
        FractionRecipes.remove(((Map.Entry<FluidType, FractionPair>) view.recipe()).getKey());
    }

    @Override
    public void reset() {
        FractionRecipes.reset();
    }

    public void add(Spec.Fluid input, Spec.Fluid left, Spec.Fluid right) {
        edit("add " + input, () -> FractionRecipes.put(ScriptSpecs.fluidType(input),
                new FractionPair(ScriptSpecs.fluid(left), ScriptSpecs.fluid(right))));
    }

    public void remove(Spec.Fluid input) {
        edit("remove " + input, () -> FractionRecipes.remove(ScriptSpecs.fluidType(input)));
    }
}
