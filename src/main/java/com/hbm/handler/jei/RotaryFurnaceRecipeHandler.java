package com.hbm.handler.jei;

//? if jei {

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.RotaryFurnaceRecipes;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.items.machine.ItemScraps;
import dev.golem.core.jei.JeiRecipeType;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * JEI view of the rotary furnace: the original NEI handler's unordered item inputs plus the optional fluid
 * icon, a molten-scraps output, and the duration / steam draw in the lower right.
 */
public class RotaryFurnaceRecipeHandler extends JEIUniversalHandler<RotaryFurnaceRecipes.RotaryFurnaceRecipe> {

    public RotaryFurnaceRecipeHandler(IGuiHelper helper, JeiRecipeType<RotaryFurnaceRecipes.RotaryFurnaceRecipe> type,
                                      Component title, ItemStack... machines) {
        super(helper, type, title, machines);
    }

    public static List<RotaryFurnaceRecipes.RotaryFurnaceRecipe> buildRecipes() {
        RotaryFurnaceRecipes.register();
        return List.copyOf(RotaryFurnaceRecipes.recipes);
    }

    @Override
    protected List<List<ItemStack>> inputs(RotaryFurnaceRecipes.RotaryFurnaceRecipe recipe) {
        List<List<ItemStack>> inputs = new ArrayList<>();
        for (AStack ingredient : recipe.ingredients) {
            List<ItemStack> display = ingredient.getDisplayStacks();
            if (!display.isEmpty()) inputs.add(display);
        }
        if (recipe.fluid != null) inputs.add(List.of(ItemFluidIcon.make(recipe.fluid)));
        return inputs;
    }

    @Override
    protected List<List<ItemStack>> outputs(RotaryFurnaceRecipes.RotaryFurnaceRecipe recipe) {
        return List.of(List.of(ItemScraps.create(recipe.output, true)));
    }

    @Override
    public void draw(RotaryFurnaceRecipes.RotaryFurnaceRecipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics,
                     double mouseX, double mouseY) {
        super.draw(recipe, slots, graphics, mouseX, mouseY);
        String duration = String.format(Locale.US, "%,d", recipe.duration) + " ticks";
        String consumption = Fluids.STEAM.getDisplayNameString() + ": " + String.format(Locale.US, "%,d", recipe.steam) + " mB/t";
        text(graphics, duration, 160 - textWidth(duration), 43);
        text(graphics, consumption, 160 - textWidth(consumption), 55);
    }
}
//?}
