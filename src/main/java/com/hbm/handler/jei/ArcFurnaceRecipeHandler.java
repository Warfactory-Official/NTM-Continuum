package com.hbm.handler.jei;

//? if jei {

import com.hbm.foundry.ArcFurnaceRecipes;
import com.hbm.api.material.MaterialStack;
import com.hbm.api.material.SmeltingBehavior;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.machine.ItemScraps;
import dev.golem.core.jei.JeiRecipeType;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** JEI's two old universal categories for the Large Arc Furnace. */
public final class ArcFurnaceRecipeHandler extends JEIUniversalHandler<ArcFurnaceRecipes.ArcFurnaceRecipe> {

    private final boolean liquid;

    public ArcFurnaceRecipeHandler(IGuiHelper helper,
                                   JeiRecipeType<ArcFurnaceRecipes.ArcFurnaceRecipe> type,
                                   Component title, boolean liquid, ItemStack... machines) {
        super(helper, type, title, machines);
        this.liquid = liquid;
    }

    public static List<ArcFurnaceRecipes.ArcFurnaceRecipe> buildRecipes(boolean liquid) {
        List<ArcFurnaceRecipes.ArcFurnaceRecipe> recipes = new ArrayList<>(
                liquid ? ArcFurnaceRecipes.liquidRecipes() : ArcFurnaceRecipes.solidRecipes());

        // The old JEI map also exposed the special scrap -> molten-material lookup.
        if (liquid) {
            for (NTMMaterial material : Mats.orderedList) {
                if (material.smeltable != SmeltingBehavior.SMELTABLE) continue;
                ItemStack scrap = ItemScraps.create(
                        new MaterialStack(material, MaterialShapes.INGOT.quantity));
                ArcFurnaceRecipes.ArcFurnaceRecipe recipe = ArcFurnaceRecipes.getOutput(scrap, true);
                if (recipe != null) recipes.add(recipe);
            }
        }
        return recipes;
    }

    @Override
    protected List<List<ItemStack>> inputs(ArcFurnaceRecipes.ArcFurnaceRecipe recipe) {
        return List.of(recipe.input.getDisplayStacks());
    }

    @Override
    protected List<List<ItemStack>> outputs(ArcFurnaceRecipes.ArcFurnaceRecipe recipe) {
        List<List<ItemStack>> outputs = new ArrayList<>();
        if (!liquid) {
            if (recipe.solidOutput != null && !recipe.solidOutput.isEmpty()) {
                outputs.add(List.of(recipe.solidOutput.copy()));
            }
        } else {
            for (MaterialStack material : recipe.fluidOutput) {
                ItemStack scrap = ItemScraps.create(material, true);
                if (!scrap.isEmpty()) outputs.add(List.of(scrap));
            }
        }
        return outputs;
    }

}
//?}
