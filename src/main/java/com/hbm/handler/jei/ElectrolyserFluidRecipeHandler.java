package com.hbm.handler.jei;

import dev.golem.core.viewer.Role;
import dev.golem.core.viewer.SlotLayout;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.recipes.ElectrolyserFluidRecipes;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.items.machine.ItemFluidIcon;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** JEI category for the real-fluid side of the legacy electrolyser. */
public final class ElectrolyserFluidRecipeHandler extends HbmRecipeCategory<ElectrolyserFluidRecipes.ElectrolysisRecipe> {

    public ElectrolyserFluidRecipeHandler(ViewerType<ElectrolyserFluidRecipes.ElectrolysisRecipe> type,
                                          Component title, ItemStack machine) {
        super(type, title, machine, "gui_nei.png");
    }

    public List<ElectrolyserFluidRecipes.ElectrolysisRecipe> buildRecipes() {
        ElectrolyserFluidRecipes.register();
        return new ArrayList<>(ElectrolyserFluidRecipes.RECIPES.values());
    }

    @Override
    public void setRecipe(SlotLayout builder, ElectrolyserFluidRecipes.ElectrolysisRecipe recipe) {
        add(builder, Role.INPUT, 48, 24, ItemFluidIcon.make(new FluidStack(recipe.input, recipe.amount)));
        List<ItemStack> outputs = new ArrayList<>();
        if (recipe.output1.type != com.hbm.inventory.fluid.Fluids.NONE) outputs.add(ItemFluidIcon.make(recipe.output1));
        if (recipe.output2.type != com.hbm.inventory.fluid.Fluids.NONE) outputs.add(ItemFluidIcon.make(recipe.output2));
        for (ItemStack stack : recipe.byproduct) outputs.add(stack.copy());
        int[][] positions = JEIUniversalHandler.getOutputCoords(outputs.size());
        for (int i = 0; i < outputs.size(); i++) add(builder, Role.OUTPUT,
                positions[i][0], positions[i][1], outputs.get(i));
        add(builder, Role.CRAFTING_STATION, 75, 31,
                new ItemStack(ModMachineBlocks.machine_electrolyser.get()));
    }

    @Override
    public void draw(ElectrolyserFluidRecipes.ElectrolysisRecipe recipe, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        super.draw(recipe, graphics, mouseX, mouseY);
        text(graphics, recipe.amount + " mB", 47, 43);
        text(graphics, recipe.duration + " t", 8, 4);
    }

    private static void add(SlotLayout builder, Role role, int x, int y, ItemStack stack) {
        builder.addSlot(role, x, y).addItemStacks(List.of(stack));
    }
}
