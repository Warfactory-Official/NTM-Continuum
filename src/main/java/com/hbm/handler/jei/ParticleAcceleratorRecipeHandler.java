package com.hbm.handler.jei;

//? if jei {

import com.hbm.inventory.recipes.ParticleAcceleratorRecipes;
import com.hbm.inventory.recipes.ParticleAcceleratorRecipes.ParticleAcceleratorRecipe;
import dev.golem.core.jei.JeiRecipeType;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** JEI category for the particle accelerator: two particles in, one or two products out, and the momentum the beam has to reach. */
public class ParticleAcceleratorRecipeHandler extends JEIUniversalHandler<ParticleAcceleratorRecipe> {

    public ParticleAcceleratorRecipeHandler(IGuiHelper helper, JeiRecipeType<ParticleAcceleratorRecipe> type,
                                            Component title, ItemStack... machines) {
        super(helper, type, title, machines);
    }

    public static List<ParticleAcceleratorRecipe> buildRecipes() {
        ParticleAcceleratorRecipes.register();
        return new ArrayList<>(ParticleAcceleratorRecipes.recipes);
    }

    @Override
    protected List<List<ItemStack>> inputs(ParticleAcceleratorRecipe recipe) {
        return List.of(recipe.input1.getDisplayStacks(), recipe.input2.getDisplayStacks());
    }

    @Override
    protected List<List<ItemStack>> outputs(ParticleAcceleratorRecipe recipe) {
        if (recipe.output2 == null) return List.of(List.of(recipe.output1.copy()));
        return List.of(List.of(recipe.output1.copy()), List.of(recipe.output2.copy()));
    }

    @Override
    public void draw(ParticleAcceleratorRecipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor g,
                     double mouseX, double mouseY) {
        super.draw(recipe, slots, g, mouseX, mouseY);
        text(g, "Momentum: " + String.format(Locale.US, "%,d", recipe.momentum), 8, 52);
    }
}
//?}
