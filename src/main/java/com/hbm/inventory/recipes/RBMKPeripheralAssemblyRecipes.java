package com.hbm.inventory.recipes;

import com.hbm.blocks.machine.rbmk.ModRBMKBlocks;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ItemEnums.EnumExpensiveType;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import net.minecraft.world.item.ItemStack;

import static com.hbm.inventory.OreDictManager.*;

/** The 1.7 {@code AssemblyMachineRecipes} entry for the RBMK autoloader, kept with the feature. */
public final class RBMKPeripheralAssemblyRecipes {

    private RBMKPeripheralAssemblyRecipes() {}

    public static void registerAll(GenericRecipes recipes) {
        if (recipes.getRecipe("ass.rbmkautoloader") != null) return;
        // 1.7 AssemblyMachineRecipes:521-524: the expensive-mode list and the 528 "ferrouranium" pool are applied
        // only when their GeneralConfig switches are on, exactly as 1.7's inputItemsEx / setPools528.
        recipes.registerGenerated(new GenericRecipe("ass.rbmkautoloader").setup(100, 100)
                .outputItems(new ItemStack(ModRBMKBlocks.rbmk_autoloader.get(), 1))
                .inputItems(STEEL.plateWelded().count(4), PB.plateCast().count(4), B.ingot().count(4),
                        new ComparableStack(ModItems.motor.get(), 3))
                .inputItemsEx(new ComparableStack(ModIntermediateItems.expensive(EnumExpensiveType.FERRO_PLATING), 1),
                        new ComparableStack(ModItems.motor_desh.get(), 3))
                .setPools528(GenericRecipes.POOL_PREFIX_528 + "ferrouranium"));
    }
}
