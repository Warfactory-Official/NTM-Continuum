package com.hbm.recipes.tables;

import static com.hbm.inventory.fluid.Fluids.*;
import static com.hbm.inventory.recipes.SolidificationRecipes.*;

import com.hbm.blocks.ModFallingBlocks;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.SolidificationRecipes;
import com.hbm.inventory.recipes.SolidificationRecipes.Row;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.hbm.items.ItemEnums.EnumTarType;
import com.hbm.items.ModFoodItems;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModOilItems;
import dev.golem.core.world.RecipeSource;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * 1.7 {@code SolidificationRecipes.registerDefaults}. Rows go into a map keyed by fluid as in 1.7, so a later line
 * replaces an earlier one (BALEFIRE); a {@code registerSFAuto} row is written with its TU per item
 * ({@code solid_fuel_energy}) and {@code SolidificationRecipes} computes the amount from the fluid when the table loads.
 * 1.7's {@code ModItems.ingot_mercury} is the item registered as {@code nugget_mercury}.
 */
public final class SolidificationTable extends RecipeSource<Row> {

    private final Map<FluidType, Row> recipes = new LinkedHashMap<>();

    public SolidificationTable() {
        super(SolidificationRecipes.TABLE.table.table);
    }

    @Override
    protected void registerDefaults() {

        registerRecipe(WATER,       1000,           Blocks.ICE);
        registerRecipe(LAVA,        1000,           Blocks.OBSIDIAN);
        registerRecipe(MERCURY,     125,            ModItems.nugget_mercury.get());
        registerRecipe(BIOGAS,      250,            new ItemStack(ModFuelItems.biomass_compressed.get(), 4));
        registerRecipe(SALIENT,     1280,           new ItemStack(ModFoodItems.bio_wafer.get(), 8)); //4 (food val) * 2 (sat mod) * 2 (constant) * 10 (quanta) * 8 (batch size)
        registerRecipe(ENDERJUICE,  100,            Items.ENDER_PEARL);
        registerRecipe(WATZ,        1000,           ModItems.ingot_mud.get());
        registerRecipe(REDMUD,      450,            Items.IRON_INGOT);
        registerRecipe(SODIUM,      100,            ModItems.powder_sodium.get());
        registerRecipe(LEAD,        100,            ModItems.ingot_lead.get());
        registerRecipe(SLOP,        250,            ModFallingBlocks.ore_oil_sand.get());

        registerRecipe(OIL,             SF_OIL,         ModOilItems.tar(EnumTarType.CRUDE, 1));
        registerRecipe(CRACKOIL,        SF_CRACK,       ModOilItems.tar(EnumTarType.CRACK, 1));
        registerRecipe(COALOIL,         SF_COALOIL,     ModOilItems.tar(EnumTarType.COAL, 1));
        registerRecipe(HEAVYOIL,        SF_HEAVY,       ModOilItems.tar(EnumTarType.CRUDE, 1));
        registerRecipe(HEAVYOIL_VACUUM, SF_HEAVY,       ModOilItems.tar(EnumTarType.CRUDE, 1));
        registerRecipe(BITUMEN,         SF_BITUMEN,     ModOilItems.tar(EnumTarType.CRUDE, 1));
        registerRecipe(COALCREOSOTE,    SF_CREOSOTE,    ModOilItems.tar(EnumTarType.COAL, 1));
        registerRecipe(WOODOIL,         SF_WOOD,        ModOilItems.tar(EnumTarType.WOOD, 1));
        registerRecipe(LUBRICANT,       SF_LUBE,        ModOilItems.tar(EnumTarType.PARAFFIN, 1));

        registerRecipe(BALEFIRE,        250,            ModItems.solid_fuel_bf.get());

        registerSFAuto(SMEAR);
        registerSFAuto(HEATINGOIL);
        registerSFAuto(HEATINGOIL_VACUUM);
        registerSFAuto(RECLAIMED);
        registerSFAuto(PETROIL);
        registerSFAuto(NAPHTHA);
        registerSFAuto(NAPHTHA_CRACK);
        registerSFAuto(DIESEL);
        registerSFAuto(DIESEL_REFORM);
        registerSFAuto(DIESEL_CRACK);
        registerSFAuto(DIESEL_CRACK_REFORM);
        registerSFAuto(LIGHTOIL);
        registerSFAuto(LIGHTOIL_CRACK);
        registerSFAuto(LIGHTOIL_VACUUM);
        registerSFAuto(KEROSENE);
        registerSFAuto(KEROSENE_REFORM);
        //registerSFAuto(GAS);
        registerSFAuto(SOURGAS);
        registerSFAuto(REFORMGAS);
        registerSFAuto(SYNGAS);
        registerSFAuto(PETROLEUM);
        registerSFAuto(LPG);
        //registerSFAuto(BIOGAS);
        registerSFAuto(BIOFUEL);
        registerSFAuto(AROMATICS);
        registerSFAuto(UNSATURATEDS);
        registerSFAuto(REFORMATE);
        registerSFAuto(XYLENE);
        registerSFAuto(BALEFIRE, 24_000_000L, ModItems.solid_fuel_bf.get()); //holy shit this is energy dense

        recipes.forEach((fluid, row) -> register(fluid.getKey(), row));
    }

    private void registerSFAuto(FluidType fluid) {
        registerSFAuto(fluid, 1_440_000L, ModFuelItems.solid_fuel.get()); //3200 burntime * 1.5 burntime bonus * 300 TU/t
    }

    /** 1.7 {@code registerSFAuto}; the mB amount is computed by {@code SolidificationRecipes}. */
    private void registerSFAuto(FluidType fluid, long tuPerSF, Item fuel) {
        recipes.put(fluid, new Row(fluid, Optional.empty(), Optional.of(tuPerSF), ItemOut.of(new ItemStack(fuel))));
    }

    private void registerRecipe(FluidType type, int quantity, Item output) { registerRecipe(type, quantity, new ItemStack(output)); }
    private void registerRecipe(FluidType type, int quantity, Block output) { registerRecipe(type, quantity, new ItemStack(output)); }
    private void registerRecipe(FluidType type, int quantity, ItemStack output) {
        recipes.put(type, new Row(type, Optional.of(quantity), Optional.empty(), ItemOut.of(output)));
    }
}
