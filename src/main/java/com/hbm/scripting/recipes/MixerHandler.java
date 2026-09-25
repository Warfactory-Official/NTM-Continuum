package com.hbm.scripting.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.MixerRecipes;
import com.hbm.inventory.recipes.MixerRecipes.MixerRecipe;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.scripting.Spec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * <pre>
 * e.mixer.add('coolant', 2000, 50, r => r.fluid1('1800x water').fluid2('200x mercury').solid('#c:dusts/niter'))
 * e.mixer.remove('coolant')        every recipe for that output fluid
 * e.mixer.removeAll()
 * ids: hbm:mixer/&lt;output fluid&gt;, hbm:mixer/&lt;output fluid&gt;_2 ...
 * </pre>
 * An add appends a further selectable recipe to the output fluid; amounts in mB, time in ticks.
 */
public final class MixerHandler extends TableHandler {

    private record Row(FluidType output, MixerRecipe recipe) {}

    @Override
    protected Object registry() {
        return MixerRecipes.INSTANCE;
    }

    @Override
    public List<RecipeView> views() {
        MixerRecipes.INSTANCE.registerRecipes();
        List<Row> rows = new ArrayList<>();
        for (Map.Entry<FluidType, MixerRecipe[]> entry : MixerRecipes.recipes.entrySet()) {
            for (MixerRecipe recipe : entry.getValue()) rows.add(new Row(entry.getKey(), recipe));
        }
        return RecipeIds.views(machine(), rows, row -> RecipeIds.spec(row.output()), row -> {
            List<String> in = new ArrayList<>();
            if (row.recipe().input1 != null) in.add(RecipeIds.spec(row.recipe().input1));
            if (row.recipe().input2 != null) in.add(RecipeIds.spec(row.recipe().input2));
            if (row.recipe().solidInput != null) in.add(RecipeIds.spec(row.recipe().solidInput));
            return in;
        }, row -> List.of(RecipeIds.spec(new FluidStack(row.output(), row.recipe().output))));
    }

    @Override
    protected void delete(RecipeView view) {
        MixerRecipes.removeEntry(((Row) view.recipe()).recipe());
    }

    @Override
    public void reset() {
        MixerRecipes.INSTANCE.reset();
    }

    public void add(Spec.Fluid output, int amount, int processTime, Consumer<MixerBuilder> spec) {
        MixerBuilder builder = new MixerBuilder();
        spec.accept(builder);
        edit("add " + output, () -> MixerRecipes.add(
                ScriptSpecs.fluidType(output), amount, processTime,
                builder.fluid1 == null ? null : ScriptSpecs.fluid(builder.fluid1),
                builder.fluid2 == null ? null : ScriptSpecs.fluid(builder.fluid2),
                builder.solid == null ? null : ScriptSpecs.ingredient(builder.solid)));
    }

    public void remove(Spec.Fluid output) {
        edit("remove " + output, () -> {
            if (!MixerRecipes.removeOutput(ScriptSpecs.fluidType(output))) {
                throw new IllegalArgumentException("no mixer recipe for " + output);
            }
        });
    }

    public void removeAll() {
        edit("removeAll", MixerRecipes::removeAll);
    }

    public static final class MixerBuilder {

        private Spec.Fluid fluid1;
        private Spec.Fluid fluid2;
        private Spec.Ingredient solid;

        public MixerBuilder fluid1(Spec.Fluid spec) {
            this.fluid1 = spec;
            return this;
        }

        public MixerBuilder fluid2(Spec.Fluid spec) {
            this.fluid2 = spec;
            return this;
        }

        public MixerBuilder solid(Spec.Ingredient spec) {
            this.solid = spec;
            return this;
        }
    }
}
