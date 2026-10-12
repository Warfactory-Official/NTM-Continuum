package com.hbm.inventory.fluid.trait;

/**
 * 1.7 {@code FT_Flammable}: how much heat energy (usually HE 1:1) a bucket holds when burned (oil burner, gas flare, the
 * solid-fuel rows of the coker, pyrolysis oven and solidifier). In a {@code fluid_property} file:
 * {@code "flammable": {"energy": 1000000}} (1.7's own trait JSON). Read through
 * {@link com.hbm.inventory.fluid.FluidCombustion}.
 */
public final class FT_Flammable {

    private final long energy;

    public FT_Flammable(long energy) {
        this.energy = energy;
    }

    public long getHeatEnergy() {
        return energy;
    }
}
