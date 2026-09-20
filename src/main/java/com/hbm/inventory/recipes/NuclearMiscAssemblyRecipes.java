package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModItems;
import net.minecraft.world.item.ItemStack;

import static com.hbm.inventory.OreDictManager.*;
import com.hbm.blocks.ModNuclearMiscBlocks;

/** The 1.7 {@code AssemblyMachineRecipes} entry for the radiolysis chamber, kept with the feature. */
public final class NuclearMiscAssemblyRecipes {

    private NuclearMiscAssemblyRecipes() {}

    public static void registerAll(GenericRecipes recipes) {
        if (recipes.getRecipe("ass.radiolysis") != null) return;
        // The port has no inputItemsEx alternative list; the primary 1.7 inputs are used.
        recipes.registerGenerated(new GenericRecipe("ass.radiolysis").setup(200, 100)
                .outputItems(new ItemStack(ModNuclearMiscBlocks.MACHINE_RADIOLYSIS.get(), 1))
                .inputItems(STEEL.shell().count(4), ANY_RESISTANTALLOY.ingot().count(4), PB.plate().count(12),
                        CU.plateCast().count(4), RUBBER.ingot().count(8), new ComparableStack(ModItems.thermo_element.get(), 8)));
    }
}
