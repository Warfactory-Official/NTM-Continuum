package com.hbm.scripting.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.RefineryRecipes;
import com.hbm.inventory.recipes.RefineryRecipes.RefineryRecipe;
import com.hbm.inventory.recipes.RefineryRecipes.VacuumRecipe;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.scripting.Spec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 * e.refinery.refinery('hotoil', ['50x heavyoil', '25x naphtha', '15x lightoil', '10x petroleum'], 'hbm:sulfur')
 * e.refinery.refinery('hotoil', ['50x heavyoil', '25x naphtha', '15x lightoil', '10x petroleum'])
 * e.refinery.vacuum('oil', ['40x heavyoil_vacuum', '25x reformate', '20x lightoil_vacuum', '15x sourgas'])
 * e.refinery.removeRefinery('hotcrackoil')
 * e.refinery.removeVacuum('oil_ds')
 * e.refinery.removeById('hbm:refinery/vacuum/oil')   id = refinery|vacuum / input fluid
 * </pre>
 * Exactly four fractions per 100 mB input; the refinery byproduct drops every 100 operations.
 */
public final class RefineryHandler extends TableHandler {

    @Override
    protected Object registry() {
        return RefineryRecipes.class;
    }

    @Override
    public List<RecipeView> views() {
        List<Map.Entry<FluidType, ?>> rows = new ArrayList<>(RefineryRecipes.getAll().entrySet());
        rows.addAll(RefineryRecipes.getAllVacuum().entrySet());
        return RecipeIds.views(machine(), rows,
                e -> (e.getValue() instanceof VacuumRecipe ? "vacuum/" : "refinery/") + RecipeIds.spec(e.getKey()),
                e -> List.of(RecipeIds.spec(new FluidStack(e.getKey(), 100))),
                RefineryHandler::outputs);
    }

    private static List<String> outputs(Map.Entry<FluidType, ?> row) {
        List<String> out = new ArrayList<>();
        if (row.getValue() instanceof RefineryRecipe recipe) {
            Arrays.stream(recipe.fractions()).map(RecipeIds::spec).forEach(out::add);
            if (!recipe.byproduct().isEmpty()) out.add(RecipeIds.spec(recipe.byproduct()));
        } else {
            Arrays.stream(((VacuumRecipe) row.getValue()).fractions()).map(RecipeIds::spec).forEach(out::add);
        }
        return out;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void delete(RecipeView view) {
        Map.Entry<FluidType, ?> row = (Map.Entry<FluidType, ?>) view.recipe();
        if (row.getValue() instanceof VacuumRecipe) RefineryRecipes.removeVacuum(row.getKey());
        else RefineryRecipes.removeRefinery(row.getKey());
    }

    @Override
    public void reset() {
        RefineryRecipes.reset();
    }

    public void refinery(Spec.Fluid input, Spec.Fluid[] fractions) {
        refinery(input, fractions, null);
    }

    public void refinery(Spec.Fluid input, Spec.Fluid[] fractions, Spec.Item byproduct) {
        edit("refinery " + input, () -> {
            FluidStack[] f = fractions(fractions);
            RefineryRecipes.putRefinery(ScriptSpecs.fluidType(input), new RefineryRecipe(
                    f[0], f[1], f[2], f[3], byproduct == null ? ItemStack.EMPTY : ScriptSpecs.stack(byproduct)));
        });
    }

    public void vacuum(Spec.Fluid input, Spec.Fluid[] fractions) {
        edit("vacuum " + input, () -> {
            FluidStack[] f = fractions(fractions);
            RefineryRecipes.putVacuum(ScriptSpecs.fluidType(input), new VacuumRecipe(f[0], f[1], f[2], f[3]));
        });
    }

    public void removeRefinery(Spec.Fluid input) {
        edit("removeRefinery " + input, () -> RefineryRecipes.removeRefinery(ScriptSpecs.fluidType(input)));
    }

    public void removeVacuum(Spec.Fluid input) {
        edit("removeVacuum " + input, () -> RefineryRecipes.removeVacuum(ScriptSpecs.fluidType(input)));
    }

    private static FluidStack[] fractions(Spec.Fluid[] specs) {
        if (specs.length != 4) throw new IllegalArgumentException("expected 4 fractions, got " + specs.length);
        return ScriptSpecs.fluids(specs);
    }
}
