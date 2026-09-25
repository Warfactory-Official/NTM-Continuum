package com.hbm.scripting.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.HeatRecipes;
import com.hbm.inventory.recipes.HeatRecipes.HeatStep;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.scripting.Spec;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * <pre>
 * e.heating.boiler('1x water', '100x steam', 200)          heat = TU per operation
 * e.heating.reactor('1x coolant', '1x coolant_hot', 300)   Watz coolant
 * e.heating.removeBoiler('oil')
 * e.heating.removeReactor('coolant')
 * e.heating.removeById('hbm:heating/boiler/water')         id = boiler|reactor / input fluid
 * </pre>
 * Independent of fluid heatable traits.
 */
public final class HeatHandler extends TableHandler {

    private record Row(boolean reactor, FluidType input, HeatStep step) {}

    @Override
    protected Object registry() {
        return HeatRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        List<Row> rows = Stream.concat(rows(false, HeatRecipes.getAll()), rows(true, HeatRecipes.getAllReactor())).toList();
        return RecipeIds.views(machine(), rows, r -> (r.reactor() ? "reactor/" : "boiler/") + RecipeIds.spec(r.input()),
                r -> List.of(RecipeIds.spec(new FluidStack(r.input(), r.step().amountReq()))),
                r -> List.of(RecipeIds.spec(new FluidStack(r.step().output(), r.step().amountProduced()))));
    }

    private static Stream<Row> rows(boolean reactor, Map<FluidType, HeatStep> table) {
        return table.entrySet().stream().map(e -> new Row(reactor, e.getKey(), e.getValue()));
    }

    @Override
    protected void delete(RecipeView view) {
        Row row = (Row) view.recipe();
        if (row.reactor()) HeatRecipes.removeReactor(row.input());
        else HeatRecipes.removeBoiler(row.input());
    }

    @Override
    public void reset() {
        HeatRecipes.reset();
    }

    public void boiler(Spec.Fluid input, Spec.Fluid output, int heat) {
        edit("boiler " + input, () -> {
            FluidStack in = ScriptSpecs.fluid(input);
            HeatRecipes.putBoiler(in.type, step(in, output, heat));
        });
    }

    public void reactor(Spec.Fluid input, Spec.Fluid output, int heat) {
        edit("reactor " + input, () -> {
            FluidStack in = ScriptSpecs.fluid(input);
            HeatRecipes.putReactor(in.type, step(in, output, heat));
        });
    }

    public void removeBoiler(Spec.Fluid input) {
        edit("removeBoiler " + input, () -> HeatRecipes.removeBoiler(ScriptSpecs.fluidType(input)));
    }

    public void removeReactor(Spec.Fluid input) {
        edit("removeReactor " + input, () -> HeatRecipes.removeReactor(ScriptSpecs.fluidType(input)));
    }

    private static HeatStep step(FluidStack in, Spec.Fluid output, int heat) {
        FluidStack out = ScriptSpecs.fluid(output);
        return new HeatStep(heat, in.fill, out.type, out.fill);
    }
}
