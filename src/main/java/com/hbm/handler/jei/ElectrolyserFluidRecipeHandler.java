package com.hbm.handler.jei;

//? if jei {

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.recipes.ElectrolyserFluidRecipes;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.items.machine.ItemFluidIcon;
import dev.golem.core.jei.JeiRecipeType;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** JEI category for the real-fluid side of the legacy electrolyser. */
public final class ElectrolyserFluidRecipeHandler extends HbmRecipeCategory<ElectrolyserFluidRecipes.ElectrolysisRecipe> {

    public ElectrolyserFluidRecipeHandler(IGuiHelper helper,
                                          JeiRecipeType<ElectrolyserFluidRecipes.ElectrolysisRecipe> type,
                                          Component title, ItemStack machine) {
        super(helper, type, title, machine, "gui_nei.png");
    }

    public List<ElectrolyserFluidRecipes.ElectrolysisRecipe> buildRecipes() {
        ElectrolyserFluidRecipes.register();
        return new ArrayList<>(ElectrolyserFluidRecipes.RECIPES.values());
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ElectrolyserFluidRecipes.ElectrolysisRecipe recipe,
                          IFocusGroup focuses) {
        add(builder, RecipeIngredientRole.INPUT, 48, 24, ItemFluidIcon.make(new FluidStack(recipe.input, recipe.amount)));
        List<ItemStack> outputs = new ArrayList<>();
        if (recipe.output1.type != com.hbm.inventory.fluid.Fluids.NONE) outputs.add(ItemFluidIcon.make(recipe.output1));
        if (recipe.output2.type != com.hbm.inventory.fluid.Fluids.NONE) outputs.add(ItemFluidIcon.make(recipe.output2));
        for (ItemStack stack : recipe.byproduct) outputs.add(stack.copy());
        int[][] positions = JEIUniversalHandler.getOutputCoords(outputs.size());
        for (int i = 0; i < outputs.size(); i++) add(builder, RecipeIngredientRole.OUTPUT,
                positions[i][0], positions[i][1], outputs.get(i));
        add(builder, dev.golem.core.jei.Jei.CRAFTING_STATION, 75, 31,
                new ItemStack(ModMachineBlocks.machine_electrolyser.get()));
    }

    @Override
    public void draw(ElectrolyserFluidRecipes.ElectrolysisRecipe recipe, IRecipeSlotsView slots,
                     GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        super.draw(recipe, slots, graphics, mouseX, mouseY);
        text(graphics, recipe.amount + " mB", 47, 43);
        text(graphics, recipe.duration + " t", 8, 4);
    }

    private static void add(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y, ItemStack stack) {
        builder.addSlot(role, x, y).addItemStacks(List.of(stack));
    }
}
//?}
