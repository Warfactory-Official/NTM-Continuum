package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingType;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Read-only projection of the {@link FT_Heatable} fluid trait, which is data ({@code heatable} in a fluid's
 * {@code fluid_property} file), for the machines that 1.7 has read the trait's <b>first step</b> directly:
 * <ul>
 *   <li>the heat boilers ({@code TileEntityHeatBoiler}, the industrial boiler, the fusion boiler) take a fluid only when its
 *       BOILER efficiency is above 0, and need {@code max(heatReq / efficiency, 1)} TU per step (1.7
 *       {@code TileEntityHeatBoiler.heatFluid}); HBM's boiler fluids all have efficiency 1.0;</li>
 *   <li>the Watz reads the first step of its coolant with no efficiency factor (1.7 {@code TileEntityWatz.updateCoolant});
 *       its tank is always COOLANT.</li>
 * </ul>
 * Read on every call, so a datapack reload of the fluid properties applies at once. 1.7 has no heatable trait on steam, so
 * the boiler cannot upgrade steam.
 */
public final class HeatRecipes {

    private HeatRecipes() {}

    /** heatReq TU + amountReq mB input → amountProduced mB of output. */
    public record HeatStep(int heatReq, int amountReq, FluidType output, int amountProduced) {}

    /** The reactor-coolant heating step for this input fluid, or null if it isn't heatable. */
    public static HeatStep getReactorStep(FluidType input) {
        FT_Heatable trait = input == null ? null : input.getTrait(FT_Heatable.class);
        if (trait == null || trait.steps().isEmpty()) return null;
        FT_Heatable.HeatingStep step = trait.getFirstStep();
        return new HeatStep(step.heatReq, step.amountReq, step.typeProduced, step.amountProduced);
    }

    /** The boiler conversion for this input fluid, or null if the boiler can't heat it. */
    public static HeatStep getBoilerStep(FluidType input) {
        FT_Heatable trait = input == null ? null : input.getTrait(FT_Heatable.class);
        if (trait == null || trait.steps().isEmpty()) return null;
        double efficiency = trait.getEfficiency(HeatingType.BOILER);
        if (efficiency <= 0) return null;
        FT_Heatable.HeatingStep step = trait.getFirstStep();
        return new HeatStep((int) Math.max(step.heatReq / efficiency, 1), step.amountReq, step.typeProduced, step.amountProduced);
    }

    public static boolean hasBoilerRecipe(FluidType input) {
        return getBoilerStep(input) != null;
    }

    /** Every boiler conversion, for JEI display, in 1.7 BoilingHandler's Fluids.getInNiceOrder(). */
    public static Map<FluidType, HeatStep> getAll() {
        Map<FluidType, HeatStep> all = new LinkedHashMap<>();
        for (FluidType input : Fluids.getInNiceOrder()) {
            HeatStep step = getBoilerStep(input);
            if (step != null) all.put(input, step);
        }
        return Collections.unmodifiableMap(all);
    }
}
