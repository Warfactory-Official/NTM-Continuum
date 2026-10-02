package com.hbm.inventory.fluid.trait;

/**
 * How a quantity of fluid left its container, which decides which of a fluid's pollution tables
 * ({@link FT_Polluting}) applies.
 *
 * <p>1.7 nests this in {@code FluidTrait}. The port's traits are plain objects looked up by their own
 * class and have no {@code FluidTrait} base, so the enum stands on its own in the same package.
 */
public enum FluidReleaseType {
    /** If fluid is deleted entirely, shouldn't be used. */
    VOID,
    /** If fluid is burned or combusted. */
    BURN,
    /** If fluid is spilled via leakage or the container breaking. */
    SPILL
}
