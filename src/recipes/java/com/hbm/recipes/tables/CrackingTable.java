package com.hbm.recipes.tables;

import static com.hbm.inventory.recipes.CrackingRecipes.*;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.CrackingRecipes;
import com.hbm.inventory.recipes.CrackingRecipes.Row;
import dev.golem.core.world.RecipeSource;

/** 1.7 {@code CrackingRecipes.registerDefaults}. */
public final class CrackingTable extends RecipeSource<Row> {

    public CrackingTable() {
        super(CrackingRecipes.TABLE.table.table);
    }

    @Override
    protected void registerDefaults() {
        put(Fluids.OIL,                 new FluidStack(Fluids.CRACKOIL,     oil_crack_oil),     new FluidStack(Fluids.PETROLEUM,    oil_crack_petro));
        put(Fluids.BITUMEN,             new FluidStack(Fluids.OIL,          bitumen_crack_oil), new FluidStack(Fluids.AROMATICS,    bitumen_crack_aroma));
        put(Fluids.SMEAR,               new FluidStack(Fluids.NAPHTHA,      smear_crack_napht), new FluidStack(Fluids.PETROLEUM,    smear_crack_petro));
        put(Fluids.GAS,                 new FluidStack(Fluids.PETROLEUM,    gas_crack_petro),   new FluidStack(Fluids.UNSATURATEDS, gas_crack_unsat));
        put(Fluids.DIESEL,              new FluidStack(Fluids.KEROSENE,     diesel_crack_kero), new FluidStack(Fluids.PETROLEUM,    diesel_crack_petro));
        put(Fluids.DIESEL_CRACK,        new FluidStack(Fluids.KEROSENE,     diesel_crack_kero), new FluidStack(Fluids.PETROLEUM,    diesel_crack_petro));
        put(Fluids.KEROSENE,            new FluidStack(Fluids.PETROLEUM,    kero_crack_petro),  new FluidStack(Fluids.NONE,         0));
        put(Fluids.WOODOIL,             new FluidStack(Fluids.HEATINGOIL,   wood_crack_heat),   new FluidStack(Fluids.AROMATICS,    wood_crack_aroma));
        put(Fluids.XYLENE,              new FluidStack(Fluids.AROMATICS,    xyl_crack_aroma),   new FluidStack(Fluids.PETROLEUM,    xyl_crack_petro));
        put(Fluids.HEATINGOIL_VACUUM,   new FluidStack(Fluids.HEATINGOIL,   80),                new FluidStack(Fluids.REFORMGAS,    20));
        put(Fluids.REFORMATE,           new FluidStack(Fluids.UNSATURATEDS, 40),                new FluidStack(Fluids.REFORMGAS,    60));
        put(Fluids.BIOGAS,              new FluidStack(Fluids.PETROLEUM,    20),                new FluidStack(Fluids.AROMATICS,    20));
    }

    /** 1.7 {@code cracking.put(input, new Pair(left, right))}; the id is the input fluid. */
    private void put(FluidType input, FluidStack left, FluidStack right) {
        register(input.getKey(), new Row(input, left, right));
    }
}
