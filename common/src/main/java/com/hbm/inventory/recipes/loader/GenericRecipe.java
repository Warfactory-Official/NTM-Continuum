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
    public int duration;
    public long power;
    public String autoSwitchGroup = null;
    public String[] blueprintPools = null;
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

    /** The recipe's display name — the icon's item name (the original's customLocalization is deferred). */
    public String getLocalizedName() {
        ItemStack icon = getIcon();
        return icon.isEmpty() ? this.name : icon.getHoverName().getString();
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
        if (outputItem != null) for (ItemStack out : outputItem) {
            list.add(Component.literal("  " + out.getCount() + "x ")
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
