package com.hbm.scripting.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.CokerRecipes;
import com.hbm.inventory.recipes.CokerRecipes.CokerRecipe;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.scripting.Spec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 * e.coker.add('4000x watz', '4x hbm:ingot_mud')                  input count = mB per operation
 * e.coker.add('1000x sourgas', 'hbm:sulfur', '150x gas_coker')   third argument: byproduct fluid
 * e.coker.remove('heavyoil')
 * e.coker.removeById('hbm:coker/heavyoil')                       id = input fluid
 * </pre>
 */
public final class CokerHandler extends TableHandler {

    @Override
    protected Object registry() {
        return CokerRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        return RecipeIds.views(machine(), List.copyOf(CokerRecipes.getAll().entrySet()), e -> RecipeIds.spec(e.getKey()),
                e -> List.of(RecipeIds.spec(new FluidStack(e.getKey(), e.getValue().amount()))),
                e -> {
                    List<String> out = new ArrayList<>();
                    out.add(RecipeIds.spec(e.getValue().output()));
                    if (e.getValue().byproduct() != null) out.add(RecipeIds.spec(e.getValue().byproduct()));
                    return out;
                });
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void delete(RecipeView view) {
        CokerRecipes.remove(((Map.Entry<FluidType, CokerRecipe>) view.recipe()).getKey());
    }

    @Override
    public void reset() {
        CokerRecipes.reset();
    }

    public void add(Spec.Fluid input, Spec.Item output) {
        edit("add " + input, () -> put(input, output, null));
    }

    public void add(Spec.Fluid input, Spec.Item output, Spec.Fluid byproduct) {
        edit("add " + input, () -> put(input, output, byproduct));
    }

    public void remove(Spec.Fluid input) {
        edit("remove " + input, () -> CokerRecipes.remove(ScriptSpecs.fluidType(input)));
    }

    private static void put(Spec.Fluid input, Spec.Item output, Spec.Fluid byproduct) {
        FluidStack in = ScriptSpecs.fluid(input);
        CokerRecipes.put(in.type, new CokerRecipe(in.fill, ScriptSpecs.stack(output),
                byproduct == null ? null : ScriptSpecs.fluid(byproduct)));
    }
}
