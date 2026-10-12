package com.hbm.recipes.tables;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.FractionRecipes;
import com.hbm.inventory.recipes.FractionRecipes.Row;
import dev.golem.core.world.RecipeSource;

/** 1.7 {@code FractionRecipes.registerDefaults}. */
public final class FractionTable extends RecipeSource<Row> {

    public FractionTable() {
        super(FractionRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {
        put(Fluids.HEAVYOIL,            new FluidStack(Fluids.BITUMEN,                  30),    new FluidStack(Fluids.SMEAR,                70));
        put(Fluids.HEAVYOIL_VACUUM,     new FluidStack(Fluids.SMEAR,                    40),    new FluidStack(Fluids.HEATINGOIL_VACUUM,    60));
        put(Fluids.SMEAR,               new FluidStack(Fluids.HEATINGOIL,               60),    new FluidStack(Fluids.LUBRICANT,            40));
        put(Fluids.NAPHTHA,             new FluidStack(Fluids.HEATINGOIL,               40),    new FluidStack(Fluids.DIESEL,               60));
        put(Fluids.NAPHTHA_DS,          new FluidStack(Fluids.XYLENE,                   60),    new FluidStack(Fluids.DIESEL_REFORM,        40));
        put(Fluids.NAPHTHA_CRACK,       new FluidStack(Fluids.HEATINGOIL,               30),    new FluidStack(Fluids.DIESEL_CRACK,         70));
        put(Fluids.LIGHTOIL,            new FluidStack(Fluids.DIESEL,                   40),    new FluidStack(Fluids.KEROSENE,             60));
        put(Fluids.LIGHTOIL_DS,         new FluidStack(Fluids.DIESEL_REFORM,            60),    new FluidStack(Fluids.KEROSENE_REFORM,      40));
        put(Fluids.LIGHTOIL_CRACK,      new FluidStack(Fluids.KEROSENE,                 70),    new FluidStack(Fluids.PETROLEUM,            30));
        put(Fluids.COALOIL,             new FluidStack(Fluids.COALGAS,                  30),    new FluidStack(Fluids.OIL,                  70));
        put(Fluids.COALCREOSOTE,        new FluidStack(Fluids.COALOIL,                  10),    new FluidStack(Fluids.BITUMEN,              90));
        put(Fluids.REFORMATE,           new FluidStack(Fluids.AROMATICS,                40),    new FluidStack(Fluids.XYLENE,               60));
        put(Fluids.LIGHTOIL_VACUUM,     new FluidStack(Fluids.KEROSENE,                 70),    new FluidStack(Fluids.REFORMGAS,            30));
        put(Fluids.EGG,                 new FluidStack(Fluids.CHOLESTEROL,              50),    new FluidStack(Fluids.RADIOSOLVENT,         50));
        put(Fluids.OIL_COKER,           new FluidStack(Fluids.CRACKOIL,                 30),    new FluidStack(Fluids.HEATINGOIL,           70));
        put(Fluids.NAPHTHA_COKER,       new FluidStack(Fluids.NAPHTHA_CRACK,            75),    new FluidStack(Fluids.LIGHTOIL_CRACK,       25));
        put(Fluids.GAS_COKER,           new FluidStack(Fluids.AROMATICS,                25),    new FluidStack(Fluids.CARBONDIOXIDE,        75));
        put(Fluids.CHLOROCALCITE_MIX,   new FluidStack(Fluids.CHLOROCALCITE_CLEANED,    50),    new FluidStack(Fluids.COLLOID,              50));
        put(Fluids.BAUXITE_SOLUTION,    new FluidStack(Fluids.REDMUD,                   50),    new FluidStack(Fluids.SODIUM_ALUMINATE,     50));
    }

    /** 1.7 {@code fractions.put(input, new Pair(left, right))}; the id is the input fluid. */
    private void put(FluidType input, FluidStack left, FluidStack right) {
        register(input.getKey(), new Row(input, left, right));
    }
}
