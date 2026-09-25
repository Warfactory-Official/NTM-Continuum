package com.hbm.scripting.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.ElectrolyserFluidRecipes;
import com.hbm.inventory.recipes.ElectrolyserFluidRecipes.ElectrolysisRecipe;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.scripting.Spec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * <pre>
 * e.electrolyserFluid.add('2000x water', r => r.outputs('200x hydrogen', '200x oxygen').duration(10))
 * e.electrolyserFluid.add('1000x slop', r => r.outputs('250x mercury').byproducts('2x hbm:niter', 'hbm:sulfur'))
 * e.electrolyserFluid.remove('vitriol')
 * e.electrolyserFluid.removeById('hbm:electrolyser_fluid/water')   id = input fluid
 * </pre>
 * Input count = mB per operation; one or two fluid outputs; duration defaults to 20 ticks.
 */
public final class ElectrolyserFluidHandler extends TableHandler {

    @Override
    protected Object registry() {
        return ElectrolyserFluidRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        ElectrolyserFluidRecipes.register();
        return RecipeIds.views(machine(), List.copyOf(ElectrolyserFluidRecipes.RECIPES.values()), r -> RecipeIds.spec(r.input),
                r -> List.of(RecipeIds.spec(new FluidStack(r.input, r.amount))),
                r -> {
                    List<String> out = new ArrayList<>();
                    if (r.output1.type != Fluids.NONE) out.add(RecipeIds.spec(r.output1));
                    if (r.output2.type != Fluids.NONE) out.add(RecipeIds.spec(r.output2));
                    for (ItemStack stack : r.byproduct) out.add(RecipeIds.spec(stack));
                    return out;
                });
    }

    @Override
    protected void delete(RecipeView view) {
        ElectrolyserFluidRecipes.remove(((ElectrolysisRecipe) view.recipe()).input);
    }

    @Override
    public void reset() {
        ElectrolyserFluidRecipes.reset();
    }

    public void add(Spec.Fluid input, Consumer<Builder> spec) {
        Builder builder = new Builder();
        spec.accept(builder);
        edit("add " + input, () -> {
            FluidStack in = ScriptSpecs.fluid(input);
            if (builder.outputs == null || builder.outputs.length < 1 || builder.outputs.length > 2) {
                throw new IllegalArgumentException("electrolysis needs one or two fluid outputs");
            }
            FluidStack[] out = ScriptSpecs.fluids(builder.outputs);
            ElectrolyserFluidRecipes.put(in.type, in.fill, out[0],
                    out.length > 1 ? out[1] : new FluidStack(Fluids.NONE, 0), builder.duration,
                    ScriptSpecs.stacks(builder.byproducts));
        });
    }

    public void remove(Spec.Fluid input) {
        edit("remove " + input, () -> ElectrolyserFluidRecipes.remove(ScriptSpecs.fluidType(input)));
    }

    public static final class Builder {

        private Spec.Fluid[] outputs;
        private Spec.Item[] byproducts = new Spec.Item[0];
        private int duration = 20;

        public Builder outputs(Spec.Fluid... specs) {
            this.outputs = specs;
            return this;
        }

        public Builder byproducts(Spec.Item... specs) {
            this.byproducts = specs;
            return this;
        }

        public Builder duration(int ticks) {
            this.duration = ticks;
            return this;
        }
    }
}
