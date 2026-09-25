package com.hbm.scripting.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.SolidificationRecipes;
import com.hbm.inventory.recipes.SolidificationRecipes.SolidRecipe;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.scripting.Spec;

import java.util.List;
import java.util.Map;

/**
 * <pre>
 * e.solidifier.add('1000x water', 'minecraft:ice')   input count = mB per item
 * e.solidifier.remove('lava')
 * e.solidifier.removeById('hbm:solidifier/water')    id = input fluid
 * </pre>
 */
public final class SolidificationHandler extends TableHandler {

    @Override
    protected Object registry() {
        return SolidificationRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        return RecipeIds.views(machine(), List.copyOf(SolidificationRecipes.getAll().entrySet()), e -> RecipeIds.spec(e.getKey()),
                e -> List.of(RecipeIds.spec(new FluidStack(e.getKey(), e.getValue().amount()))),
                e -> List.of(RecipeIds.spec(e.getValue().output())));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void delete(RecipeView view) {
        SolidificationRecipes.remove(((Map.Entry<FluidType, SolidRecipe>) view.recipe()).getKey());
    }

    @Override
    public void reset() {
        SolidificationRecipes.reset();
    }

    public void add(Spec.Fluid input, Spec.Item output) {
        edit("add " + input, () -> {
            FluidStack in = ScriptSpecs.fluid(input);
            SolidificationRecipes.put(in.type, new SolidRecipe(in.fill, ScriptSpecs.stack(output)));
        });
    }

    public void remove(Spec.Fluid input) {
        edit("remove " + input, () -> SolidificationRecipes.remove(ScriptSpecs.fluidType(input)));
    }
}
