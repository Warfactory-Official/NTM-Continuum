package com.hbm.api.content.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.recipes.CompressorRecipes;
import com.hbm.inventory.recipes.CompressorRecipes.CompressorRecipe;
import com.hbm.inventory.recipes.CompressorRecipes.Key;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.api.content.Spec;

import java.util.List;
import java.util.Map;

/**
 * <pre>
 * e.compressor.add('2000x petroleum@1', '1000x lpg', 20)   input @pressure = the pressure compressed from; duration in ticks
 * e.compressor.remove('petroleum@1')                       unlisted fluid@p compresses to itself at p+1
 * e.compressor.removeById('hbm:compressor/petroleum_1')    id = input fluid _ pressure
 * </pre>
 */
public final class CompressorHandler extends TableHandler {

    @Override
    protected Object registry() {
        return CompressorRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        return RecipeIds.views(machine(), List.copyOf(CompressorRecipes.getAll().entrySet()),
                e -> RecipeIds.spec(e.getKey().type()) + "@" + e.getKey().pressure(),
                e -> List.of(RecipeIds.spec(new FluidStack(e.getKey().type(), e.getValue().inputAmount(), e.getKey().pressure()))),
                e -> List.of(RecipeIds.spec(e.getValue().output())));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void delete(RecipeView view) {
        Key key = ((Map.Entry<Key, CompressorRecipe>) view.recipe()).getKey();
        CompressorRecipes.remove(key.type(), key.pressure());
    }

    @Override
    public void reset() {
        CompressorRecipes.reset();
    }

    public void add(Spec.Fluid input, Spec.Fluid output, int duration) {
        edit("add " + input, () -> {
            FluidStack in = ScriptSpecs.fluid(input);
            CompressorRecipes.put(in.type, in.pressure, new CompressorRecipe(in.fill, ScriptSpecs.fluid(output), duration));
        });
    }

    public void remove(Spec.Fluid input) {
        edit("remove " + input, () -> {
            FluidStack in = ScriptSpecs.fluid(input);
            CompressorRecipes.remove(in.type, in.pressure);
        });
    }
}
