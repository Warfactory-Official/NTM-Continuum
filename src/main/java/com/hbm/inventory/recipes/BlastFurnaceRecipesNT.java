package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModItems;
import com.hbm.blocks.ModOreBlocks;
import com.hbm.items.machine.ItemScraps;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static com.hbm.inventory.OreDictManager.AL;
import static com.hbm.inventory.OreDictManager.CO;
import static com.hbm.inventory.OreDictManager.CU;
import static com.hbm.inventory.OreDictManager.GOLD;
import static com.hbm.inventory.OreDictManager.IRON;

/** The feasible default recipes from HBM's 1.12.2 {@code BlastFurnaceRecipesNT}. */
public final class BlastFurnaceRecipesNT extends GenericRecipes {

    public static final BlastFurnaceRecipesNT INSTANCE = new BlastFurnaceRecipesNT();

    private BlastFurnaceRecipesNT() {}

    @Override
    public void registerRecipes() {
        if (!recipeOrderedList.isEmpty()) return;

        // 1.7 new ItemStack(ModItems.ingot_raw, n, MAT_SLAG.id): the autogen slag ingot (hbm:ingot_raw_slag).
        ItemStack slag = slag(1);
        ItemStack slagTwo = slag(2);

        register(new GenericRecipe("blast.steelFromIngot").setup(800, 0)
                .inputItems(IRON.ingot().count(2), new TagStack(ItemTags.SAND))
                .outputItems(new ItemStack(ModItems.ingot_steel.get(), 2), slag.copy()));
        register(new GenericRecipe("blast.steelFromDust").setup(800, 0)
                .inputItems(IRON.dust().count(2), new TagStack(ItemTags.SAND))
                .outputItems(new ItemStack(ModItems.ingot_steel.get(), 2), slag.copy()));
        register(new GenericRecipe("blast.steelFromOre").setup(800, 0)
                .inputItems(IRON.ore(), new TagStack(ItemTags.SAND))
                .outputItems(new ItemStack(ModItems.ingot_steel.get(), 2), slagTwo.copy()));
        register(new GenericRecipe("blast.steelWithFlux").setup(1_200, 0)
                .inputItems(IRON.ore(), new ComparableStack(ModItems.powder_flux.get()))
                .outputItems(new ItemStack(ModItems.ingot_steel.get(), 3), slagTwo.copy()));

        register(new GenericRecipe("blast.mingrade").setup(400, 0)
                .inputItems(CU.ingot(), Mats.MAT_REDSTONE.dust())
                .outputItems(new ItemStack(ModItems.ingot_red_copper.get(), 2)));
        register(new GenericRecipe("blast.mingradeDust").setup(400, 0)
                .inputItems(CU.dust(), Mats.MAT_REDSTONE.dust())
                .outputItems(new ItemStack(ModItems.ingot_red_copper.get(), 2)));
        register(new GenericRecipe("blast.mingradeIngot").setup(400, 0)
                .inputItems(CU.ingot(), Mats.MAT_REDSTONE.ingot()) // 1.7 REDSTONE.ingot() = ingot_raw[redstone]
                .outputItems(new ItemStack(ModItems.ingot_red_copper.get(), 2)));
        register(new GenericRecipe("blast.mingradeCursed").setup(400, 0)
                .inputItems(CU.dust(), Mats.MAT_REDSTONE.ingot())
                .outputItems(new ItemStack(ModItems.ingot_red_copper.get(), 2)));
        register(new GenericRecipe("blast.mingradeOre").setup(1_200, 0)
                .inputItems(CU.ore(), Mats.MAT_REDSTONE.dust().count(6))
                .outputItems(new ItemStack(ModItems.ingot_red_copper.get(), 6), slag.copy()));

        register(new GenericRecipe("blast.paa").setup(600, 0)
                .inputItems(GOLD.ingot(), new ComparableStack(ModItems.plate_mixed.get()))
                .outputItems(new ItemStack(ModItems.plate_paa.get())));

        register(new GenericRecipe("blast.firebrick").setup(800, 0)
                .inputItems(AL.dust(), new ComparableStack(Items.CLAY_BALL, 7))
                .outputItems(new ItemStack(ModItems.ingot_firebrick.get(), 8)));
        register(new GenericRecipe("blast.firebrickLimestone").setup(800, 0)
                .inputItems(new ComparableStack(ModOreBlocks.stone_resource_limestone.get().asItem()),
                        new ComparableStack(Items.CLAY_BALL, 6))
                .outputItems(new ItemStack(ModItems.ingot_firebrick.get(), 8)));
        AbilityToolMachineRecipes.blastFurnace(this); // meteor sword, meteorite ingot, starmetal
    }

    /** Matches either furnace ingredient order, as the old two-slot machine did. */
    public GenericRecipe getRecipe(ItemStack first, ItemStack second) {
        for (GenericRecipe recipe : recipeOrderedList) {
            if (recipe.inputItem == null || recipe.inputItem.length != 2) continue;
            if (recipe.inputItem[0].matchesRecipe(first, false)
                    && recipe.inputItem[1].matchesRecipe(second, false)) return recipe;
            if (recipe.inputItem[1].matchesRecipe(first, false)
                    && recipe.inputItem[0].matchesRecipe(second, false)) return recipe;
        }
        return null;
    }

    private static ItemStack slag(int count) {
        return new ItemStack(com.hbm.items.ModAutogenItems.get(MaterialShapes.INGOT, Mats.MAT_SLAG), count);
    }
}
