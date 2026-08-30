package com.hbm.handler.jei;

//? if jei {

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.ElectrolyserMetalRecipes;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.items.machine.ItemScraps;
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

/** JEI category for crystal-to-molten-material electrolyser recipes. */
public final class ElectrolyserMetalRecipeHandler extends HbmRecipeCategory<ElectrolyserMetalRecipes.ElectrolysisMetalRecipe> {

    public ElectrolyserMetalRecipeHandler(IGuiHelper helper,
                                          JeiRecipeType<ElectrolyserMetalRecipes.ElectrolysisMetalRecipe> type,
                                          Component title, ItemStack machine) {
        super(helper, type, title, machine, "gui_nei.png");
    }

    public List<ElectrolyserMetalRecipes.ElectrolysisMetalRecipe> buildRecipes() {
        ElectrolyserMetalRecipes.register();
        return new ArrayList<>(ElectrolyserMetalRecipes.RECIPES);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ElectrolyserMetalRecipes.ElectrolysisMetalRecipe recipe,
                          IFocusGroup focuses) {
        List<ItemStack> inputs = recipe.input.getDisplayStacks();
        if (!inputs.isEmpty()) add(builder, RecipeIngredientRole.INPUT, 30, 24, inputs);
        add(builder, RecipeIngredientRole.INPUT, 48, 24, ItemFluidIcon.make(new FluidStack(Fluids.NITRIC_ACID, 100)));
        List<ItemStack> outputs = new ArrayList<>();
        if (recipe.output1 != null) outputs.add(ItemScraps.create(recipe.output1, true));
        if (recipe.output2 != null) outputs.add(ItemScraps.create(recipe.output2, true));
        for (ItemStack stack : recipe.byproduct) outputs.add(stack.copy());
        int[][] positions = JEIUniversalHandler.getOutputCoords(outputs.size());
        for (int i = 0; i < outputs.size(); i++) add(builder, RecipeIngredientRole.OUTPUT,
                positions[i][0], positions[i][1], outputs.get(i));
        add(builder, dev.golem.core.jei.Jei.CRAFTING_STATION, 75, 31,
                new ItemStack(ModMachineBlocks.machine_electrolyser.get()));
    }

    @Override
    public void draw(ElectrolyserMetalRecipes.ElectrolysisMetalRecipe recipe, IRecipeSlotsView slots,
                     GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        super.draw(recipe, slots, graphics, mouseX, mouseY);
        text(graphics, "100 mB", 47, 43);
        text(graphics, recipe.duration + " t", 8, 4);
    }

    private static void add(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y, ItemStack stack) {
        add(builder, role, x, y, List.of(stack));
    }

    private static void add(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y, List<ItemStack> stacks) {
        builder.addSlot(role, x, y).addItemStacks(stacks);
    }
}
//?}
