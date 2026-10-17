package com.hbm.handler.jei;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.ElectrolyserMetalRecipes;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.items.machine.ItemScraps;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** 1.7 {@code ElectrolyserMetalHandler}: the crystal and 100 mB of nitric acid, then the molten metals and byproducts. */
public final class ElectrolyserMetalRecipeHandler extends JEIUniversalHandler<ElectrolyserMetalRecipes.ElectrolysisMetalRecipe> {

    public ElectrolyserMetalRecipeHandler(ViewerType<ElectrolyserMetalRecipes.ElectrolysisMetalRecipe> type,
                                          Component title, ItemStack machine) {
        super(type, title, machine);
    }

    public static List<ElectrolyserMetalRecipes.ElectrolysisMetalRecipe> buildRecipes() {
        ElectrolyserMetalRecipes.register();
        return new ArrayList<>(ElectrolyserMetalRecipes.RECIPES);
    }

    @Override
    protected List<List<ItemStack>> inputs(ElectrolyserMetalRecipes.ElectrolysisMetalRecipe recipe) {
        return List.of(recipe.input.getDisplayStacks(),
                List.of(ItemFluidIcon.make(new FluidStack(Fluids.NITRIC_ACID, 100))));
    }

    @Override
    protected List<List<ItemStack>> outputs(ElectrolyserMetalRecipes.ElectrolysisMetalRecipe recipe) {
        List<List<ItemStack>> outputs = new ArrayList<>();
        if (recipe.output1 != null) outputs.add(List.of(ItemScraps.create(recipe.output1, true)));
        if (recipe.output2 != null) outputs.add(List.of(ItemScraps.create(recipe.output2, true)));
        for (ItemStack stack : recipe.byproduct) outputs.add(List.of(stack.copy()));
        return outputs;
    }
}
