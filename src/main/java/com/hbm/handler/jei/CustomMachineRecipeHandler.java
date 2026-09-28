package com.hbm.handler.jei;

//? if jei {

import com.hbm.blocks.ModCustomMachineBlocks;
import com.hbm.config.CustomMachineConfigJSON;
import com.hbm.config.CustomMachineConfigJSON.MachineConfiguration;
import com.hbm.inventory.recipes.CustomMachineRecipes;
import com.hbm.inventory.recipes.CustomMachineRecipes.CustomMachineRecipe;
import com.hbm.items.machine.ItemFluidIcon;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.jei.JeiRecipeType;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
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
import java.util.Locale;

/**
 * 1.7 {@code CustomMachineHandler}: one category per configured machine, titled with its name, on
 * {@code gui_nei_custom.png}. Input fluids (12+18i, 6) and items (12+18i, 24/42), output fluids (102+18i, 6) and items
 * (102+18i, 24/42), the controller at (75,42); radiation and pollution right-aligned at x 160 (y 63/75), heat and
 * flux centred on x 83 when the machine uses them.
 */
public final class CustomMachineRecipeHandler extends HbmRecipeCategory<CustomMachineRecipe> {

    private final MachineConfiguration conf;

    public CustomMachineRecipeHandler(IGuiHelper helper, MachineConfiguration conf) {
        super(helper, type(conf), Component.literal(conf.displayName()), machine(conf), "gui_nei_custom.png");
        this.conf = conf;
    }

    /** 1.7's handler id was {@code "ntm_" + unlocalizedName}. */
    public static JeiRecipeType<CustomMachineRecipe> type(MachineConfiguration conf) {
        String path = ("ntm_" + conf.unlocalizedName).toLowerCase(Locale.US).replaceAll("[^a-z0-9_]", "_");
        return JeiRecipeType.create("hbm", path, CustomMachineRecipe.class);
    }

    public static ItemStack machine(MachineConfiguration conf) {
        return ModCustomMachineBlocks.custom_machine_item.get().of(CustomMachineConfigJSON.niceList.indexOf(conf));
    }

    public static List<CustomMachineRecipe> buildRecipes(MachineConfiguration conf) {
        List<CustomMachineRecipe> recipes = CustomMachineRecipes.get(conf.recipeKey);
        return recipes != null ? recipes : new ArrayList<>();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CustomMachineRecipe recipe, IFocusGroup focuses) {
        for (int i = 0; i < 3; i++) if (recipe.inputFluids.length > i)
            builder.addSlot(RecipeIngredientRole.INPUT, 12 + i * 18, 6).addItemStack(ItemFluidIcon.make(recipe.inputFluids[i]));
        for (int i = 0; i < 3; i++) if (recipe.inputItems.length > i)
            builder.addSlot(RecipeIngredientRole.INPUT, 12 + i * 18, 24).addItemStacks(recipe.inputItems[i].getDisplayStacks());
        for (int i = 3; i < 6; i++) if (recipe.inputItems.length > i)
            builder.addSlot(RecipeIngredientRole.INPUT, 12 + (i - 3) * 18, 42).addItemStacks(recipe.inputItems[i].getDisplayStacks());

        for (int i = 0; i < 3; i++) if (recipe.outputFluids.length > i)
            builder.addSlot(RecipeIngredientRole.OUTPUT, 102 + i * 18, 6).addItemStack(ItemFluidIcon.make(recipe.outputFluids[i]));
        for (int i = 0; i < 6; i++) if (recipe.outputItems.length > i) {
            var out = recipe.outputItems[i];
            var slot = builder.addSlot(RecipeIngredientRole.OUTPUT, 102 + (i % 3) * 18, i < 3 ? 24 : 42)
                    .addItemStack(out.stack().copy());
            // 1.7 RecipeSet: a red "<chance>%" tooltip line on every output whose chance is not exactly 1
            if (out.chance() != 1) {
                String percent = chanceText(out.chance());
                slot.addRichTooltipCallback((view, tooltip) ->
                        tooltip.add(Component.literal(percent).withStyle(net.minecraft.ChatFormatting.RED)));
            }
        }

        builder.addSlot(dev.golem.core.jei.Jei.CRAFTING_STATION, 75, 42).addItemStack(machine(conf));
    }

    /** 1.7 {@code (((int)(chance * 1000)) / 10D) + "%"}: one decimal, truncated toward zero. */
    public static String chanceText(float chance) {
        return (((int) (chance * 1000)) / 10D) + "%";
    }

    /** 1.7 {@code drawExtras}. */
    @Override
    public void draw(CustomMachineRecipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        super.draw(recipe, slots, g, mouseX, mouseY);
        var font = Minecraft.getInstance().font;
        int side = 83;
        if (recipe.radiationAmount != 0) {
            String radiation = "Radiation:" + recipe.radiationAmount;
            GuiDraw.text(g, font, radiation, 160 - font.width(radiation), 63, 0xFF08FF00, false);
        }
        if (recipe.pollutionAmount != 0) {
            String pollution = recipe.pollutionType + ":" + recipe.pollutionAmount;
            GuiDraw.text(g, font, pollution, 160 - font.width(pollution), 75, 0xFF404040, false);
        }
        if (conf.fluxMode) {
            String flux = "Flux:" + recipe.flux;
            GuiDraw.text(g, font, flux, side - font.width(flux) / 2, 16, 0xFF08FF00, false);
        }
        if (conf.maxHeat > 0 && recipe.heat > 0) {
            String heat = "Heat:" + recipe.heat;
            GuiDraw.text(g, font, heat, side - font.width(heat) / 2, 8, 0xFFFF0000, false);
        }
    }
}
//?}
