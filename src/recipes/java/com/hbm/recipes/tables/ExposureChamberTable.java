package com.hbm.recipes.tables;

import static com.hbm.inventory.OreDictManager.PU;
import static com.hbm.inventory.OreDictManager.SBD;
import static com.hbm.inventory.OreDictManager.U;
import static com.hbm.inventory.OreDictManager.U238;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.ExposureChamberRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ItemEnums.EnumExpensiveType;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import com.hbm.inventory.recipes.loader.GenericTableSource;
import com.hbm.tileentity.machine.TileEntityMachineExposureChamber;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code ExposureChamberRecipes.registerDefaults}. 1.7's rows have no names; the port names them
 * {@code exposure.<ingredient>}, and the duration and power are the machine's (1.7 {@code TileEntityMachineExposureChamber}).
 */
public final class ExposureChamberTable extends GenericTableSource<GenericRecipe> {

    public ExposureChamberTable() {
        super(ExposureChamberRecipes.INSTANCE);
    }

    @Override
    protected void registerDefaults() {
        add("exposure.uranium", new ComparableStack(ModItems.particle_higgs.get()), U.ingot(), new ItemStack(ModFuelItems.ingot_schraranium.get()));
        add("exposure.uranium238", new ComparableStack(ModItems.particle_higgs.get()), U238.ingot(), new ItemStack(ModItems.ingot_schrabidium.get()));
        add("exposure.plutonium", new ComparableStack(ModItems.particle_dark.get()), PU.ingot(), new ItemStack(ModItems.ingot_euphemium.get()));

        // 1.7: if(GeneralConfig.enableExpensiveMode) degenerate matter, else schrabidate; the row carries both
        register(recipe("exposure.schrabidate", new ComparableStack(ModItems.particle_sparkticle.get()), SBD.ingot(), new ItemStack(ModItems.ingot_dineutronium.get()))
                .inputItemsEx(new ComparableStack(ModItems.particle_sparkticle.get()), new ComparableStack(expensive(EnumExpensiveType.DEGENERATE_MATTER))));
    }

    /** 1.7 {@code recipes.add(new ExposureChamberRecipe(particle, ingredient, output))}. */
    private void add(String name, AStack particle, AStack ingredient, ItemStack output) {
        register(recipe(name, particle, ingredient, output));
    }

    private static GenericRecipe recipe(String name, AStack particle, AStack ingredient, ItemStack output) {
        return new GenericRecipe(name).setup(TileEntityMachineExposureChamber.PROCESS_TIME_BASE, TileEntityMachineExposureChamber.CONSUMPTION_BASE)
                .inputItems(particle, ingredient).outputItems(output);
    }

    /** 1.7 {@code new ItemStack(ModItems.item_expensive, 1, type)}. */
    private static Item expensive(EnumExpensiveType type) {
        return ModIntermediateItems.expensive(type);
    }
}
