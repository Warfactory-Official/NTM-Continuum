package com.hbm.handler.jei;

//? if jei {

import com.hbm.inventory.recipes.GasCentrifugeRecipes;
import com.hbm.items.ModBatteryItems;
import com.hbm.items.ModUpgradeItems;
import com.hbm.items.machine.ItemFluidIcon;
import dev.golem.core.jei.JeiRecipeType;
import dev.golem.core.gui.GuiDraw;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** JEI category for the legacy gas centrifuge's real-fluid recipes. */
public final class GasCentrifugeRecipeHandler extends HbmRecipeCategory<GasCentrifugeRecipes.GasCentrifugeRecipe> {

    private final ItemStack[] batteries;
    private final IDrawableAnimated powerAnimated;
    private final IDrawableAnimated progressAnimated;
    private final IDrawableStatic highSpeedOverlay;

    public GasCentrifugeRecipeHandler(IGuiHelper helper, JeiRecipeType<GasCentrifugeRecipes.GasCentrifugeRecipe> type,
                                       Component title, ItemStack machine) {
        super(helper, type, title, machine, "gui_jei_gas_centrifuge.png", 162, 54, 3, 6);
        IDrawableStatic powerStatic = helper.createDrawable(tex("gui_jei_gas_centrifuge.png"), 168, 37, 16, 34);
        powerAnimated = helper.createAnimatedDrawable(powerStatic, 480, IDrawableAnimated.StartDirection.TOP, true);
        IDrawableStatic progressStatic = helper.createDrawable(tex("gui_jei_gas_centrifuge.png"), 168, 0, 44, 37);
        progressAnimated = helper.createAnimatedDrawable(progressStatic, 150, IDrawableAnimated.StartDirection.LEFT, false);
        highSpeedOverlay = helper.createDrawable(tex("gui_jei_gas_centrifuge.png"), 184, 37, 16, 16);
        List<ItemStack> list = new ArrayList<>();
        for (var battery : ModBatteryItems.PACKS.values()) list.add(new ItemStack(battery.get()));
        batteries = list.toArray(new ItemStack[0]);
    }

    @Override
    public void setRecipe(mezz.jei.api.gui.builder.IRecipeLayoutBuilder builder,
                           GasCentrifugeRecipes.GasCentrifugeRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 45, 19)
                .addItemStacks(List.of(ItemFluidIcon.make(recipe.input)));
        for (int i = 0; i < recipe.output.length; i++) {
            int x = i % 2 == 0 ? 127 : 145;
            int y = i < 2 ? 10 : 28;
            builder.addSlot(RecipeIngredientRole.OUTPUT, x, y).addItemStacks(List.of(recipe.output[i]));
        }
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 37).addItemStacks(List.of(batteries));
        if (recipe.highSpeed) {
            builder.addSlot(RecipeIngredientRole.INPUT, 24, 20)
                    .addItemStacks(List.of(new ItemStack(ModUpgradeItems.upgrade_gc_speed.get())));
        }
    }

    @Override
    public void draw(GasCentrifugeRecipes.GasCentrifugeRecipe recipe, IRecipeSlotsView slots,
                     GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        super.draw(recipe, slots, graphics, mouseX, mouseY);
        powerAnimated.draw(graphics, 1, 1);
        progressAnimated.draw(graphics, 72, 12);
        if (recipe.highSpeed) {
            GuiDraw.nextStratum(graphics);
            highSpeedOverlay.draw(graphics, 23, 19);
        }

        String centrifuges = recipe.centrifuges + " G. Cents";
        GuiDraw.text(graphics, Minecraft.getInstance().font, centrifuges,
                50 - textWidth(centrifuges) / 2 - 3, 4, 0xFF00FF00, false);
    }
}
//?}
