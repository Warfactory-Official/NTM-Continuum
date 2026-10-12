package com.hbm.handler.jei;

import dev.golem.core.viewer.Drawable;
import dev.golem.core.viewer.Role;
import dev.golem.core.viewer.SlotLayout;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.items.machine.ItemBlueprints;
import dev.golem.core.viewer.ViewerType;
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
 * {@code getDisplayStacks()} (the old {@code extractForJEI()}). Pooled recipes display their required
 * dynamic blueprint templates above the machine, as in the old handler.
 */
public class JEIGenericRecipeHandler extends HbmRecipeCategory<GenericRecipe> {

    protected final GenericRecipes recipeSet;
    protected final ItemStack[] machines;
    protected final Drawable slotFrame;    // gui_nei 5,87 18x18
    protected final Drawable machinePill;  // gui_nei 59,87 18x36 (arrow above, machine below)
    protected final Drawable blueprintPill; // gui_nei 77,87 18x50 (blueprint, arrow, machine)

    public JEIGenericRecipeHandler(ViewerType<GenericRecipe> type, Component title,
                                   GenericRecipes recipeSet, ItemStack... machines) {
        super(type, title, machines.length > 0 ? machines[0] : ItemStack.EMPTY, "gui_nei.png");
        this.recipeSet = recipeSet;
        this.machines = machines;
        this.slotFrame = Drawable.texture(tex("gui_nei.png"), 5, 87, 18, 18);
        this.machinePill = Drawable.texture(tex("gui_nei.png"), 59, 87, 18, 36);
        this.blueprintPill = Drawable.texture(tex("gui_nei.png"), 77, 87, 18, 50);
    }

    /** The recipes to register for this category — the machine's whole ordered list, minus empties. */
    public List<GenericRecipe> buildRecipes() {
        recipeSet.registerRecipes(); // idempotent; JEI runs client-side before the machine's first server tick
        List<GenericRecipe> out = new ArrayList<>();
        for (GenericRecipe recipe : recipeSet.recipeOrderedList) {
            if (recipe.isPooled() && recipe.blueprintPools != null) {
                boolean secret = false;
                for (String pool : recipe.blueprintPools) {
                    if (pool.startsWith(GenericRecipes.POOL_PREFIX_SECRET)) {
                        secret = true;
                        break;
                    }
                }
                if (secret) continue;
            }
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
        if (recipe.outputChoices != null) {
            List<ItemStack> choices = new ArrayList<>();
            for (GenericRecipe.OutputChoice choice : recipe.outputChoices) choices.add(choice.stack().copy());
            if (!choices.isEmpty()) list.add(choices);
        } else if (recipe.outputItem != null) for (ItemStack s : recipe.outputItem) {
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

    /** Dynamic blueprint stacks are the pooled recipe requirement, not ordinary recipe ingredients. */
    protected static List<ItemStack> blueprintTemplates(GenericRecipe recipe) {
        if (!recipe.isPooled() || recipe.blueprintPools == null) return List.of();
        List<ItemStack> templates = new ArrayList<>(recipe.blueprintPools.length);
        for (String pool : recipe.blueprintPools) templates.add(ItemBlueprints.make(pool));
        return templates;
    }

    @Override
    public void setRecipe(SlotLayout builder, GenericRecipe recipe) {
        int off = xOffset(recipe);
        List<List<ItemStack>> in = inputs(recipe);
        List<List<ItemStack>> out = outputs(recipe);
        List<ItemStack> templates = blueprintTemplates(recipe);

        int[][] inPos = getInputSlotPositions(in.size());
        for (int i = 0; i < in.size(); i++) {
            builder.addSlot(Role.INPUT, inPos[i][0] + off, inPos[i][1])
                    .addItemStacks(in.get(i)).setBackground(slotFrame, -1, -1);
        }

        int[][] outPos = getOutputSlotPositions(out.size());
        for (int i = 0; i < out.size(); i++) {
            builder.addSlot(Role.OUTPUT, outPos[i][0] + off, outPos[i][1])
                    .addItemStacks(out.get(i)).setBackground(slotFrame, -1, -1);
        }

        // The machine itself, sitting in the lower half of the pill (old: init(74+off, 29) → item at +1).
        if (machines != null && machines.length > 0) {
            builder.addSlot(Role.CRAFTING_STATION, 75 + off, templates.isEmpty() ? 30 : 37)
                    .addItemStacks(List.of(machines));
        }

        if (!templates.isEmpty()) {
            builder.addSlot(Role.OUTPUT, 75 + off, 10)
                    .addItemStacks(templates);
        }
    }

    @Override
    public void draw(GenericRecipe recipe, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        super.draw(recipe, g, mouseX, mouseY);
        if (blueprintTemplates(recipe).isEmpty()) {
            machinePill.draw(g, 74 + xOffset(recipe), 14);
        } else {
            blueprintPill.draw(g, 74 + xOffset(recipe), 7);
        }
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
