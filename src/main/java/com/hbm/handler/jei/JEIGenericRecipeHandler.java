package com.hbm.handler.jei;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.machine.ItemFluidIcon;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The JEI category for machines on the {@link GenericRecipes} engine (assembly machine, chemical plant) —
 * a faithful port of the 1.12.2 {@code JEIGenericRecipeHandler}: the shared {@code gui_nei.png} background,
 * the same count-based input/output coordinate tables, per-slot 18×18 frames from the texture, and the
 * vertical <b>arrow-over-machine pill</b> {@code (59,87,18,36)} drawn at {@code (74,14)} with the machine
 * icon in its lower half. The old assembly-machine offset (recipes with &gt;9 inputs shift everything
 * +18px) is preserved. Fluids render as {@link ItemFluidIcon} droplets; {@link AStack} inputs expand via
 * {@code getDisplayStacks()} (the old {@code extractForJEI()}). Blueprint-template display is deferred
 * (the port's blueprints are one-item-per-pool, no {@code ItemBlueprints.make}).
 */
public class JEIGenericRecipeHandler extends HbmRecipeCategory<GenericRecipe> {

    protected final GenericRecipes recipeSet;
    protected final ItemStack[] machines;
    protected final IDrawable slotFrame;    // gui_nei 5,87 18x18
    protected final IDrawable machinePill;  // gui_nei 59,87 18x36 (arrow above, machine below)

    public JEIGenericRecipeHandler(IGuiHelper helper, IRecipeType<GenericRecipe> type, Component title,
                                   GenericRecipes recipeSet, ItemStack... machines) {
        super(helper, type, title, machines.length > 0 ? machines[0] : ItemStack.EMPTY, "gui_nei.png");
        this.recipeSet = recipeSet;
        this.machines = machines;
        this.slotFrame = helper.createDrawable(tex("gui_nei.png"), 5, 87, 18, 18);
        this.machinePill = helper.createDrawable(tex("gui_nei.png"), 59, 87, 18, 36);
    }

    /** The recipes to register for this category — the machine's whole ordered list, minus empties. */
    public List<GenericRecipe> buildRecipes() {
        recipeSet.registerRecipes(); // idempotent; JEI runs client-side before the machine's first server tick
        List<GenericRecipe> out = new ArrayList<>();
        for (GenericRecipe recipe : recipeSet.recipeOrderedList) {
            if (!inputs(recipe).isEmpty() || !outputs(recipe).isEmpty()) out.add(recipe);
        }
        return out;
    }

    /** Expanded input ingredient lists (item AStacks first, then fluid droplets), one entry per slot. */
    protected static List<List<ItemStack>> inputs(GenericRecipe recipe) {
        List<List<ItemStack>> list = new ArrayList<>();
        if (recipe.inputItem != null) for (AStack a : recipe.inputItem) {
            List<ItemStack> vars = a.getDisplayStacks();
            if (!vars.isEmpty()) list.add(vars);
        }
        if (recipe.inputFluid != null) for (FluidStack f : recipe.inputFluid) {
            ItemStack icon = ItemFluidIcon.make(f);
            if (!icon.isEmpty()) list.add(Collections.singletonList(icon));
        }
        return list;
    }

    /** Expanded output lists (item outputs first, then fluid droplets), one entry per slot. */
    protected static List<List<ItemStack>> outputs(GenericRecipe recipe) {
        List<List<ItemStack>> list = new ArrayList<>();
        if (recipe.outputItem != null) for (ItemStack s : recipe.outputItem) {
            if (s != null && !s.isEmpty()) list.add(Collections.singletonList(s.copy()));
        }
        if (recipe.outputFluid != null) for (FluidStack f : recipe.outputFluid) {
            ItemStack icon = ItemFluidIcon.make(f);
            if (!icon.isEmpty()) list.add(Collections.singletonList(icon));
        }
        return list;
    }

    /** The old {@code AssemblyMachineRecipeHandler} offset: &gt;9 item inputs shift the whole row +18px. */
    protected static int xOffset(GenericRecipe recipe) {
        return recipe.inputItem != null && recipe.inputItem.length > 9 ? 18 : 0;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GenericRecipe recipe, IFocusGroup focuses) {
        int off = xOffset(recipe);
        List<List<ItemStack>> in = inputs(recipe);
        List<List<ItemStack>> out = outputs(recipe);

        int[][] inPos = getInputSlotPositions(in.size());
        for (int i = 0; i < in.size(); i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, inPos[i][0] + off, inPos[i][1])
                    .addItemStacks(in.get(i)).setBackground(slotFrame, -1, -1);
        }

        int[][] outPos = getOutputSlotPositions(out.size());
        for (int i = 0; i < out.size(); i++) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, outPos[i][0] + off, outPos[i][1])
                    .addItemStacks(out.get(i)).setBackground(slotFrame, -1, -1);
        }

        // The machine itself, sitting in the lower half of the pill (old: init(74+off, 29) → item at +1).
        if (machines != null && machines.length > 0) {
            builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 75 + off, 30)
                    .addItemStacks(List.of(machines));
        }
    }

    @Override
    public void draw(GenericRecipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        super.draw(recipe, slots, g, mouseX, mouseY);
        machinePill.draw(g, 74 + xOffset(recipe), 14);
    }

    // The 1.12.2 JEIGenericRecipeHandler coordinate tables (item positions; the frame draws at -1,-1).
    public static int[][] getInputSlotPositions(int count) {
        if (count == 1) return new int[][]{{48, 24}};
        if (count == 2) return new int[][]{{30, 24}, {48, 24}};
        if (count == 3) return new int[][]{{12, 24}, {30, 24}, {48, 24}};
        if (count == 4) return new int[][]{{30, 15}, {48, 15}, {30, 33}, {48, 33}};
        if (count == 5) return new int[][]{{12, 15}, {30, 15}, {48, 15}, {12, 33}, {30, 33}};
        if (count == 6) return new int[][]{{12, 15}, {30, 15}, {48, 15}, {12, 33}, {30, 33}, {48, 33}};

        int[][] slots = new int[count][2];
        int cols = (count + 2) / 3;
        for (int i = 0; i < count; i++) {
            slots[i][0] = 12 + (i % cols) * 18 - (cols == 4 ? 18 : 0);
            slots[i][1] = 6 + (i / cols) * 18;
        }
        return slots;
    }

    public static int[][] getOutputSlotPositions(int count) {
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
