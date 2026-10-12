package com.hbm.handler.jei;

import dev.golem.core.viewer.Drawable;
import dev.golem.core.viewer.Role;
import dev.golem.core.viewer.SlotLayout;
import com.hbm.inventory.recipes.CyclotronRecipes;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** The legacy three-slot cyclotron JEI category. */
public final class CyclotronRecipeHandler extends HbmRecipeCategory<CyclotronRecipes.CyclotronRecipe> {
    private final Drawable arrow;

    public CyclotronRecipeHandler(ViewerType<CyclotronRecipes.CyclotronRecipe> type,
                                  Component title, ItemStack machine) {
        super(type, title, machine, "gui_nei_cyclotron.png", 127, 53, 24, 16);
        Drawable.Texture arrowStatic = Drawable.texture(tex("gui_nei_cyclotron.png"), 100, 118, 24, 18);
        arrow = Drawable.animated(arrowStatic, 48, Drawable.Direction.LEFT, false);
    }

    @Override
    public void setRecipe(SlotLayout builder, CyclotronRecipes.CyclotronRecipe recipe) {
        builder.addSlot(Role.INPUT, 1, 18)
                .addItemStacks(recipe.particle.getDisplayStacks());
        builder.addSlot(Role.INPUT, 55, 18)
                .addItemStacks(recipe.input.getDisplayStacks());
        builder.addSlot(Role.OUTPUT, 109, 18)
                .addItemStacks(List.of(recipe.output));
    }

    @Override
    public void draw(CyclotronRecipes.CyclotronRecipe recipe, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        super.draw(recipe, graphics, mouseX, mouseY);
        arrow.draw(graphics, 25, 18);
    }
}
