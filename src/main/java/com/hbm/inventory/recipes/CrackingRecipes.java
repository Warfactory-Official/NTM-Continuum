package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Catalytic cracker recipes — 1:1 port of the 1.12.2 {@code CrackingRecipes}. Inputs are always 100 mB
 * (+ 200 mB steam, fixed) → the two outputs {@code left}/{@code right} (+ 2 mB spentsteam, fixed). The
 * {@code KEROSENE} entry has {@code right = NONE 0} — the cracker only emits {@code left} for it.
 */
public final class CrackingRecipes {

    private CrackingRecipes() {}

    // cracking in percent (verbatim)
    public static final int oil_crack_oil = 80;
    public static final int oil_crack_petro = 20;
    public static final int bitumen_crack_oil = 80;
    public static final int bitumen_crack_aroma = 20;
    public static final int smear_crack_napht = 60;
    public static final int smear_crack_petro = 40;
    public static final int gas_crack_petro = 30;
    public static final int gas_crack_unsat = 20;
    public static final int diesel_crack_kero = 40;
    public static final int diesel_crack_petro = 30;
    public static final int kero_crack_petro = 60;
    public static final int wood_crack_aroma = 10;
    public static final int wood_crack_heat = 40;
    public static final int xyl_crack_aroma = 80;
    public static final int xyl_crack_petro = 20;

    /** Two outputs per 100 mB input (+200 mB steam → +2 mB spentsteam, both fixed in the machine). */
    public record CrackingRecipe(FluidStack left, FluidStack right) {}

    private static final Map<FluidType, CrackingRecipe> cracking = new LinkedHashMap<>();
    private static boolean initialized = false;

    public static CrackingRecipe getCracking(FluidType oil) {
        init();
        return cracking.get(oil);
    }

    public static Map<FluidType, CrackingRecipe> getAll() {
        init();
        return Collections.unmodifiableMap(cracking);
    }

    private static synchronized void init() {
        if (initialized) return;
        initialized = true;

        cracking.put(Fluids.OIL, new CrackingRecipe(new FluidStack(Fluids.CRACKOIL, oil_crack_oil), new FluidStack(Fluids.PETROLEUM, oil_crack_petro)));
        cracking.put(Fluids.BITUMEN, new CrackingRecipe(new FluidStack(Fluids.OIL, bitumen_crack_oil), new FluidStack(Fluids.AROMATICS, bitumen_crack_aroma)));
        cracking.put(Fluids.SMEAR, new CrackingRecipe(new FluidStack(Fluids.NAPHTHA, smear_crack_napht), new FluidStack(Fluids.PETROLEUM, smear_crack_petro)));
        cracking.put(Fluids.GAS, new CrackingRecipe(new FluidStack(Fluids.PETROLEUM, gas_crack_petro), new FluidStack(Fluids.UNSATURATEDS, gas_crack_unsat)));
        cracking.put(Fluids.DIESEL, new CrackingRecipe(new FluidStack(Fluids.KEROSENE, diesel_crack_kero), new FluidStack(Fluids.PETROLEUM, diesel_crack_petro)));
        cracking.put(Fluids.DIESEL_CRACK, new CrackingRecipe(new FluidStack(Fluids.KEROSENE, diesel_crack_kero), new FluidStack(Fluids.PETROLEUM, diesel_crack_petro)));
        cracking.put(Fluids.KEROSENE, new CrackingRecipe(new FluidStack(Fluids.PETROLEUM, kero_crack_petro), new FluidStack(Fluids.NONE, 0)));
        cracking.put(Fluids.WOODOIL, new CrackingRecipe(new FluidStack(Fluids.HEATINGOIL, wood_crack_heat), new FluidStack(Fluids.AROMATICS, wood_crack_aroma)));
        cracking.put(Fluids.XYLENE, new CrackingRecipe(new FluidStack(Fluids.AROMATICS, xyl_crack_aroma), new FluidStack(Fluids.PETROLEUM, xyl_crack_petro)));
        cracking.put(Fluids.HEATINGOIL_VACUUM, new CrackingRecipe(new FluidStack(Fluids.HEATINGOIL, 80), new FluidStack(Fluids.REFORMGAS, 20)));
        cracking.put(Fluids.REFORMATE, new CrackingRecipe(new FluidStack(Fluids.UNSATURATEDS, 40), new FluidStack(Fluids.REFORMGAS, 60)));
        dev.golem.core.content.Replay.apply(CrackingRecipes.class);
    }

    public static void put(FluidType oil, CrackingRecipe recipe) {
        init();
        cracking.put(oil, recipe);
    }

    public static boolean remove(FluidType oil) {
        init();
        return cracking.remove(oil) != null;
    }

    public static synchronized void reset() {
        cracking.clear();
        initialized = false;
    }
}
