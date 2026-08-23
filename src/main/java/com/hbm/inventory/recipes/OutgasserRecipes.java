package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModItems;
import net.minecraft.world.item.ItemStack;

import static com.hbm.inventory.material.Mats.MAT_GOLD;
import static com.hbm.inventory.material.Mats.MAT_LITHIUM;

/** Direct lookup table for neutron-irradiation recipes. */
public final class OutgasserRecipes extends GenericRecipes {
    public static final OutgasserRecipes INSTANCE = new OutgasserRecipes();
    public static final int DURATION = 10_000;
    private OutgasserRecipes() {}
    @Override public void registerRecipes() {
        if (!recipeOrderedList.isEmpty()) return;

        // Current ntm-next's intentionally scoped projection of the legacy irradiation table.
        register(new OutgasserRecipe("outgasser.tritium_block").inputItems(MAT_LITHIUM.block())
                .outputFluids(new FluidStack(Fluids.TRITIUM, 10_000)).setup(DURATION, 0));
        register(new OutgasserRecipe("outgasser.tritium_ingot").inputItems(MAT_LITHIUM.ingot())
                .outputFluids(new FluidStack(Fluids.TRITIUM, 1_000)).setup(DURATION, 0));
        register(new OutgasserRecipe("outgasser.tritium_dust").inputItems(MAT_LITHIUM.dust())
                .outputFluids(new FluidStack(Fluids.TRITIUM, 1_000)).setup(DURATION, 0));
        register(new OutgasserRecipe("outgasser.au198_nugget").inputItems(MAT_GOLD.nugget())
                .outputItems(new ItemStack(ModItems.nugget_au198.get())).setup(DURATION, 0));
        register(new OutgasserRecipe("outgasser.au198_dust").inputItems(MAT_GOLD.dust())
                .outputItems(new ItemStack(ModItems.powder_au198.get())).setup(DURATION, 0));
    }
    public GenericRecipe getRecipe(ItemStack stack) { registerRecipes(); return find(stack, false); }
    public GenericRecipe getRecipeForFusion(ItemStack stack) { registerRecipes(); return find(stack, true); }
    private GenericRecipe find(ItemStack stack, boolean fusion) {
        if (stack.isEmpty()) return null;
        for (GenericRecipe recipe : recipeOrderedList) {
            if (recipe instanceof OutgasserRecipe out && (fusion || !out.fusionOnly)
                    && recipe.inputItem != null && recipe.inputItem.length == 1
                    && recipe.inputItem[0].matchesRecipe(stack, false)) return recipe;
        }
        return null;
    }
    public static final class OutgasserRecipe extends GenericRecipe {
        public boolean fusionOnly;
        public OutgasserRecipe(String name) { super(name); }
        @Override public OutgasserRecipe inputItems(AStack... input) { super.inputItems(input); return this; }
        @Override public OutgasserRecipe outputItems(ItemStack... output) { super.outputItems(output); return this; }
        @Override public OutgasserRecipe outputFluids(FluidStack... output) { super.outputFluids(output); return this; }
        public OutgasserRecipe fusionOnly() { fusionOnly = true; return this; }
        public FluidStack fluidOutput() { return outputFluid != null && outputFluid.length != 0 ? outputFluid[0] : null; }
        public ItemStack solidOutput() { return outputItem != null && outputItem.length != 0 ? outputItem[0].copy() : ItemStack.EMPTY; }
    }
}
