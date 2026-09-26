package com.hbm.inventory.recipes;

import com.hbm.api.fluid.FluidType;
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
    /**
     * The reactor/heat-exchanger heatable fluids, which the boiler deliberately cannot heat. These are
     * the {@code FT_Heatable} entries whose efficiency map has no BOILER key — a reactor reads the raw
     * first step with no efficiency factor, which is how {@code TileEntityWatz.updateCoolant} used it.
     */
    private static final Map<FluidType, HeatStep> REACTOR = new LinkedHashMap<>();
    private static boolean initialized = false;

    /** The reactor-coolant heating step for this input fluid, or null if it isn't reactor-heatable. */
    public static HeatStep getReactorStep(FluidType input) {
        init();
        return REACTOR.get(input);
    }

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

    public static Map<FluidType, HeatStep> getAllReactor() {
        init();
        return Collections.unmodifiableMap(REACTOR);
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

        REACTOR.put(Fluids.COOLANT, new HeatStep(300, 1, Fluids.COOLANT_HOT, 1));
        dev.golem.core.content.Replay.apply(HeatRecipes.class);
    }

    public static void putBoiler(FluidType input, HeatStep step) {
        init();
        BOILER.put(input, step);
    }

    public static boolean removeBoiler(FluidType input) {
        init();
        return BOILER.remove(input) != null;
    }

    public static void putReactor(FluidType input, HeatStep step) {
        init();
        REACTOR.put(input, step);
    }

    public static boolean removeReactor(FluidType input) {
        init();
        return REACTOR.remove(input) != null;
    }

    public static synchronized void reset() {
        BOILER.clear();
        REACTOR.clear();
        initialized = false;
    }
}
