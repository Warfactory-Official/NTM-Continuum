package com.hbm.inventory.recipes;

import com.hbm.Constants;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.recipes.loader.DataGenericRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.ItemStack;

/**
 * Neutron irradiation (1.7 {@code OutgasserRecipes}): the RBMK outgasser and the fusion breeder's item mode. The
 * datapack table {@code hbm:outgasser}, one file per row in {@code data/hbm/recipe/outgasser/}, converted once from the
 * former Java table and the source now; a row may add {@code "fusion_only": true} (ntm-next's field).
 */
public final class OutgasserRecipes extends DataGenericRecipes<OutgasserRecipes.OutgasserRecipe> {
    public static final OutgasserRecipes INSTANCE = new OutgasserRecipes();
    /** 1.7's fixed processing time; the rows state it as their duration. */
    public static final int DURATION = 10_000;

    private OutgasserRecipes() {
        super(Constants.MOD_ID, "outgasser", OutgasserRecipe::codec);
    }

    /** Declares the table at mod construction (its recipe type and serializer register with HBM's content). */
    public static void declare() {}

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

        /** The row codec of the outgasser table: the generic fields plus {@code fusion_only} (ntm-next's name). */
        public static MapCodec<OutgasserRecipe> codec() {
            return GenericRecipeCodec.codec(OutgasserRecipe::new, Codec.BOOL.optionalFieldOf("fusion_only", false),
                    recipe -> recipe.fusionOnly, (recipe, only) -> recipe.fusionOnly = only);
        }

        @Override
        public String describeExtra() {
            return fusionOnly ? "fusionOnly" : "";
        }

        public FluidStack fluidOutput() { return outputFluid != null && outputFluid.length != 0 ? outputFluid[0] : null; }
        public ItemStack solidOutput() { return outputItem != null && outputItem.length != 0 ? outputItem[0].copy() : ItemStack.EMPTY; }
    }
}
