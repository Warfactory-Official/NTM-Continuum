package com.hbm.inventory.recipes;

import com.hbm.blocks.ModEnergyBedrockBlocks;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilConstructionRecipe;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilOutput;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.OverlayType;
import com.hbm.items.ModItems;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static com.hbm.inventory.OreDictManager.MINGRADE;

/**
 * The machine recipes of the energy extras, handed to their registry from one line there: 1.7 {@code AnvilRecipes}
 * 567-571, a red copper ingot and a polymer plate into 16 boxcable of each size (tier 2), and back.
 */
public final class EnergyBedrockMachineRecipes {

    private EnergyBedrockMachineRecipes() {}

    public static void anvil(List<AnvilConstructionRecipe> recipes) {
        for (int i = 0; i < 5; i++) {
            ItemStack box = new ItemStack(ModEnergyBedrockBlocks.red_cable_box[i].get(), 16);
            recipes.add(new AnvilConstructionRecipe(new AStack[]{MINGRADE.ingot(), new ComparableStack(ModItems.plate_polymer.get())},
                    new AnvilOutput(box)).setTier(2).setOverlay(OverlayType.CONSTRUCTION));
            recipes.add(new AnvilConstructionRecipe(new ComparableStack(box.getItem(), 16), new AnvilOutput[]{
                    new AnvilOutput(new ItemStack(ModItems.ingot_red_copper.get(), 1)),
                    new AnvilOutput(new ItemStack(ModItems.plate_polymer.get()))}).setTier(2));
        }
    }
}
