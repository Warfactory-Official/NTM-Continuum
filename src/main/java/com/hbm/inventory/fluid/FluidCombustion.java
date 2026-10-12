package com.hbm.inventory.fluid;

import com.hbm.inventory.fluid.trait.FT_Combustible;
import com.hbm.inventory.fluid.trait.FT_Flammable;

/**
 * A fluid's fuel values, read from its traits {@link FT_Flammable} (heat energy per bucket: oil burner, gas flare, the
 * solid-fuel rows of the coker, pyrolysis oven and solidifier) and {@link FT_Combustible} (fuel grade and combustion energy
 * per bucket: diesel generator, combustion engine, turbines). Both are data: {@code "flammable": {"energy"}} and
 * {@code "combustible": {"grade", "energy"}} in the fluid's {@code fluid_property} file, so addons and datapacks set them
 * like any trait and a datapack reload applies at once.
 *
 * <p>HBM's values were converted once from the former Java table, which replicated 1.7's {@code Fluids.init()} exactly: the
 * literal trait registrations, then the "almighty excel spreadsheet" {@code registerCalculatedFuel} block that overwrites
 * most of them with its magnitude-based {@code round()}. The files hold the results, not the spreadsheet.
 */
public final class FluidCombustion {

    private FluidCombustion() {}

    /** Old {@code FT_Combustible.FuelGrade}. */
    public enum FuelGrade { LOW, MEDIUM, HIGH, AERO, GAS }

    public record Combustible(FuelGrade grade, long energyPerBucket) {}

    /** TU per bucket when burned for heat (old FT_Flammable.getHeatEnergy), 0 when not flammable. */
    public static long getFlammableEnergy(FluidType type) {
        FT_Flammable trait = type == null ? null : type.getTrait(FT_Flammable.class);
        return trait == null ? 0L : trait.getHeatEnergy();
    }

    public static boolean isFlammable(FluidType type) {
        return type != null && type.hasTrait(FT_Flammable.class);
    }

    /** Fuel grade + combustion energy per bucket (old FT_Combustible), null when not combustible. */
    public static Combustible getCombustible(FluidType type) {
        FT_Combustible trait = type == null ? null : type.getTrait(FT_Combustible.class);
        return trait == null ? null : trait.combustible();
    }

    /**
     * 1.7 {@code FT_Gaseous} alone (not {@code FT_Gaseous_ART}); consumers that test both use
     * {@link com.hbm.inventory.fluid.trait.FluidTraitSimple#isGaseousAny}.
     */
    public static boolean isGaseous(FluidType type) {
        return type != null && type.hasTrait(com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous.class);
    }

    /** The 1.7 drain's {@code FT_Liquid && FT_Viscous} gate. */
    public static boolean isLiquidAndViscous(FluidType type) {
        return type != null && type.hasTrait(com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Liquid.class)
                && type.hasTrait(com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Viscous.class);
    }
}
