package com.hbm.inventory.recipes;

import com.hbm.Constants;
import com.hbm.inventory.recipes.loader.DataGenericRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import dev.golem.core.world.RecipeFields;

/**
 * The fusion torus's fuel cycles (1.7 {@code FusionRecipes}): the datapack table {@code hbm:fusion}, one file per cycle
 * in {@code data/hbm/recipe/fusion/}. A row adds ntm-next's {@code ignition_temp}, {@code output_temp},
 * {@code neutron_flux} and {@code red}/{@code green}/{@code blue} ({@link FusionRecipe#codec}).
 *
 * <p>The cycles form a progression rather than a menu: each cycle's {@link FusionRecipe#ignitionTemp} is the
 * klystron energy needed to light it, and its {@link FusionRecipe#outputTemp} is what it then gives
 * back, so an early cycle's output is what lets you afford the next one's ignition. D-D and D-O light
 * off a single klystron; chlorine needs the tritium or helium-3 phases running first; DHC needs
 * chlorine; and stellar flux sits at the top at 10 MHE ignition for 50 MHE output. The neutron flux of a cycle
 * is a fraction of the breeder's buffer ({@link #BREEDER_CAPACITY}).
 *
 * <p>{@code maxInput} is the largest ignition temperature of any cycle, which the creative klystron
 * uses as its output so it can light anything; recomputed whenever the table rebuilds.
 */
public final class FusionRecipes extends DataGenericRecipes<FusionRecipe> {

    public static final FusionRecipes INSTANCE = new FusionRecipes();

    /** The highest ignition temperature of any registered cycle (the creative klystron's output). */
    public long maxInput;

    /**
     * The fusion breeder's flux buffer. Cycle flux values are written as fractions of it, so a cycle's
     * breeding rate reads as "fills a breeder in N seconds" rather than as a bare number. Lives here
     * rather than on the breeder so the recipe table has no forward dependency on that machine.
     */
    public static final double BREEDER_CAPACITY = 10_000D;

    private FusionRecipes() {
        super(Constants.MOD_ID, "fusion", FusionRecipe::codec, RecipeFields.of()
                .add(RecipeFields.field("ignition_temp", RecipeFields.Kind.LONG).optional().alias("ignitionTemp"))
                .add(RecipeFields.field("output_temp", RecipeFields.Kind.LONG).optional().alias("outputTemp"))
                .add(RecipeFields.field("neutron_flux", RecipeFields.Kind.DOUBLE).optional().alias("neutronFlux"))
                .add(RecipeFields.field("red", RecipeFields.Kind.DOUBLE).optional())
                .add(RecipeFields.field("green", RecipeFields.Kind.DOUBLE).optional())
                .add(RecipeFields.field("blue", RecipeFields.Kind.DOUBLE).optional()));
    }

    /** Declares the table at mod construction (its recipe type and serializer register with HBM's content). */
    public static void declare() {}

    @Override
    protected void rebuilt() {
        long max = 0;
        for (GenericRecipe recipe : recipeOrderedList) {
            if (recipe instanceof FusionRecipe fusion && fusion.ignitionTemp > max) max = fusion.ignitionTemp;
        }
        maxInput = max;
    }
}
