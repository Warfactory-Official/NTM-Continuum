package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.ItemEnums.EnumTarType;
import com.hbm.items.ModOilItems;
import net.minecraft.world.item.ItemStack;

import static com.hbm.inventory.material.Mats.MAT_GOLD;
import static com.hbm.inventory.material.Mats.MAT_LITHIUM;
import static com.hbm.inventory.material.Mats.MAT_THORIUM;
import static com.hbm.inventory.material.Mats.MAT_COAL;
import static com.hbm.inventory.material.MaterialShapes.DUSTTINY;

/** Direct lookup table for neutron-irradiation recipes. */
public final class OutgasserRecipes extends GenericRecipes {
    public static final OutgasserRecipes INSTANCE = new OutgasserRecipes();
    public static final int DURATION = 10_000;
    private OutgasserRecipes() {}
    @Override public void registerRecipes() {
        if (!recipeOrderedList.isEmpty()) return;

        // Lithium to tritium.
        register(new OutgasserRecipe("outgasser.tritium_block").inputItems(MAT_LITHIUM.block())
                .outputFluids(new FluidStack(Fluids.TRITIUM, 10_000)).setup(DURATION, 0));
        register(new OutgasserRecipe("outgasser.tritium_ingot").inputItems(MAT_LITHIUM.ingot())
                .outputFluids(new FluidStack(Fluids.TRITIUM, 1_000)).setup(DURATION, 0));
        register(new OutgasserRecipe("outgasser.tritium_dust").inputItems(MAT_LITHIUM.dust())
                .outputFluids(new FluidStack(Fluids.TRITIUM, 1_000)).setup(DURATION, 0));
        register(new OutgasserRecipe("outgasser.tritium_dust_tiny")
                .inputItems(new TagStack(MAT_LITHIUM.tag(DUSTTINY)))
                .outputFluids(new FluidStack(Fluids.TRITIUM, 100)).setup(DURATION, 0));

        // Gold to gold-198.
        register(new OutgasserRecipe("outgasser.au198_ingot").inputItems(MAT_GOLD.ingot())
                .outputItems(new ItemStack(ModItems.ingot_au198.get())).setup(DURATION, 0));
        register(new OutgasserRecipe("outgasser.au198_nugget").inputItems(MAT_GOLD.nugget())
                .outputItems(new ItemStack(ModItems.nugget_au198.get())).setup(DURATION, 0));
        register(new OutgasserRecipe("outgasser.au198_dust").inputItems(MAT_GOLD.dust())
                .outputItems(new ItemStack(ModItems.powder_au198.get())).setup(DURATION, 0));

        // Thorium to thorium fuel.
        register(new OutgasserRecipe("outgasser.thorium_fuel_ingot").inputItems(MAT_THORIUM.ingot())
                .outputItems(new ItemStack(ModItems.ingot_thorium_fuel.get())).setup(DURATION, 0));
        register(new OutgasserRecipe("outgasser.thorium_fuel_nugget").inputItems(MAT_THORIUM.nugget())
                .outputItems(new ItemStack(ModItems.nugget_thorium_fuel.get())).setup(DURATION, 0));
        register(new OutgasserRecipe("outgasser.thorium_fuel_billet").inputItems(MAT_THORIUM.billet())
                .outputItems(new ItemStack(ModItems.billet_thorium_fuel.get())).setup(DURATION, 0));

        // 1.7 OutgasserRecipes:51-53, mushrooms to glowing mushrooms.
        register(new OutgasserRecipe("outgasser.mush_brown").inputItems(
                        new com.hbm.inventory.RecipesCommon.ComparableStack(net.minecraft.world.item.Items.BROWN_MUSHROOM))
                .outputItems(new ItemStack(com.hbm.blocks.ModTerrainBlocks.mush.get())).setup(DURATION, 0));
        register(new OutgasserRecipe("outgasser.mush_red").inputItems(
                        new com.hbm.inventory.RecipesCommon.ComparableStack(net.minecraft.world.item.Items.RED_MUSHROOM))
                .outputItems(new ItemStack(com.hbm.blocks.ModTerrainBlocks.mush.get())).setup(DURATION, 0));
        register(new OutgasserRecipe("outgasser.glowing_stew").inputItems(
                        new com.hbm.inventory.RecipesCommon.ComparableStack(net.minecraft.world.item.Items.MUSHROOM_STEW))
                .outputItems(new ItemStack(com.hbm.items.ModFinalConsumableItems.glowing_stew.get())).setup(DURATION, 0));
        // Coal to coal tar plus syngas.
        register(new OutgasserRecipe("outgasser.coal_gem").inputItems(MAT_COAL.gem())
                .outputItems(ModOilItems.tar(EnumTarType.COAL, 1))
                .outputFluids(new FluidStack(Fluids.SYNGAS, 50)).setup(DURATION, 0));
        register(new OutgasserRecipe("outgasser.coal_dust").inputItems(MAT_COAL.dust())
                .outputItems(ModOilItems.tar(EnumTarType.COAL, 1))
                .outputFluids(new FluidStack(Fluids.SYNGAS, 50)).setup(DURATION, 0));
        register(new OutgasserRecipe("outgasser.coal_block").inputItems(MAT_COAL.block())
                .outputItems(ModOilItems.tar(EnumTarType.COAL, 9))
                .outputFluids(new FluidStack(Fluids.SYNGAS, 500)).setup(DURATION, 0));
        // 1.7 OutgasserRecipes:59, PVC to C4 plus colloid.
        register(new OutgasserRecipe("outgasser.pvc").inputItems(com.hbm.inventory.OreDictManager.PVC.ingot())
                .outputItems(new ItemStack(com.hbm.items.ModFuelItems.ingot_c4.get()))
                .outputFluids(new FluidStack(Fluids.COLLOID, 250)).setup(DURATION, 0));
        register(new OutgasserRecipe("outgasser.coal_tar").inputItems(
                        new com.hbm.inventory.RecipesCommon.ComparableStack(ModOilItems.tar(EnumTarType.COAL)))
                .outputFluids(new FluidStack(Fluids.COALOIL, 100)).setup(DURATION, 0));
        register(new OutgasserRecipe("outgasser.wax_tar").inputItems(
                        new com.hbm.inventory.RecipesCommon.ComparableStack(ModOilItems.tar(EnumTarType.WAX)))
                .outputFluids(new FluidStack(Fluids.RADIOSOLVENT, 100)).setup(DURATION, 0));
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
