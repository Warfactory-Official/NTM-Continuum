package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.inventory.fluid.FluidCombustion.Combustible;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ItemEnums.EnumCokeType;
import com.hbm.items.ModItems;
import com.hbm.items.ModOilItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Coker recipes — 1:1 port of the 1.12.2 {@code CokerRecipes}. Heat-driven (the coker pulls heat from
 * the {@code IHeatSource} below): fluid → (mB required, output ItemStack, optional byproduct FluidStack).
 * The {@code registerAuto}/{@code registerSFAuto} auto-computation replicates the old math verbatim
 * (820_000 TU per petroleum coke, penalty 1, magnitude-stepped round; byproduct = {@code max(10, mB/10)}).
 */
public final class CokerRecipes {

    private CokerRecipes() {}

    /** mB required + output item + optional byproduct fluid (nullable). */
    public record CokerRecipe(int amount, ItemStack output, FluidStack byproduct) {}

    private static final Map<FluidType, CokerRecipe> recipes = new LinkedHashMap<>();
    private static boolean initialized = false;

    public static CokerRecipe getOutput(FluidType type) {
        init();
        return recipes.get(type);
    }

    public static Map<FluidType, CokerRecipe> getAll() {
        init();
        return Collections.unmodifiableMap(recipes);
    }

    private static ItemStack cokePetroleum() {
        return ModOilItems.coke(EnumCokeType.PETROLEUM, 1);
    }

    /** Old registerAuto: 820_000 TU per petroleum-coke unit → coke[PETROLEUM] + a gas byproduct. */
    private static void registerAuto(FluidType fluid, FluidType type) {
        registerSFAuto(fluid, 820_000L, cokePetroleum(), type);
    }

    /** Old registerSFAuto: mB = tuPerSF·1000 / max(flammable, combustible), magnitude-rounded; byproduct max(10, mB/10). */
    private static void registerSFAuto(FluidType fluid, long tuPerSF, ItemStack fuel, FluidType type) {
        long tuFlammable = FluidCombustion.getFlammableEnergy(fluid);
        Combustible comb = FluidCombustion.getCombustible(fluid);
        long tuCombustible = comb == null ? 0L : comb.energyPerBucket();
        long tuPerBucket = Math.max(tuFlammable, tuCombustible);
        if (tuPerBucket <= 0) return; // not fuel in this fork -> no auto recipe (old would divide by 0)

        double penalty = 1;
        int mB = (int) (tuPerSF * 1000L * penalty / tuPerBucket);

        if (mB > 10_000) mB -= (mB % 1000);
        else if (mB > 1_000) mB -= (mB % 100);
        else if (mB > 100) mB -= (mB % 10);

        FluidStack byproduct = type == null ? null : new FluidStack(type, Math.max(10, mB / 10));
        registerRecipe(fluid, mB, fuel, byproduct);
    }

    private static void registerRecipe(FluidType type, int quantity, ItemStack output, FluidStack byproduct) {
        recipes.put(type, new CokerRecipe(quantity, output, byproduct));
    }

    private static synchronized void init() {
        if (initialized) return;
        initialized = true;

        registerAuto(Fluids.HEAVYOIL, Fluids.OIL_COKER);
        registerAuto(Fluids.HEAVYOIL_VACUUM, Fluids.REFORMATE);
        registerAuto(Fluids.COALCREOSOTE, Fluids.NAPHTHA_COKER);
        registerAuto(Fluids.SMEAR, Fluids.OIL_COKER);
        registerAuto(Fluids.HEATINGOIL, Fluids.OIL_COKER);
        registerAuto(Fluids.HEATINGOIL_VACUUM, Fluids.OIL_COKER);
        registerAuto(Fluids.RECLAIMED, Fluids.NAPHTHA_COKER);
        registerAuto(Fluids.NAPHTHA, Fluids.NAPHTHA_COKER);
        registerAuto(Fluids.NAPHTHA_DS, Fluids.NAPHTHA_COKER);
        registerAuto(Fluids.NAPHTHA_CRACK, Fluids.NAPHTHA_COKER);
        registerAuto(Fluids.DIESEL, Fluids.NAPHTHA_COKER);
        registerAuto(Fluids.DIESEL_REFORM, Fluids.NAPHTHA_COKER);
        registerAuto(Fluids.DIESEL_CRACK, Fluids.GAS_COKER);
        registerAuto(Fluids.DIESEL_CRACK_REFORM, Fluids.GAS_COKER);
        registerAuto(Fluids.LIGHTOIL, Fluids.GAS_COKER);
        registerAuto(Fluids.LIGHTOIL_DS, Fluids.GAS_COKER);
        registerAuto(Fluids.LIGHTOIL_CRACK, Fluids.GAS_COKER);
        registerAuto(Fluids.LIGHTOIL_VACUUM, Fluids.GAS_COKER);
        registerAuto(Fluids.BIOFUEL, Fluids.GAS_COKER);
        registerAuto(Fluids.AROMATICS, Fluids.GAS_COKER);
        registerAuto(Fluids.REFORMATE, Fluids.GAS_COKER);
        registerAuto(Fluids.XYLENE, Fluids.GAS_COKER);
        registerAuto(Fluids.FISHOIL, Fluids.MERCURY);
        registerAuto(Fluids.SUNFLOWEROIL, Fluids.GAS_COKER);

        registerSFAuto(Fluids.WOODOIL, 340_000L, new ItemStack(Items.CHARCOAL, 1), Fluids.GAS_COKER);

        registerRecipe(Fluids.WATZ, 4_000, new ItemStack(ModItems.ingot_mud.get(), 4), null);
        registerRecipe(Fluids.REDMUD, 450, new ItemStack(Items.IRON_INGOT, 1), new FluidStack(Fluids.MERCURY, 50));
        registerRecipe(Fluids.BITUMEN, 16_000, cokePetroleum(), new FluidStack(Fluids.OIL_COKER, 1_600));
        registerRecipe(Fluids.LUBRICANT, 12_000, cokePetroleum(), new FluidStack(Fluids.OIL_COKER, 1_200));
        registerRecipe(Fluids.CALCIUM_SOLUTION, 125, new ItemStack(ModItems.powder_calcium.get()), new FluidStack(Fluids.SPENTSTEAM, 100));
        registerRecipe(Fluids.SOURGAS, 1_000, new ItemStack(ModItems.sulfur.get()), new FluidStack(Fluids.GAS_COKER, 150));
        registerRecipe(Fluids.SLOP, 1_000, new ItemStack(ModItems.powder_limestone.get()), new FluidStack(Fluids.COLLOID, 250));
        registerRecipe(Fluids.VITRIOL, 4_000, new ItemStack(ModItems.powder_iron.get()), new FluidStack(Fluids.SULFURIC_ACID, 500));
        dev.golem.core.content.Replay.apply(CokerRecipes.class);
    }

    public static void put(FluidType type, CokerRecipe recipe) {
        init();
        recipes.put(type, recipe);
    }

    public static boolean remove(FluidType type) {
        init();
        return recipes.remove(type) != null;
    }

    public static synchronized void reset() {
        recipes.clear();
        initialized = false;
    }
}
