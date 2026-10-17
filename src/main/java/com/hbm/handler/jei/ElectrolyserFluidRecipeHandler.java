package com.hbm.handler.jei;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.ElectrolyserFluidRecipes;
import com.hbm.items.machine.ItemFluidIcon;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** 1.7 {@code ElectrolyserFluidHandler}: the input fluid, then both product fluids and the byproducts, as 1.7 listed them. */
public final class ElectrolyserFluidRecipeHandler extends JEIUniversalHandler<ElectrolyserFluidRecipes.ElectrolysisRecipe> {

    public ElectrolyserFluidRecipeHandler(ViewerType<ElectrolyserFluidRecipes.ElectrolysisRecipe> type,
                                          Component title, ItemStack machine) {
        super(type, title, machine);
    }

    public static List<ElectrolyserFluidRecipes.ElectrolysisRecipe> buildRecipes() {
        ElectrolyserFluidRecipes.register();
        return new ArrayList<>(ElectrolyserFluidRecipes.RECIPES.values());
    }

    @Override
    protected List<List<ItemStack>> inputs(ElectrolyserFluidRecipes.ElectrolysisRecipe recipe) {
        return List.of(List.of(ItemFluidIcon.make(new FluidStack(recipe.input, recipe.amount))));
    }

    @Override
    protected List<List<ItemStack>> outputs(ElectrolyserFluidRecipes.ElectrolysisRecipe recipe) {
        List<List<ItemStack>> outputs = new ArrayList<>();
        if (recipe.output1.type != Fluids.NONE) outputs.add(List.of(ItemFluidIcon.make(recipe.output1)));
        if (recipe.output2.type != Fluids.NONE) outputs.add(List.of(ItemFluidIcon.make(recipe.output2)));
        for (ItemStack stack : recipe.byproduct) outputs.add(List.of(stack.copy()));
        return outputs;
    }
}
