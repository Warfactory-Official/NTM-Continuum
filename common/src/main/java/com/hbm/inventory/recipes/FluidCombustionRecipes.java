package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The flame-heat table — 1:1 port of the 1.12.2 {@code FluidCombustionRecipes} (TU per <b>mB</b>,
 * "for 1000 mb" per the old comment): the fuel values used by flame boilers / burn heaters and shown
 * in JEI. The cross-mod Forge-fluid-name entries (crude_oil, fuel, pyrotheum, …) are dropped — the
 * port has no Forge fluid compat.
 */
public final class FluidCombustionRecipes {

    private FluidCombustionRecipes() {}

    private static final Map<FluidType, Integer> resultingTU = new LinkedHashMap<>();
    private static boolean initialized = false;

    /** TU released per mB burned; 0 when the fluid has no flame recipe. */
    public static int getFlameEnergy(FluidType fluid) {
        init();
        Integer heat = resultingTU.get(fluid);
        return heat == null ? 0 : heat;
    }

    public static boolean hasFuelRecipe(FluidType fluid) {
        init();
        return resultingTU.containsKey(fluid);
    }

    /** Read-only view for JEI display. */
    public static Map<FluidType, Integer> getAll() {
        init();
        return Collections.unmodifiableMap(resultingTU);
    }

    private static synchronized void init() {
        if (initialized) return;
        initialized = true;

        add(Fluids.HYDROGEN, 5);
        add(Fluids.DEUTERIUM, 5);
        add(Fluids.TRITIUM, 5);

        add(Fluids.OIL, 10);
        add(Fluids.HOTOIL, 10);
        add(Fluids.CRACKOIL, 10);
        add(Fluids.HOTCRACKOIL, 10);

        add(Fluids.GAS, 10);
        add(Fluids.FISHOIL, 15);
        add(Fluids.LUBRICANT, 20);
        add(Fluids.AROMATICS, 25);
        add(Fluids.PETROLEUM, 25);
        add(Fluids.BIOGAS, 25);
        add(Fluids.BITUMEN, 35);
        add(Fluids.HEAVYOIL, 50);
        add(Fluids.SMEAR, 50);
        add(Fluids.ETHANOL, 75);
        add(Fluids.RECLAIMED, 100);
        add(Fluids.PETROIL, 125);
        add(Fluids.NAPHTHA, 125);
        add(Fluids.HEATINGOIL, 150);
        add(Fluids.BIOFUEL, 150);
        add(Fluids.DIESEL, 200);
        add(Fluids.LIGHTOIL, 200);
        add(Fluids.KEROSENE, 300);
        add(Fluids.GASOLINE, 800);
        add(Fluids.UNSATURATEDS, 1_000);
        add(Fluids.NITAN, 2_000);
        add(Fluids.BALEFIRE, 10_000);
    }

    private static void add(FluidType fluid, int heatPerMilliBucket) {
        resultingTU.put(fluid, heatPerMilliBucket);
    }
}
