package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ItemEnums.EnumTarType;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModOilItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Solidifier recipes — fluid → (mB required, output ItemStack) — 1:1 port of the 1.12.2
 * {@code SolidificationRecipes.registerDefaults}, including the {@code registerSFAuto} "solid fuel"
 * auto-computation (mB = {@code SF_energy·1000·1.25 / FT_Flammable_energy}, magnitude-rounded, min 1).
 *
 * <p>Every 1.7 entry is registered. 1.7's {@code ModItems.ingot_mercury} is registered under the name
 * {@code nugget_mercury} (the field {@code nugget_mercury} is {@code nugget_mercury_tiny}), so MERCURY
 * solidifies into {@code hbm:nugget_mercury}.
 */
public final class SolidificationRecipes {

    private SolidificationRecipes() {}

    public static final int SF_OIL = 200;
    public static final int SF_CRACK = 200;
    public static final int SF_HEAVY = 150;
    public static final int SF_BITUMEN = 100;
    public static final int SF_COALOIL = 200;
    public static final int SF_CREOSOTE = 200;
    public static final int SF_WOOD = 1000;
    public static final int SF_LUBE = 100;

    public record SolidRecipe(int amount, ItemStack output) {}

    private static final Map<FluidType, SolidRecipe> recipes = new LinkedHashMap<>();
    private static boolean initialized = false;

    public static SolidRecipe getOutput(FluidType type) {
        init();
        return recipes.get(type);
    }

    /** Read-only view for JEI display. */
    public static Map<FluidType, SolidRecipe> getAll() {
        init();
        return Collections.unmodifiableMap(recipes);
    }

    private static void reg(FluidType type, int quantity, Item output) {
        if (output != null && output != Items.AIR) recipes.put(type, new SolidRecipe(quantity, new ItemStack(output)));
    }

    private static void reg(FluidType type, int quantity, ItemStack output) {
        recipes.put(type, new SolidRecipe(quantity, output));
    }

    private static synchronized void init() {
        if (initialized) return;
        initialized = true;

        reg(Fluids.WATER, 1000, Items.ICE.asItem());
        reg(Fluids.LAVA, 1000, Items.OBSIDIAN.asItem());
        reg(Fluids.MERCURY, 125, ModItems.nugget_mercury.get()); // 1.7 ingot_mercury
        reg(Fluids.BIOGAS, 250, new ItemStack(ModFuelItems.biomass_compressed.get(), 4));
        // 4 (food val) * 2 (sat mod) * 2 (constant) * 10 (quanta) * 8 (batch size)
        reg(Fluids.SALIENT, 1280, new ItemStack(com.hbm.items.ModFoodItems.bio_wafer.get(), 8));
        reg(Fluids.ENDERJUICE, 100, Items.ENDER_PEARL);
        reg(Fluids.WATZ, 1000, ModItems.ingot_mud.get());
        reg(Fluids.REDMUD, 450, Items.IRON_INGOT);
        reg(Fluids.SODIUM, 100, ModItems.powder_sodium.get());
        reg(Fluids.LEAD, 100, ModItems.ingot_lead.get());
        reg(Fluids.SLOP, 250, com.hbm.blocks.ModFallingBlocks.ore_oil_sand.get().asItem());

        reg(Fluids.OIL, SF_OIL, ModOilItems.tar(EnumTarType.CRUDE, 1));
        reg(Fluids.CRACKOIL, SF_CRACK, ModOilItems.tar(EnumTarType.CRACK, 1));
        reg(Fluids.COALOIL, SF_COALOIL, ModOilItems.tar(EnumTarType.COAL, 1));
        reg(Fluids.HEAVYOIL, SF_HEAVY, ModOilItems.tar(EnumTarType.CRUDE, 1));
        reg(Fluids.HEAVYOIL_VACUUM, SF_HEAVY, ModOilItems.tar(EnumTarType.CRUDE, 1));
        reg(Fluids.BITUMEN, SF_BITUMEN, ModOilItems.tar(EnumTarType.CRUDE, 1));
        reg(Fluids.COALCREOSOTE, SF_CREOSOTE, ModOilItems.tar(EnumTarType.COAL, 1));
        reg(Fluids.WOODOIL, SF_WOOD, ModOilItems.tar(EnumTarType.WOOD, 1));
        reg(Fluids.LUBRICANT, SF_LUBE, ModOilItems.tar(EnumTarType.PARAFFIN, 1));

        reg(Fluids.BALEFIRE, 250, ModItems.solid_fuel_bf.get());

        registerSFAuto(Fluids.SMEAR);
        registerSFAuto(Fluids.HEATINGOIL);
        registerSFAuto(Fluids.HEATINGOIL_VACUUM);
        registerSFAuto(Fluids.RECLAIMED);
        registerSFAuto(Fluids.PETROIL);
        registerSFAuto(Fluids.NAPHTHA);
        registerSFAuto(Fluids.NAPHTHA_CRACK);
        registerSFAuto(Fluids.DIESEL);
        registerSFAuto(Fluids.DIESEL_REFORM);
        registerSFAuto(Fluids.DIESEL_CRACK);
        registerSFAuto(Fluids.DIESEL_CRACK_REFORM);
        registerSFAuto(Fluids.LIGHTOIL);
        registerSFAuto(Fluids.LIGHTOIL_CRACK);
        registerSFAuto(Fluids.LIGHTOIL_VACUUM);
        registerSFAuto(Fluids.KEROSENE);
        registerSFAuto(Fluids.KEROSENE_REFORM);
        registerSFAuto(Fluids.SOURGAS);
        registerSFAuto(Fluids.REFORMGAS);
        registerSFAuto(Fluids.SYNGAS);
        registerSFAuto(Fluids.PETROLEUM);
        registerSFAuto(Fluids.LPG);
        registerSFAuto(Fluids.BIOFUEL);
        registerSFAuto(Fluids.AROMATICS);
        registerSFAuto(Fluids.UNSATURATEDS);
        registerSFAuto(Fluids.REFORMATE);
        registerSFAuto(Fluids.XYLENE);
        registerSFAuto(Fluids.BALEFIRE, 24_000_000L, ModItems.solid_fuel_bf.get());
    }

    private static void registerSFAuto(FluidType fluid) {
        registerSFAuto(fluid, 1_440_000L, ModFuelItems.solid_fuel.get()); // 3200 burntime * 1.5 bonus * 300 TU/t
    }

    /** Old {@code registerSFAuto}: mB = tuPerSF·1000·1.25 / flammableEnergy, magnitude-rounded, min 1. */
    private static void registerSFAuto(FluidType fluid, long tuPerSF, Item fuel) {
        long tuPerBucket = FluidCombustion.getFlammableEnergy(fluid);
        if (tuPerBucket <= 0) return; // not flammable in this fork -> no SF recipe (old would NPE; guarded)
        double penalty = 1.25D;

        int mB = (int) (tuPerSF * 1000L * penalty / tuPerBucket);

        if (mB > 10_000) mB -= (mB % 1000);
        else if (mB > 1_000) mB -= (mB % 100);
        else if (mB > 100) mB -= (mB % 10);

        mB = Math.max(mB, 1);

        reg(fluid, mB, fuel);
    }
}
