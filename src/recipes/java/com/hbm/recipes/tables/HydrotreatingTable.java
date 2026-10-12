package com.hbm.recipes.tables;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.HydrotreatingRecipes;
import com.hbm.inventory.recipes.HydrotreatingRecipes.Row;
import dev.golem.core.world.RecipeSource;
import java.util.List;

/** 1.7 {@code HydrotreatingRecipes.registerDefaults}. */
public final class HydrotreatingTable extends RecipeSource<Row> {

    public HydrotreatingTable() {
        super(HydrotreatingRecipes.TABLE.table.table);
    }

    @Override
    protected void registerDefaults() {

        put(Fluids.OIL,
            new FluidStack(Fluids.HYDROGEN, 5, 1),
            new FluidStack(Fluids.OIL_DS, 90),
            new FluidStack(Fluids.SOURGAS, 15)
        );

        put(Fluids.CRACKOIL,
            new FluidStack(Fluids.HYDROGEN, 5, 1),
            new FluidStack(Fluids.CRACKOIL_DS, 90),
            new FluidStack(Fluids.SOURGAS, 15)
        );

        put(Fluids.GAS,
            new FluidStack(Fluids.HYDROGEN, 5, 1),
            new FluidStack(Fluids.PETROLEUM, 80),
            new FluidStack(Fluids.SOURGAS, 15)
        );

        put(Fluids.DIESEL_CRACK,
            new FluidStack(Fluids.HYDROGEN, 10, 1),
            new FluidStack(Fluids.DIESEL, 80),
            new FluidStack(Fluids.SOURGAS, 30)
        );

        put(Fluids.DIESEL_CRACK_REFORM,
            new FluidStack(Fluids.HYDROGEN, 10, 1),
            new FluidStack(Fluids.DIESEL_REFORM, 80),
            new FluidStack(Fluids.SOURGAS, 30)
        );

        put(Fluids.COALOIL,
            new FluidStack(Fluids.HYDROGEN, 10, 1),
            new FluidStack(Fluids.COALGAS, 80),
            new FluidStack(Fluids.SOURGAS, 15)
        );
    }

    /** 1.7 {@code recipes.put(input, new Triplet(hydrogen, out1, out2))}; the id is the input fluid. */
    private void put(FluidType input, FluidStack hydrogen, FluidStack out1, FluidStack out2) {
        register(input.getKey(), new Row(input, hydrogen, List.of(out1, out2)));
    }
}
