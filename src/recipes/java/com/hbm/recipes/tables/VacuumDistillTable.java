package com.hbm.recipes.tables;

import static com.hbm.inventory.recipes.RefineryRecipes.*;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.RefineryRecipes;
import com.hbm.inventory.recipes.RefineryRecipes.VacuumRow;
import dev.golem.core.world.RecipeSource;
import java.util.List;

/** 1.7 {@code VacuumRefineryRecipes.registerDefaults}. */
public final class VacuumDistillTable extends RecipeSource<VacuumRow> {

    public VacuumDistillTable() {
        super(RefineryRecipes.VACUUM.table.table);
    }

    @Override
    protected void registerDefaults() {

        put(Fluids.OIL,
                new FluidStack(Fluids.HEAVYOIL_VACUUM,  vac_frac_heavy),
                new FluidStack(Fluids.REFORMATE,        vac_frac_reform),
                new FluidStack(Fluids.LIGHTOIL_VACUUM,  vac_frac_light),
                new FluidStack(Fluids.SOURGAS,          vac_frac_sour)
                );
        put(Fluids.OIL_DS,
                new FluidStack(Fluids.HEAVYOIL_VACUUM,  vac_frac_heavy),
                new FluidStack(Fluids.REFORMATE,        vac_frac_reform),
                new FluidStack(Fluids.LIGHTOIL_VACUUM,  vac_frac_light),
                new FluidStack(Fluids.REFORMGAS,        vac_frac_sour)
                );
    }

    /** 1.7 {@code recipes.put(input, new VacuumRefineryRecipe(frac1, frac2, frac3, frac4))}; the id is the input fluid. */
    private void put(FluidType input, FluidStack frac1, FluidStack frac2, FluidStack frac3, FluidStack frac4) {
        register(input.getKey(), new VacuumRow(input, List.of(frac1, frac2, frac3, frac4)));
    }
}
