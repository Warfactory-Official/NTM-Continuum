package com.hbm.handler.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Faithful port of the 1.12.2 {@code JEIUniversalHandler} / {@code JeiUniversalRecipe}: the shared
 * {@code gui_nei.png} background, the count-based input/output coordinate tables, 18×18 slot frames at
 * every slot, and the vertical arrow-over-machine pill {@code (59,87,18,36)} at {@code (74,14)} with the
 * machine icon in its lower half. Subclasses provide the expanded input/output lists per recipe and may
 * draw extra stat text (the old wrapper {@code drawInfo} overrides).
 */
public abstract class JEIUniversalHandler<T> extends HbmRecipeCategory<T> {

    protected final ItemStack[] machines;
    protected final IDrawable slotFrame;
    protected final IDrawable machinePill;

    protected JEIUniversalHandler(IGuiHelper helper, IRecipeType<T> type, Component title, ItemStack... machines) {
        super(helper, type, title, machines.length > 0 ? machines[0] : ItemStack.EMPTY, "gui_nei.png");
        this.machines = machines;
        this.slotFrame = helper.createDrawable(tex("gui_nei.png"), 5, 87, 18, 18);
        this.machinePill = helper.createDrawable(tex("gui_nei.png"), 59, 87, 18, 36);
    }

    /** One entry per input slot (each entry cycles through its alternatives). */
    protected abstract List<List<ItemStack>> inputs(T recipe);

    /** One entry per output slot. */
    protected abstract List<List<ItemStack>> outputs(T recipe);

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses) {
        List<List<ItemStack>> in = inputs(recipe);
        List<List<ItemStack>> out = outputs(recipe);

        int[][] inPos = getInputCoords(in.size());
        for (int i = 0; i < in.size(); i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, inPos[i][0] + 1, inPos[i][1] + 1)
                    .addItemStacks(in.get(i)).setBackground(slotFrame, -1, -1);
        }

        int[][] outPos = getOutputCoords(out.size());
        for (int i = 0; i < out.size(); i++) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, outPos[i][0] + 1, outPos[i][1] + 1)
                    .addItemStacks(out.get(i)).setBackground(slotFrame, -1, -1);
        }

        // Machine icon in the pill's lower half (old: init(74,31) → item at 75,32).
        if (machines != null && machines.length > 0) {
            builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 75, 32)
                    .addItemStacks(List.of(machines));
        }
    }

    @Override
    public void draw(T recipe, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        super.draw(recipe, slots, g, mouseX, mouseY);
        machinePill.draw(g, 74, 14);
    }

    // The 1.12.2 JEIUniversalHandler coordinate tables (frame positions; items render at +1,+1).
    public static int[][] getInputCoords(int count) {
        return switch (count) {
            case 1 -> new int[][]{{48, 24}};
            case 2 -> new int[][]{{48, 24}, {30, 24}};
            case 3 -> new int[][]{{48, 24}, {30, 24}, {12, 24}};
            case 4 -> new int[][]{{48, 15}, {30, 15}, {48, 33}, {30, 33}};
            case 5 -> new int[][]{{48, 15}, {30, 15}, {12, 24}, {48, 33}, {30, 33}};
            case 6 -> new int[][]{{48, 15}, {30, 15}, {12, 15}, {48, 33}, {30, 33}, {12, 33}};
            case 7 -> new int[][]{{48, 6}, {30, 15}, {12, 15}, {48, 24}, {30, 33}, {12, 33}, {48, 42}};
            case 8 -> new int[][]{{48, 6}, {30, 6}, {12, 15}, {48, 24}, {30, 24}, {12, 33}, {48, 42}, {30, 42}};
            case 9 -> new int[][]{{48, 6}, {30, 6}, {12, 6}, {48, 24}, {30, 24}, {12, 24}, {48, 42}, {30, 42}, {12, 42}};
            default -> new int[count][2];
        };
    }

    public static int[][] getOutputCoords(int count) {
        return switch (count) {
            case 1 -> new int[][]{{102, 24}};
            case 2 -> new int[][]{{102, 24}, {120, 24}};
            case 3 -> new int[][]{{102, 24}, {120, 24}, {138, 24}};
            case 4 -> new int[][]{{102, 15}, {120, 15}, {102, 33}, {120, 33}};
            case 5 -> new int[][]{{102, 15}, {120, 15}, {102, 33}, {120, 33}, {138, 24}};
            case 6 -> new int[][]{{102, 6}, {120, 6}, {102, 24}, {120, 24}, {102, 42}, {120, 42}};
            case 7 -> new int[][]{{102, 6}, {120, 6}, {102, 24}, {120, 24}, {102, 42}, {120, 42}, {138, 24}};
            case 8 -> new int[][]{{102, 6}, {120, 6}, {102, 24}, {120, 24}, {102, 42}, {120, 42}, {138, 24}, {138, 42}};
            default -> new int[count][2];
        };
    }
}
