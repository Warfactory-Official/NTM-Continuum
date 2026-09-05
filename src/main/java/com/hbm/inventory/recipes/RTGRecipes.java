package com.hbm.inventory.recipes;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.ModRtgContent;
import com.hbm.items.machine.ItemRTGPellet;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.hbm.inventory.OreDictManager.ANY_PLASTIC;
import static com.hbm.inventory.OreDictManager.MINGRADE;
import static com.hbm.inventory.OreDictManager.STEEL;

/** Grey RTG construction and the fresh-to-depleted display table used by recipe viewers. */
public final class RTGRecipes {
    private RTGRecipes() {}

    public static void registerAssembly() {
        AssemblyMachineRecipes recipes = AssemblyMachineRecipes.INSTANCE;
        recipes.registerRecipes();
    }

    public static void registerAll(AssemblyMachineRecipes recipes) {
        if (recipes.getRecipe("ass.rtg") != null) return;
        recipes.registerGenerated(new GenericRecipe("ass.rtg").setup(200, 100)
                .inputItems(new ComparableStack(ModItems.rtg_unit.get(), 3), STEEL.plate().count(4),
                        MINGRADE.wire().count(16), ANY_PLASTIC.ingot().count(4))
                .outputItems(new ItemStack(ModMachineBlocks.machine_rtg_grey.get())));
    }

    public static Map<ItemStack, ItemStack> decayPairs() {
        Map<ItemStack, ItemStack> result = new LinkedHashMap<>();
        for (var entry : ModRtgContent.PELLETS.values()) {
            ItemRTGPellet pellet = entry.get();
            result.put(new ItemStack(pellet), new ItemStack(pellet.getDecayItem()));
        }
        return result;
    }
}
