package com.hbm.inventory.recipes.material;

import com.hbm.inventory.OreDictManager;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.PyroOvenRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.material.FuelAdditiveItems;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Exact recipe handoff for the two flattened fuel-additive variants. */
public final class FuelAdditiveRecipes {

    private FuelAdditiveRecipes() {}

    /** The two old ChemicalPlantRecipes entries, retained until I0 wires them into the shared registry. */
    public static List<GenericRecipe> chemicalPlantRecipes() {
        AStack tar = new TagStack(PyroOvenRecipes.TARS);
        AStack leadDust = OreDictManager.PB.dust();
        return List.of(
                new GenericRecipe("chem.tel").setup(40, 100)
                        .inputItems(tar, leadDust)
                        .inputFluids(new FluidStack(Fluids.PETROLEUM, 100), new FluidStack(Fluids.STEAM, 1_000))
                        .outputItems(FuelAdditiveItems.stack(FuelAdditiveItems.Variant.ANTIKNOCK)),
                new GenericRecipe("chem.deicer").setup(40, 100)
                        .inputFluids(new FluidStack(Fluids.GAS, 100), new FluidStack(Fluids.HYDROGEN, 50))
                        .outputItems(FuelAdditiveItems.stack(FuelAdditiveItems.Variant.DEICER)));
    }

    /** I0 can call this from ChemicalPlantRecipes without duplicating the reviewed recipe constants. */
    public static void registerChemicalPlantRecipes(GenericRecipes target) {
        for (GenericRecipe recipe : chemicalPlantRecipes()) target.registerGenerated(recipe);
    }

    /** The three old MixerRecipes entries. The solid is always legacy metadata variant 0. */
    public static List<MixerRecipeSpec> mixerRecipes() {
        return List.of(
                new MixerRecipeSpec(Fluids.PETROIL_LEADED, Fluids.PETROIL, FuelAdditiveItems.Variant.ANTIKNOCK),
                new MixerRecipeSpec(Fluids.GASOLINE_LEADED, Fluids.GASOLINE, FuelAdditiveItems.Variant.ANTIKNOCK),
                new MixerRecipeSpec(Fluids.COALGAS_LEADED, Fluids.COALGAS, FuelAdditiveItems.Variant.ANTIKNOCK));
    }

    public record MixerRecipeSpec(FluidType output, FluidType input, FuelAdditiveItems.Variant additive) {
        public int outputAmount() { return 12_000; }
        public int inputAmount() { return 10_000; }
        public int processTime() { return 40; }
        public ItemStack solid() { return FuelAdditiveItems.stack(additive); }
    }
}
