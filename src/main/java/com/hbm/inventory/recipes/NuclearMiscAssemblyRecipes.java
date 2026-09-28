package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ItemEnums.EnumExpensiveType;
import net.minecraft.world.item.ItemStack;

import static com.hbm.inventory.OreDictManager.*;
import com.hbm.blocks.ModNuclearMiscBlocks;

/** The 1.7 {@code AssemblyMachineRecipes} entry for the radiolysis chamber, kept with the feature. */
public final class NuclearMiscAssemblyRecipes {

    private NuclearMiscAssemblyRecipes() {}

    public static void registerAll(GenericRecipes recipes) {
        if (recipes.getRecipe("ass.radiolysis") != null) return;
        // 1.7 inputItemsEx: the expensive-mode inputs replace the primary ones only in expensive mode.
        recipes.registerGenerated(new GenericRecipe("ass.radiolysis").setup(200, 100)
                .outputItems(new ItemStack(ModNuclearMiscBlocks.MACHINE_RADIOLYSIS.get(), 1))
                .inputItems(STEEL.shell().count(4), ANY_RESISTANTALLOY.ingot().count(4), PB.plate().count(12),
                        CU.plateCast().count(4), RUBBER.ingot().count(8), new ComparableStack(ModItems.thermo_element.get(), 8))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.HEAVY_FRAME), 2), new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.LEAD_PLATING), 4), CU.plateCast().count(4), RUBBER.ingot().count(16), new ComparableStack(ModItems.thermo_element.get(), 8)));
    }
}
