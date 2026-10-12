package com.hbm.recipes.tables;

import static com.hbm.inventory.fluid.Fluids.*;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.CokerRecipes;
import com.hbm.inventory.recipes.CokerRecipes.Row;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.hbm.items.ItemEnums.EnumCokeType;
import com.hbm.items.ModItems;
import com.hbm.items.ModOilItems;
import dev.golem.core.world.RecipeSource;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 1.7 {@code CokerRecipes.registerDefaults}. A {@code registerSFAuto} row is written with its TU per item
 * ({@code solid_fuel_energy}) and byproduct fluid; {@code CokerRecipes} computes the amounts from the fluid's fuel values
 * when the table loads, as 1.7 did at registration.
 */
public final class CokerTable extends RecipeSource<Row> {

    public CokerTable() {
        super(CokerRecipes.TABLE.table.table);
    }

    @Override
    protected void registerDefaults() {

        registerAuto(HEAVYOIL,              OIL_COKER);
        registerAuto(HEAVYOIL_VACUUM,       REFORMATE);
        registerAuto(COALCREOSOTE,          NAPHTHA_COKER);
        registerAuto(SMEAR,                 OIL_COKER);
        registerAuto(HEATINGOIL,            OIL_COKER);
        registerAuto(HEATINGOIL_VACUUM,     OIL_COKER);
        registerAuto(RECLAIMED,             NAPHTHA_COKER);
        registerAuto(NAPHTHA,               NAPHTHA_COKER);
        registerAuto(NAPHTHA_DS,            NAPHTHA_COKER);
        registerAuto(NAPHTHA_CRACK,         NAPHTHA_COKER);
        registerAuto(DIESEL,                NAPHTHA_COKER);
        registerAuto(DIESEL_REFORM,         NAPHTHA_COKER);
        registerAuto(DIESEL_CRACK,          GAS_COKER);
        registerAuto(DIESEL_CRACK_REFORM,   GAS_COKER);
        registerAuto(LIGHTOIL,              GAS_COKER);
        registerAuto(LIGHTOIL_DS,           GAS_COKER);
        registerAuto(LIGHTOIL_CRACK,        GAS_COKER);
        registerAuto(LIGHTOIL_VACUUM,       GAS_COKER);
        registerAuto(BIOFUEL,               GAS_COKER);
        registerAuto(AROMATICS,             GAS_COKER);
        registerAuto(REFORMATE,             GAS_COKER);
        registerAuto(XYLENE,                GAS_COKER);
        registerAuto(FISHOIL,               MERCURY);
        registerAuto(SUNFLOWEROIL,          GAS_COKER);

        registerSFAuto(WOODOIL, 340_000L, new ItemStack(Items.CHARCOAL, 1), GAS_COKER);

        registerRecipe(WATZ, 4_000, new ItemStack(ModItems.ingot_mud.get(), 4), null);
        registerRecipe(REDMUD, 450, new ItemStack(Items.IRON_INGOT, 1), new FluidStack(MERCURY, 50));
        registerRecipe(BITUMEN, 16_000, ModOilItems.coke(EnumCokeType.PETROLEUM, 1), new FluidStack(OIL_COKER, 1_600));
        registerRecipe(LUBRICANT, 12_000, ModOilItems.coke(EnumCokeType.PETROLEUM, 1), new FluidStack(OIL_COKER, 1_200));
        registerRecipe(CALCIUM_SOLUTION, 125, new ItemStack(ModItems.powder_calcium.get()), new FluidStack(SPENTSTEAM, 100));
        //only cokable gas to extract sulfur content
        registerRecipe(SOURGAS, 1_000, new ItemStack(ModItems.sulfur.get()), new FluidStack(GAS_COKER, 150));
        registerRecipe(SLOP, 1000, new ItemStack(ModItems.powder_limestone.get()), new FluidStack(COLLOID, 250));
        registerRecipe(VITRIOL, 4000, new ItemStack(ModItems.powder_iron.get()), new FluidStack(SULFURIC_ACID, 500));
    }

    private void registerAuto(FluidType fluid, FluidType type) {
        registerSFAuto(fluid, 820_000L, ModOilItems.coke(EnumCokeType.PETROLEUM, 1), type); //3200 burntime * 1.25 burntime bonus * 200 TU/t + 20000TU per operation
    }

    /** 1.7 {@code registerSFAuto}; the mB amount and the byproduct's amount are computed by {@code CokerRecipes}. */
    private void registerSFAuto(FluidType fluid, long tuPerSF, ItemStack fuel, FluidType type) {
        register(fluid.getKey(), new Row(fluid, Optional.empty(), Optional.of(tuPerSF), ItemOut.of(fuel), Optional.empty(), Optional.ofNullable(type)));
    }

    private void registerRecipe(FluidType type, int quantity, ItemStack output, FluidStack byproduct) {
        register(type.getKey(), new Row(type, Optional.of(quantity), Optional.empty(), ItemOut.of(output), Optional.ofNullable(byproduct), Optional.empty()));
    }
}
