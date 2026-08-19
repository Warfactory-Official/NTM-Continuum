package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipe;

/**
 * One fusion fuel cycle. Beyond the generic recipe's inputs, outputs, duration and power draw, a fusion
 * recipe carries the three numbers that make the reactor a reactor:
 *
 * <ul>
 *   <li>{@link #ignitionTemp} — the klystron energy the torus must be fed before the plasma lights at all,</li>
 *   <li>{@link #outputTemp} — the plasma energy it then puts out at full blast, and</li>
 *   <li>{@link #neutronFlux} — the neutron output, which breeders take and which is never shared.</li>
 * </ul>
 *
 * plus the plasma's colour, which the torus hands to every receiver so their renderers match the fuel.
 */
public class FusionRecipe extends GenericRecipe {

    /** Minimum klystron energy to ignite the plasma. */
    public long ignitionTemp;
    /** Plasma output energy at full blast. */
    public long outputTemp;
    /** Neutron output at full blast. */
    public double neutronFlux;

    public float r = 1F;
    public float g = 0.2F;
    public float b = 0.6F;

    public FusionRecipe(String name) {
        super(name);
    }

    public FusionRecipe setInputEnergy(long ignitionTemp) { this.ignitionTemp = ignitionTemp; return this; }
    public FusionRecipe setOutputEnergy(long outputTemp) { this.outputTemp = outputTemp; return this; }
    public FusionRecipe setOutputFlux(double neutronFlux) { this.neutronFlux = neutronFlux; return this; }

    public FusionRecipe setRGB(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
        return this;
    }
}
