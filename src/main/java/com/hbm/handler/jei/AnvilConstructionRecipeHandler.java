package com.hbm.handler.jei;

//? if jei {

import com.hbm.blocks.ModAnvilBlocks;
import com.hbm.blocks.machine.NTMAnvil;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilConstructionRecipe;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilOutput;
import dev.golem.core.registry.RegistryObject;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import dev.golem.core.jei.JeiRecipeType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI category for anvil construction — a faithful port of the 1.12.2 {@code AnvilRecipeHandler}: its own
 * {@code gui_nei_anvil.png} background, and a per-{@code OverlayType} layout (SMITHING = 1:1 across the
 * middle, CONSTRUCTION = 6-wide input area → single output, RECYCLING = single input → 6-wide output
 * area, NONE = 4-wide areas both sides), with the matching texture overlays (input/output area panels +
 * the operation pill) drawn per recipe. The anvil slot cycles every anvil whose tier can run the recipe
 * (the old {@code NTMAnvil.getAnvilsFromTier}); chance outputs get the old red percentage tooltip.
 *
 * <p>(The old handler's {@code case NONE -> {}} left the layout at zeros while its {@code drawInfo} drew
 * the 4-wide-areas art — clearly a dead-{@code default} bug; NONE here uses the default-branch values,
 * which match the art.)
 */
public class AnvilConstructionRecipeHandler extends HbmRecipeCategory<AnvilConstructionRecipe> {

    private final IDrawable area72;     // input/output area panel, 4-wide
    private final IDrawable area108;    // input/output area panel, 6-wide
    private final IDrawable smallSlot;  // single-slot highlight
    private final IDrawable opNone, opSmith, opCon, opRecycle; // 18x36 operation pills

    public AnvilConstructionRecipeHandler(IGuiHelper helper, JeiRecipeType<AnvilConstructionRecipe> type, Component title, ItemStack icon) {
        super(helper, type, title, icon, "gui_nei_anvil.png");
        this.area72 = helper.createDrawable(tex("gui_nei_anvil.png"), 5, 87, 72, 54);
        this.area108 = helper.createDrawable(tex("gui_nei_anvil.png"), 5, 87, 108, 54);
        this.smallSlot = helper.createDrawable(tex("gui_nei_anvil.png"), 113, 105, 18, 18);
        this.opNone = helper.createDrawable(tex("gui_nei_anvil.png"), 131, 96, 18, 36);
        this.opSmith = helper.createDrawable(tex("gui_nei_anvil.png"), 149, 96, 18, 36);
        this.opCon = helper.createDrawable(tex("gui_nei_anvil.png"), 167, 96, 18, 36);
        this.opRecycle = helper.createDrawable(tex("gui_nei_anvil.png"), 185, 96, 18, 36);
    }

    public static List<AnvilConstructionRecipe> buildRecipes() {
        AnvilRecipes.register();
        return AnvilRecipes.getConstruction();
    }

    /** Every anvil whose tier can run this recipe (the old {@code getAnvilsFromTier}). */
    private static List<ItemStack> anvilsFor(AnvilConstructionRecipe recipe) {
        List<ItemStack> list = new ArrayList<>();
        for (RegistryObject<NTMAnvil> anvil : ModAnvilBlocks.ANVILS) {
            if (recipe.isTierValid(anvil.get().tier)) list.add(new ItemStack(anvil.get()));
        }
        return list;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AnvilConstructionRecipe recipe, IFocusGroup focuses) {
        int inLine = 1, outLine = 1, inOX = 0, inOY = 0, outOX = 0, outOY = 0, anvX = 75;

        switch (recipe.getOverlay()) {
            case SMITHING -> { inOX = 48; inOY = 24; outOX = 102; outOY = 24; anvX = 75; }
            case RECYCLING -> { outLine = 6; inOX = 12; inOY = 24; outOX = 48; outOY = 6; anvX = 30; }
            case CONSTRUCTION -> { inLine = 6; inOX = 12; inOY = 6; outOX = 138; outOY = 24; anvX = 120; }
            case NONE -> { inLine = 4; outLine = 4; inOX = 3; inOY = 6; outOX = 93; outOY = 6; anvX = 75; }
        }

        for (int i = 0; i < recipe.input.size(); i++) {
            AStack a = recipe.input.get(i);
            builder.addSlot(RecipeIngredientRole.INPUT, inOX + 18 * (i % inLine), inOY + 18 * (i / inLine))
                    .addItemStacks(a.getDisplayStacks()).setBackground(helper.getSlotDrawable(), -1, -1);
        }

        // The anvil, as an extra input like the original (so looking up an anvil's uses lists its recipes).
        builder.addSlot(RecipeIngredientRole.INPUT, anvX, 31).addItemStacks(anvilsFor(recipe));

        for (int i = 0; i < recipe.output.size(); i++) {
            AnvilOutput out = recipe.output.get(i);
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.OUTPUT, outOX + 18 * (i % outLine), outOY + 18 * (i / outLine))
                    .addItemStacks(List.of(out.stack)).setBackground(helper.getSlotDrawable(), -1, -1);
            if (out.chance < 1F) {
                slot.addRichTooltipCallback((view, tooltip) ->
                        tooltip.add(Component.literal(((int) (out.chance * 1000)) / 10D + "%").withStyle(ChatFormatting.RED)));
            }
        }
    }

    @Override
    public void draw(AnvilConstructionRecipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        super.draw(recipe, slots, g, mouseX, mouseY);
        switch (recipe.getOverlay()) {
            case NONE -> {
                area72.draw(g, 2, 5);
                area72.draw(g, 92, 5);
                opNone.draw(g, 74, 14);
            }
            case SMITHING -> {
                smallSlot.draw(g, 47, 23);
                smallSlot.draw(g, 101, 23);
                opSmith.draw(g, 74, 14);
            }
            case CONSTRUCTION -> {
                area108.draw(g, 11, 5);
                smallSlot.draw(g, 137, 23);
                opCon.draw(g, 119, 14);
            }
            case RECYCLING -> {
                smallSlot.draw(g, 11, 23);
                area108.draw(g, 47, 5);
                opRecycle.draw(g, 29, 14);
            }
        }
    }
}
//?}
