package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemStamp;
import com.hbm.items.machine.ItemStamp.StampType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.hbm.inventory.OreDictManager.*;

public final class PressRecipes {

    private PressRecipes() {}

    private record PressRecipe(StampType type, AStack input, ItemStack output) {}

    private static final List<PressRecipe> RECIPES = new ArrayList<>();
    private static boolean initialized = false;

    public static ItemStack getOutput(ItemStack ingredient, ItemStack stamp) {
        if (!initialized) register();
        if (ingredient == null || ingredient.isEmpty() || stamp == null || stamp.isEmpty()) return ItemStack.EMPTY;
        if (!(stamp.getItem() instanceof ItemStamp itemStamp)) return ItemStack.EMPTY;

        StampType type = itemStamp.getStampType();
        for (PressRecipe recipe : RECIPES) {
            if (recipe.type == type && recipe.input.matchesRecipe(ingredient, true)) {
                return recipe.output;
            }
        }
        return ItemStack.EMPTY;
    }

    public static synchronized void register() {
        if (initialized) return;
        initialized = true;

        plate(IRON, ModItems.plate_iron.get());
        plate(GOLD, ModItems.plate_gold.get());
        plate(TI, ModItems.plate_titanium.get());
        plate(AL, ModItems.plate_aluminium.get());
        plate(STEEL, ModItems.plate_steel.get());
        plate(PB, ModItems.plate_lead.get());
        plate(CU, ModItems.plate_copper.get());
        plate(GUNMETAL, ModItems.plate_gunmetal.get());
        plate(WEAPONSTEEL, ModItems.plate_weaponsteel.get());
        plate(CMB, ModItems.plate_combine_steel.get());
        plate(DURA, ModItems.plate_dura_steel.get());
        plate(BIGMT, ModItems.plate_saturnite.get());
        plate(SA326, ModItems.plate_schrabidium.get());
    }

    private static void plate(NTMMaterial mat, Item plate) {
        makeRecipe(StampType.PLATE, mat.ingot(), new ItemStack(plate));
    }

    public static void makeRecipe(StampType type, AStack in, ItemStack out) {
        RECIPES.add(new PressRecipe(type, in, out));
    }
}
