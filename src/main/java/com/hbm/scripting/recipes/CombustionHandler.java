package com.hbm.scripting.recipes;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.FluidCombustionRecipes;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.scripting.Spec;

import java.util.List;
import java.util.Map;

/**
 * <pre>
 * e.combustion.add('ethanol', 75)                  TU per mB burned
 * e.combustion.remove('oil')
 * e.combustion.removeById('hbm:combustion/oil')    id = fluid
 * </pre>
 */
public final class CombustionHandler extends TableHandler {

    @Override
    protected Object registry() {
        return FluidCombustionRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        return RecipeIds.views(machine(), List.copyOf(FluidCombustionRecipes.getAll().entrySet()), e -> RecipeIds.spec(e.getKey()),
                e -> List.of(RecipeIds.spec(e.getKey())), e -> List.of());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void delete(RecipeView view) {
        FluidCombustionRecipes.remove(((Map.Entry<FluidType, Integer>) view.recipe()).getKey());
    }

    @Override
    public void reset() {
        FluidCombustionRecipes.reset();
    }

    public void add(Spec.Fluid fluid, int heatPerMilliBucket) {
        edit("add " + fluid, () -> FluidCombustionRecipes.put(ScriptSpecs.fluidType(fluid), heatPerMilliBucket));
    }

    public void remove(Spec.Fluid fluid) {
        edit("remove " + fluid, () -> FluidCombustionRecipes.remove(ScriptSpecs.fluidType(fluid)));
    }
}
