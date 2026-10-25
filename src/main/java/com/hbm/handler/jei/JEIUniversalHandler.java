package com.hbm.handler.jei;

import dev.golem.core.viewer.Drawable;
import dev.golem.core.viewer.Role;
import dev.golem.core.viewer.SlotLayout;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 1.7 {@code NEIUniversalHandler}: the shared {@code gui_nei.png} background, the count-based input/output coordinate
 * tables (item positions, framed one pixel up and left), and the arrow-over-machine pill at {@code (74,14)} with the
 * machine at {@code (75,31)}. Subclasses give the expanded input/output lists per recipe, may name a per-recipe machine
 * (1.7's {@code machineOverrides}) and may draw extra stat text.
 */
public abstract class JEIUniversalHandler<T> extends HbmRecipeCategory<T> {

    protected final ItemStack[] machines;
    protected final Drawable slotFrame;
    protected final Drawable machinePill;

    protected JEIUniversalHandler(ViewerType<T> type, Component title, ItemStack... machines) {
        super(type, title, machines.length > 0 ? machines[0] : ItemStack.EMPTY, "gui_nei.png");
        this.machines = machines;
        this.slotFrame = Drawable.texture(tex("gui_nei.png"), 5, 87, 18, 18);
        this.machinePill = Drawable.texture(tex("gui_nei.png"), 59, 87, 18, 36);
    }

    /** One entry per input slot (each entry cycles through its alternatives). */
    protected abstract List<List<ItemStack>> inputs(T recipe);

    /** One entry per output slot. */
    protected abstract List<List<ItemStack>> outputs(T recipe);

    /** The chance output slot {@code slot} is made; below 1 it gets 1.7's red percentage line. */
    protected float outputChance(T recipe, int slot) {
        return 1F;
    }

    /** Extra tooltip lines on every input (1.7's {@code ItemStackUtil.addTooltipToStack} on the recipe's stacks). */
    protected void inputTooltip(T recipe, List<Component> tooltip) {}

    @Override
    public void setRecipe(SlotLayout builder, T recipe) {
        List<List<ItemStack>> in = inputs(recipe);
        List<List<ItemStack>> out = outputs(recipe);

        int[][] inPos = getInputCoords(in.size());
        for (int i = 0; i < in.size(); i++) {
            builder.addSlot(Role.INPUT, inPos[i][0], inPos[i][1])
                    .addItemStacks(in.get(i)).setBackground(slotFrame, -1, -1)
                    .addTooltipCallback((shown, tooltip) -> {
                        stackSizeLabel(shown, tooltip);
                        inputTooltip(recipe, tooltip);
                    });
        }

        int[][] outPos = getOutputCoords(out.size());
        for (int i = 0; i < out.size(); i++) {
            var slot = builder.addSlot(Role.OUTPUT, outPos[i][0], outPos[i][1])
                    .addItemStacks(out.get(i)).setBackground(slotFrame, -1, -1);
            float chance = outputChance(recipe, i);
            if (chance < 1F) {
                String percent = CustomMachineRecipeHandler.chanceText(chance);
                slot.addTooltipCallback((shown, tooltip) -> tooltip.add(Component.literal(percent).withStyle(ChatFormatting.RED)));
            }
        }

        List<ItemStack> machine = machine(recipe);
        if (!machine.isEmpty()) builder.addSlot(Role.CRAFTING_STATION, 75, 31).addItemStacks(machine);
    }

    /** 1.7 {@code ItemStackUtil.addStackSizeLabel}: counts over a stack spelled out in stacks of 64. */
    static void stackSizeLabel(ItemStack shown, List<Component> tooltip) {
        int count = shown.getCount();
        if (count <= 64) return;
        int rest = count % 64;
        tooltip.add(Component.literal(count / 64 + "x64" + (rest > 0 ? " + " + rest : "")).withStyle(ChatFormatting.RED));
    }

    /** The machine shown in the pill; 1.7's {@code machineOverrides} replace it per recipe. */
    protected List<ItemStack> machine(T recipe) {
        return List.of(machines);
    }

    @Override
    public void draw(T recipe, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        super.draw(recipe, g, mouseX, mouseY);
        machinePill.draw(g, 74, 14);
    }

    // 1.7's coordinate tables, item positions.
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
            default -> grid(count);
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
            default -> grid(count);
        };
    }

    private static int[][] grid(int count) {
        int[][] slots = new int[count][2];
        for (int i = 0; i < count; i++) slots[i] = new int[]{i % 4 * 18, i / 4 * 18};
        return slots;
    }
}
