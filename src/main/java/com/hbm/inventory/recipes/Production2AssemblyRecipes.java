package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import static com.hbm.inventory.OreDictManager.*;
import com.hbm.blocks.ModProduction2Blocks;

/**
 * The 1.7 {@code AssemblyMachineRecipes} entries for the group-B production machines. The annihilator,
 * the old boiler and both deprecated RTG furnaces have no obtainment recipe upstream.
 */
public final class Production2AssemblyRecipes {

    private Production2AssemblyRecipes() {}

    public static void registerAll(GenericRecipes recipes) {
        recipes.registerGenerated(new GenericRecipe("ass.excavator").setup(200, 100)
                .outputItems(new ItemStack(ModProduction2Blocks.MACHINE_EXCAVATOR.get(), 1))
                .inputItems(new ComparableStack(Blocks.STONE_BRICKS.asItem(), 8), STEEL.ingot().count(8), IRON.ingot().count(8),
                        new ComparableStack(ModItems.motor.get(), 2),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ANALOG), 1)));
        recipes.registerGenerated(new GenericRecipe("ass.slopper").setup(200, 100)
                .outputItems(new ItemStack(ModProduction2Blocks.MACHINE_ORE_SLOPPER.get(), 1))
                .inputItems(STEEL.plateCast().count(6), TI.plate().count(8), CU.pipe().count(3),
                        new ComparableStack(ModItems.motor.get(), 3),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ANALOG), 1)));
    }
}
