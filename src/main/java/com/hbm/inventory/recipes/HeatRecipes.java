package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The heat-boiler conversion table — the {@code FT_Heatable} first-step data from the old
 * {@code Fluids.init()} (L636-671), restricted to fluids with a BOILER efficiency (the old
 * {@code TileEntityHeatBoiler} only ever reads {@code getFirstStep()} and only runs when
 * {@code getEfficiency(BOILER) > 0}). Every boiler-heatable fluid in this fork has BOILER eff 1.0,
 * so the efficiency factor is folded in ({@code heatReq / 1.0}). COOLANT and PERFLUOROMETHYL are
 * heat-exchanger/reactor-only in the old data and are deliberately absent.
 */
public final class HeatRecipes {

    private HeatRecipes() {}

    /** heatReq TU + amountReq mB input → amountProduced mB of output. */
    public record HeatStep(int heatReq, int amountReq, FluidType output, int amountProduced) {}

    private static final Map<FluidType, HeatStep> BOILER = new LinkedHashMap<>();
    private static boolean initialized = false;

    /** The boiler conversion for this input fluid, or null if the boiler can't heat it. */
    public static HeatStep getBoilerStep(FluidType input) {
        init();
        return BOILER.get(input);
    }

    public static boolean hasBoilerRecipe(FluidType input) {
        init();
        return BOILER.containsKey(input);
    }

    /** Read-only view for JEI display. */
    public static Map<FluidType, HeatStep> getAll() {
        init();
        return Collections.unmodifiableMap(BOILER);
    }

    private static synchronized void init() {
        if (initialized) return;
        initialized = true;

        BOILER.put(Fluids.AIR, new HeatStep(5, 1, Fluids.AIRBLAST, 1));
        BOILER.put(Fluids.WATER, new HeatStep(200, 1, Fluids.STEAM, 100));
        BOILER.put(Fluids.STEAM, new HeatStep(2, 10, Fluids.HOTSTEAM, 1));
        BOILER.put(Fluids.HOTSTEAM, new HeatStep(18, 10, Fluids.SUPERHOTSTEAM, 1));
        BOILER.put(Fluids.SUPERHOTSTEAM, new HeatStep(120, 10, Fluids.ULTRAHOTSTEAM, 1));
        BOILER.put(Fluids.OIL, new HeatStep(10, 1, Fluids.HOTOIL, 1));
        BOILER.put(Fluids.OIL_DS, new HeatStep(10, 1, Fluids.HOTOIL_DS, 1));
        BOILER.put(Fluids.CRACKOIL, new HeatStep(10, 1, Fluids.HOTCRACKOIL, 1));
        BOILER.put(Fluids.CRACKOIL_DS, new HeatStep(10, 1, Fluids.HOTCRACKOIL_DS, 1));
    }
}
