package com.hbm.inventory;

import dev.golem.core.world.ItemStacks;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/**
 * A machine-recipe ingredient for 1.7's {@code new OreDictStack(Fluids.X.getDict(amount))}: any full container
 * of that fluid and amount in {@link FluidContainerRegistry}. Machines consume the whole container, as 1.7's
 * {@code ModuleMachineBase.consumeInput} decremented the stack without asking for a container item.
 */
public class FluidContainerStack extends RecipesCommon.AStack {

    private final String fluid;
    private final int amount;

    public FluidContainerStack(String fluid, int amount) {
        this.fluid = fluid;
        this.amount = amount;
    }

    /** The 1.7 fluid name ({@code "LUBRICANT"}), for the recipe file form. */
    public String fluid() {
        return fluid;
    }

    /** The amount a matching container holds, in mB. */
    public int amount() {
        return amount;
    }

    @Override
    public boolean matchesRecipe(ItemStack stack, boolean ignoreSize) {
        return stack != null && !stack.isEmpty() && FluidContainerRegistry.INSTANCE.holds(stack, fluid, amount)
                && (ignoreSize || stack.getCount() >= stacksize);
    }

    @Override
    public String toString() {
        return "fluid_container:" + fluid + "/" + amount;
    }

    @Override
    public List<ItemStack> getDisplayStacks() {
        return FluidContainerRegistry.INSTANCE.examples(fluid, amount).stream()
                .map(stack -> ItemStacks.copyWithCount(stack, stacksize)).toList();
    }
}
