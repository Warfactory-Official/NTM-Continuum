package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ItemEnums.EnumTarType;
import com.hbm.items.ModItems;
import com.hbm.items.ModOilItems;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Refinery recipes — 1:1 port of the 1.12.2 {@code RefineryRecipes.registerRefinery()}: per 100 mB of
 * hot input the four fraction outputs (in mB) plus the byproduct item emitted every 100 operations.
 * Also hosts the {@code vacuum} map ({@link #getVacuum}) consumed by the vacuum distiller.
 */
public final class RefineryRecipes {

    private RefineryRecipes() {}

    // the old public fraction constants, verbatim
    public static final int oil_frac_heavy = 50;
    public static final int oil_frac_naph = 25;
    public static final int oil_frac_light = 15;
    public static final int oil_frac_petro = 10;
    public static final int crack_frac_naph = 40;
    public static final int crack_frac_light = 30;
    public static final int crack_frac_aroma = 15;
    public static final int crack_frac_unsat = 15;
    public static final int oilds_frac_heavy = 30;
    public static final int oilds_frac_naph = 35;
    public static final int oilds_frac_light = 20;
    public static final int oilds_frac_unsat = 15;
    public static final int crackds_frac_naph = 35;
    public static final int crackds_frac_light = 35;
    public static final int crackds_frac_aroma = 15;
    public static final int crackds_frac_unsat = 15;

    /** Four fraction outputs (per 100 mB input) + the per-100-ops byproduct item. */
    public record RefineryRecipe(FluidStack frac1, FluidStack frac2, FluidStack frac3, FluidStack frac4, ItemStack byproduct) {
        public FluidStack[] fractions() {
            return new FluidStack[]{frac1, frac2, frac3, frac4};
        }
    }

    // vacuum-distiller fraction constants (old RefineryRecipes)
    public static final int vac_frac_heavy = 40;
    public static final int vac_frac_reform = 25;
    public static final int vac_frac_light = 20;
    public static final int vac_frac_sour = 15;

    /** Vacuum distiller: four fraction outputs per 100 mB input (no byproduct item). */
    public record VacuumRecipe(FluidStack frac1, FluidStack frac2, FluidStack frac3, FluidStack frac4) {
        public FluidStack[] fractions() {
            return new FluidStack[]{frac1, frac2, frac3, frac4};
        }
    }

    private static final Map<FluidType, RefineryRecipe> refinery = new LinkedHashMap<>();
    private static final Map<FluidType, VacuumRecipe> vacuum = new LinkedHashMap<>();
    private static boolean initialized = false;

    public static RefineryRecipe getRefinery(FluidType oil) {
        init();
        return refinery.get(oil);
    }

    public static VacuumRecipe getVacuum(FluidType oil) {
        init();
        return vacuum.get(oil);
    }

    /** Read-only view for JEI display. */
    public static Map<FluidType, RefineryRecipe> getAll() {
        init();
        return Collections.unmodifiableMap(refinery);
    }

    /** Read-only view for JEI display (vacuum distiller). */
    public static Map<FluidType, VacuumRecipe> getAllVacuum() {
        init();
        return Collections.unmodifiableMap(vacuum);
    }

    private static synchronized void init() {
        if (initialized) return;
        initialized = true;

        refinery.put(Fluids.HOTOIL, new RefineryRecipe(
                new FluidStack(Fluids.HEAVYOIL, oil_frac_heavy),
                new FluidStack(Fluids.NAPHTHA, oil_frac_naph),
                new FluidStack(Fluids.LIGHTOIL, oil_frac_light),
                new FluidStack(Fluids.PETROLEUM, oil_frac_petro),
                new ItemStack(ModItems.sulfur.get())
        ));
        refinery.put(Fluids.HOTCRACKOIL, new RefineryRecipe(
                new FluidStack(Fluids.NAPHTHA_CRACK, crack_frac_naph),
                new FluidStack(Fluids.LIGHTOIL_CRACK, crack_frac_light),
                new FluidStack(Fluids.AROMATICS, crack_frac_aroma),
                new FluidStack(Fluids.UNSATURATEDS, crack_frac_unsat),
                ModOilItems.tar(EnumTarType.CRACK, 1)
        ));
        refinery.put(Fluids.HOTOIL_DS, new RefineryRecipe(
                new FluidStack(Fluids.HEAVYOIL, oilds_frac_heavy),
                new FluidStack(Fluids.NAPHTHA_DS, oilds_frac_naph),
                new FluidStack(Fluids.LIGHTOIL_DS, oilds_frac_light),
                new FluidStack(Fluids.UNSATURATEDS, oilds_frac_unsat),
                ModOilItems.tar(EnumTarType.PARAFFIN, 1)
        ));
        refinery.put(Fluids.HOTCRACKOIL_DS, new RefineryRecipe(
                new FluidStack(Fluids.NAPHTHA_DS, crackds_frac_naph),
                new FluidStack(Fluids.LIGHTOIL_DS, crackds_frac_light),
                new FluidStack(Fluids.AROMATICS, crackds_frac_aroma),
                new FluidStack(Fluids.UNSATURATEDS, crackds_frac_unsat),
                ModOilItems.tar(EnumTarType.PARAFFIN, 1)
        ));

        // --- vacuum distiller (old RefineryRecipes.registerVacuum) ---
        vacuum.put(Fluids.OIL, new VacuumRecipe(
                new FluidStack(Fluids.HEAVYOIL_VACUUM, vac_frac_heavy),
                new FluidStack(Fluids.REFORMATE, vac_frac_reform),
                new FluidStack(Fluids.LIGHTOIL_VACUUM, vac_frac_light),
                new FluidStack(Fluids.SOURGAS, vac_frac_sour)
        ));
        vacuum.put(Fluids.OIL_DS, new VacuumRecipe(
                new FluidStack(Fluids.HEAVYOIL_VACUUM, vac_frac_heavy),
                new FluidStack(Fluids.REFORMATE, vac_frac_reform),
                new FluidStack(Fluids.LIGHTOIL_VACUUM, vac_frac_light),
                new FluidStack(Fluids.REFORMGAS, vac_frac_sour)
        ));
        dev.golem.core.content.Replay.apply(RefineryRecipes.class);
    }

    public static void putRefinery(FluidType oil, RefineryRecipe recipe) {
        init();
        refinery.put(oil, recipe);
    }

    public static boolean removeRefinery(FluidType oil) {
        init();
        return refinery.remove(oil) != null;
    }

    public static void putVacuum(FluidType oil, VacuumRecipe recipe) {
        init();
        vacuum.put(oil, recipe);
    }

    public static boolean removeVacuum(FluidType oil) {
        init();
        return vacuum.remove(oil) != null;
    }

    public static synchronized void reset() {
        refinery.clear();
        vacuum.clear();
        initialized = false;
    }
}
