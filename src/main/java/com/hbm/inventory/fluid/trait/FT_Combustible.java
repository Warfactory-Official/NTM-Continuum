package com.hbm.inventory.fluid.trait;

import com.hbm.inventory.fluid.FluidCombustion;

/**
 * 1.7 {@code FT_Combustible}: the fuel grade and the combustion energy per bucket (diesel generator, combustion engine,
 * turbines). In a {@code fluid_property} file: {@code "combustible": {"grade": "HIGH", "energy": 2500000}} (1.7's own trait
 * JSON; grades {@link FluidCombustion.FuelGrade}). Read through {@link FluidCombustion}.
 */
public final class FT_Combustible {

    private final FluidCombustion.Combustible value;

    public FT_Combustible(FluidCombustion.FuelGrade grade, long energy) {
        this.value = new FluidCombustion.Combustible(grade, energy);
    }

    public FluidCombustion.FuelGrade getGrade() {
        return value.grade();
    }

    public long getCombustionEnergy() {
        return value.energyPerBucket();
    }

    /** The grade and energy as one value. */
    public FluidCombustion.Combustible combustible() {
        return value;
    }
}
