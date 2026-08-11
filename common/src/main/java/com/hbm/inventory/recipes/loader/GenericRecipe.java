package com.hbm.inventory.recipes.loader;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.items.machine.ItemFluidIcon;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One machine recipe, ported (item-only) from HBM's {@code GenericRecipe}. Inputs are {@link AStack}s
 * (exact item or tag); outputs are plain {@link ItemStack}s (the original's chance/multi-output and
 * fluid I/O are deferred — see IMPLEMENTATION.md §18). A recipe may belong to one or more
 * <b>blueprint pools</b> (which template unlocks it) and an <b>auto-switch group</b> (machines pick the
 * matching recipe in the group from the inserted ingredient).
 */
public class GenericRecipe {

    public final String name;
    public AStack[] inputItem;
    public ItemStack[] outputItem;
    public FluidStack[] inputFluid;
    public FluidStack[] outputFluid;
    /**
     * Per-output drop chance, index-aligned with {@link #outputItem}; a missing entry or {@code 0} means
     * guaranteed. This is the port's stand-in for the original's {@code ChanceOutput} wrapper — a parallel
     * array keeps every existing {@code outputItems(ItemStack...)} call site untouched. As in the original,
     * the roll happens per <i>item</i> in the stack, so a 50% ×4 output averages two items.
     */
    public float[] outputChance;
    public int duration;
    public long power;
    public String autoSwitchGroup = null;
    public String[] blueprintPools = null;
    /** Translation key wrapping the icon's name for display, e.g. {@code purex.recycle} = "Reprocessing of %s". */
    public String nameWrapper = null;
    private ItemStack icon = ItemStack.EMPTY;

    public GenericRecipe(String name) {
        this.name = name;
    }

    public GenericRecipe setup(int duration, long power) {
        this.duration = duration;
        this.power = power;
        return this;
    }

    public GenericRecipe inputItems(AStack... input) {
        this.inputItem = input;
        return this;
    }

    public GenericRecipe outputItems(ItemStack... output) {
        this.outputItem = output;
        return this;
    }

    /** Drop chances (0..1) for the outputs set by {@link #outputItems}, in the same order. */
    public GenericRecipe outputChances(float... chances) {
        this.outputChance = chances;
        return this;
    }

    public GenericRecipe inputFluids(FluidStack... input) {
        this.inputFluid = input;
        return this;
    }

    public GenericRecipe outputFluids(FluidStack... output) {
        this.outputFluid = output;
        return this;
    }

    public GenericRecipe setPools(String... pools) {
        this.blueprintPools = pools;
        for (String pool : pools) GenericRecipes.addToPool(pool, this);
        return this;
    }

    public GenericRecipe setGroup(String autoSwitch, GenericRecipes set) {
        this.autoSwitchGroup = autoSwitch;
        set.addToGroup(autoSwitch, this);
        return this;
    }

    public GenericRecipe setIcon(ItemStack icon) {
        this.icon = icon;
        return this;
    }

    public GenericRecipe setNameWrapper(String key) {
        this.nameWrapper = key;
        return this;
    }

    /** The chance of output {@code index} dropping, 1 when no chance table applies. */
    public float getChance(int index) {
        if (outputChance == null || index >= outputChance.length || outputChance[index] <= 0F) return 1F;
        return Math.min(outputChance[index], 1F);
    }

    public boolean isPooled() { return blueprintPools != null; }

    public boolean isPartOfPool(String lookingFor) {
        if (!isPooled() || lookingFor == null) return false;
        for (String pool : blueprintPools) if (pool.equals(lookingFor)) return true;
        return false;
    }

    public String getInternalName() { return name; }

    public ItemStack getIcon() {
        if (!icon.isEmpty()) return icon;
        if (outputItem != null && outputItem.length > 0) return outputItem[0];
        // No item output → represent the recipe by its first output (or input) fluid's droplet icon.
        if (outputFluid != null && outputFluid.length > 0) return ItemFluidIcon.make(outputFluid[0]);
        if (inputFluid != null && inputFluid.length > 0) return ItemFluidIcon.make(inputFluid[0]);
        return ItemStack.EMPTY;
    }

    /**
     * The recipe's display name: the icon's item name, wrapped by {@link #nameWrapper} when set (the
     * original's {@code nameWrapper}, e.g. "Reprocessing of Spent MOX").
     */
    public String getLocalizedName() {
        ItemStack icon = getIcon();
        String base = icon.isEmpty() ? this.name : icon.getHoverName().getString();
        return nameWrapper == null ? base : Component.translatable(nameWrapper, base).getString();
    }

    /**
     * Multi-line recipe description for the GUI tooltip (name, duration, power, inputs, outputs). Ported
     * from HBM's {@code GenericRecipe.print()} — item-only (fluids/chance outputs are deferred).
     */
    public List<Component> print() {
        List<Component> list = new ArrayList<>();
        list.add(Component.literal(getLocalizedName()).withStyle(ChatFormatting.YELLOW));

        if (duration > 0) {
            list.add(Component.translatable("gui.recipe.duration")
                    .append(": " + (duration / 20D) + "s").withStyle(ChatFormatting.RED));
        }
        if (power > 0) {
            list.add(Component.translatable("gui.recipe.consumption")
                    .append(": " + power + "HE/t").withStyle(ChatFormatting.RED));
        }

        list.add(Component.translatable("gui.recipe.input").append(":").withStyle(ChatFormatting.BOLD));
        if (inputItem != null) for (AStack stack : inputItem) {
            ItemStack display = stack.getDisplayStack();
            list.add(Component.literal("  " + display.getCount() + "x ")
                    .append(display.getHoverName()).withStyle(ChatFormatting.GRAY));
        }
        if (inputFluid != null) for (FluidStack fluid : inputFluid) list.add(fluidLine(fluid));

        list.add(Component.translatable("gui.recipe.output").append(":").withStyle(ChatFormatting.BOLD));
        if (outputItem != null) for (int i = 0; i < outputItem.length; i++) {
            ItemStack out = outputItem[i];
            float chance = getChance(i);
            String prefix = chance >= 1F ? "  " : "  " + Math.round(chance * 100F) + "% ";
            list.add(Component.literal(prefix + out.getCount() + "x ")
                    .append(out.getHoverName()).withStyle(ChatFormatting.GRAY));
        }
        if (outputFluid != null) for (FluidStack fluid : outputFluid) list.add(fluidLine(fluid));

        return list;
    }

    private static Component fluidLine(FluidStack fluid) {
        String text = "  " + fluid.fill + " mB " + fluid.type.getName()
                + (fluid.pressure != 0 ? " @ " + fluid.pressure + " PU" : "");
        return Component.literal(text).withStyle(ChatFormatting.BLUE);
    }

    /** Whether this recipe's localized name contains {@code substring} (case-insensitive). */
    public boolean matchesSearch(String substring) {
        return getLocalizedName().toLowerCase(Locale.US).contains(substring.toLowerCase(Locale.US));
    }
}
