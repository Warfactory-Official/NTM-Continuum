package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * One fusion fuel cycle. Beyond the generic recipe's inputs, outputs, duration and power draw, a fusion
 * recipe carries the three numbers that make the reactor a reactor:
 *
 * <ul>
 *   <li>{@link #ignitionTemp}: the klystron energy the torus must be fed before the plasma lights at all,</li>
 *   <li>{@link #outputTemp}: the plasma energy it then puts out at full blast, and</li>
 *   <li>{@link #neutronFlux}: the neutron output, which breeders take and which is never shared.</li>
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

    /** The fields a fusion row adds, under ntm-next's names. */
    private record Extras(long ignitionTemp, long outputTemp, double neutronFlux, float r, float g, float b) {}

    private static final MapCodec<Extras> EXTRAS = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.LONG.optionalFieldOf("ignition_temp", 0L).forGetter(Extras::ignitionTemp),
            Codec.LONG.optionalFieldOf("output_temp", 0L).forGetter(Extras::outputTemp),
            Codec.DOUBLE.optionalFieldOf("neutron_flux", 0D).forGetter(Extras::neutronFlux),
            Codec.FLOAT.optionalFieldOf("red", 1F).forGetter(Extras::r),
            Codec.FLOAT.optionalFieldOf("green", 0.2F).forGetter(Extras::g),
            Codec.FLOAT.optionalFieldOf("blue", 0.6F).forGetter(Extras::b)
    ).apply(i, Extras::new));

    /** The row codec of the fusion table: the generic fields plus {@code ignition_temp}, {@code output_temp}, {@code neutron_flux} and the plasma colour. */
    public static MapCodec<FusionRecipe> codec() {
        return GenericRecipeCodec.codec(FusionRecipe::new, EXTRAS,
                recipe -> new Extras(recipe.ignitionTemp, recipe.outputTemp, recipe.neutronFlux, recipe.r, recipe.g, recipe.b),
                (recipe, extras) -> {
                    recipe.ignitionTemp = extras.ignitionTemp();
                    recipe.outputTemp = extras.outputTemp();
                    recipe.neutronFlux = extras.neutronFlux();
                    recipe.setRGB(extras.r(), extras.g(), extras.b());
                });
    }

    @Override
    public String describeExtra() {
        return "ignition=" + ignitionTemp + " output=" + outputTemp + " flux=" + neutronFlux + " rgb=" + r + "," + g + "," + b;
    }
}
