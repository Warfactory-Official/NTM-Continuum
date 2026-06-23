package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ChemicalPlantRecipes extends GenericRecipes {

    public static final ChemicalPlantRecipes INSTANCE = new ChemicalPlantRecipes();

    @Override
    public void registerRecipes() {
        if (!recipeOrderedList.isEmpty()) return;

        register(new GenericRecipe("chem.oxygen").setup(20, 400)
                .inputFluids(new FluidStack(Fluids.AIR, 8_000))
                .outputFluids(new FluidStack(Fluids.OXYGEN, 500)));
        register(new GenericRecipe("chem.xenon").setup(300, 1_000)
                .inputFluids(new FluidStack(Fluids.AIR, 16_000))
                .outputFluids(new FluidStack(Fluids.XENON, 50)));
        register(new GenericRecipe("chem.co2").setup(60, 100)
                .inputFluids(new FluidStack(Fluids.GAS, 1_000))
                .outputFluids(new FluidStack(Fluids.CARBONDIOXIDE, 1_000)));
        register(new GenericRecipe("chem.gasoline").setup(40, 100)
                .inputFluids(new FluidStack(Fluids.NAPHTHA, 1_000))
                .outputFluids(new FluidStack(Fluids.GASOLINE, 800)));
        register(new GenericRecipe("chem.reoil").setup(40, 100)
                .inputFluids(new FluidStack(Fluids.SMEAR, 1_000))
                .outputFluids(new FluidStack(Fluids.RECLAIMED, 800)));
        register(new GenericRecipe("chem.biofuel").setup(60, 100)
                .inputFluids(new FluidStack(Fluids.BIOGAS, 1_500), new FluidStack(Fluids.ETHANOL, 250))
                .outputFluids(new FluidStack(Fluids.BIOFUEL, 1_000)));
        register(new GenericRecipe("chem.cccentrifuge").setup(200, 100)
                .inputFluids(new FluidStack(Fluids.CHLOROCALCITE_CLEANED, 500), new FluidStack(Fluids.SULFURIC_ACID, 8_000))
                .outputFluids(new FluidStack(Fluids.POTASSIUM_CHLORIDE, 250), new FluidStack(Fluids.CALCIUM_CHLORIDE, 250)));

        register(new GenericRecipe("chem.hydrogen").setup(20, 400)
                .inputItems(new ComparableStack(Items.COAL, 1))
                .inputFluids(new FluidStack(Fluids.WATER, 8_000))
                .outputFluids(new FluidStack(Fluids.HYDROGEN, 500)));
        register(new GenericRecipe("chem.ethanol").setup(50, 100)
                .inputItems(new ComparableStack(Items.SUGAR, 10))
                .outputFluids(new FluidStack(Fluids.ETHANOL, 1_000)));
        register(new GenericRecipe("chem.cobble").setup(20, 100)
                .inputFluids(new FluidStack(Fluids.WATER, 1_000), new FluidStack(Fluids.LAVA, 25))
                .outputItems(new ItemStack(Items.COBBLESTONE)));
    }
}
