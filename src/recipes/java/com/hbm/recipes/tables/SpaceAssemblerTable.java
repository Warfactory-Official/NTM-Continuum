package com.hbm.recipes.tables;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.SpaceAssemblerRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericTableSource;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModSatelliteItems;
import com.hbm.items.machine.ItemOrbitalAssembly.EnumOrbitalAssembly;
import net.minecraft.world.item.ItemStack;

/** 1.7 {@code SpaceAssemblerRecipes.registerDefaults}. */
public final class SpaceAssemblerTable extends GenericTableSource<GenericRecipe> {

    public SpaceAssemblerTable() {
        super(SpaceAssemblerRecipes.INSTANCE);
    }

    @Override
    protected void registerDefaults() {

        int minute = 60 * 20;

        this.register(new GenericRecipe("space.crystalcircuit").setup(3 * minute, 0L)
                .inputItems(new ComparableStack(ModSatelliteItems.orbital_assembly.get(EnumOrbitalAssembly.CRYSTAL_CIRCUIT), 1))
                .outputItems(new ItemStack(ModIntermediateItems.circuit(EnumCircuitType.CRYSTAL))));
    }
}
