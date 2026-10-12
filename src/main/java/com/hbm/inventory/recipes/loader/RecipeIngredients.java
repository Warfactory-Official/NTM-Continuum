package com.hbm.inventory.recipes.loader;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.MaterialStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.ItemFluidTank;
import dev.golem.core.world.ItemStacks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The machine-recipe ingredient kinds beyond item, tag and autogen material ({@link GenericRecipeCodec#STACK} states
 * them with a {@code "type"}). Each is a 1.7 ingredient the port's tables used:
 * <ul>
 *   <li>{@link Exact}: 1.7 {@code new ComparableStack(ItemStack)}, the item with exactly this data (none, or
 *   {@link ItemData}); a plain item ingredient matches the item with any data.</li>
 *   <li>{@link Cold}: spent fuel that is not hot (PUREX; 1.7 matched the cooled metadata).</li>
 *   <li>{@link AnyOf}: any one of several ingredients (the DiFurnace's 1.7 {@code DictFrame} of a material's
 *   ingot, plate, gem and dust).</li>
 *   <li>{@link FluidItem}: a fluid container item holding a given fluid, at any fill (the DiFurnace's gasoline
 *   canister, 1.7's {@code canister_full} matched by fluid metadata only).</li>
 * </ul>
 */
public final class RecipeIngredients {

    private RecipeIngredients() {}

    /**
     * 1.7 {@code new ComparableStack(ItemStack)}: the item with exactly the given data; its match stack is built on first
     * use, since a row may not build an {@link ItemStack} while it is read.
     */
    public static final class Exact extends ComparableStack {

        public final ItemData data;
        private volatile ItemStack match;

        public Exact(Item item, int count, ItemData data) {
            super(item, count);
            this.data = data;
        }

        private ItemStack match() {
            ItemStack stack = match;
            if (stack == null) match = stack = data.apply(new ItemStack(item));
            return stack;
        }

        @Override
        public boolean matchesRecipe(ItemStack stack, boolean ignoreSize) {
            if (stack.isEmpty() || stack.getItem() != item || !ItemStacks.sameItemAndData(match(), stack)) return false;
            return ignoreSize || stack.getCount() >= stacksize;
        }

        @Override
        public List<ItemStack> getDisplayStacks() {
            return List.of(ItemStacks.copyWithCount(match(), stacksize));
        }
    }

    /** Spent fuel without the hot-state component (an explicit {@code false} is not cold); 1.7 PUREX's cooled waste. */
    public static final class Cold extends AStack {

        public final Item item;

        public Cold(Item item, int count) {
            this.item = item;
            this.stacksize = count;
        }

        @Override
        public boolean matchesRecipe(ItemStack stack, boolean ignoreSize) {
            return !stack.isEmpty() && stack.getItem() == item && !ModDataComponents.hasWasteHot(stack)
                    && (ignoreSize || stack.getCount() >= stacksize);
        }

        @Override
        public List<ItemStack> getDisplayStacks() {
            return List.of(new ItemStack(item, stacksize));
        }
    }

    /** Any one of several ingredients, each matched with its own count; displays them all, in order. */
    public static final class AnyOf extends AStack {

        public final List<AStack> choices;

        public AnyOf(List<AStack> choices, int count) {
            this.choices = List.copyOf(choices);
            this.stacksize = count;
        }

        @Override
        public boolean matchesRecipe(ItemStack stack, boolean ignoreSize) {
            for (AStack choice : choices) if (choice.matchesRecipe(stack, ignoreSize)) return true;
            return false;
        }

        @Override
        public List<ItemStack> getDisplayStacks() {
            List<ItemStack> display = new ArrayList<>();
            for (AStack choice : choices) display.addAll(choice.getDisplayStacks());
            return display;
        }
    }

    /** A fluid container item ({@link ItemFluidTank}) holding {@code fluid}, at any fill; shown full. */
    public static final class FluidItem extends AStack {

        public final Item item;
        public final FluidType fluid;

        public FluidItem(Item item, FluidType fluid, int count) {
            this.item = item;
            this.fluid = fluid;
            this.stacksize = count;
        }

        @Override
        public boolean matchesRecipe(ItemStack stack, boolean ignoreSize) {
            return !stack.isEmpty() && stack.getItem() == item && ItemFluidTank.getType(stack) == fluid
                    && (ignoreSize || stack.getCount() >= stacksize);
        }

        @Override
        public List<ItemStack> getDisplayStacks() {
            if (item instanceof ItemFluidTank tank) return List.of(ItemStacks.copyWithCount(tank.make(fluid, tank.capacity), stacksize));
            return List.of(new ItemStack(item, stacksize));
        }
    }

    /**
     * {@code stack} with another count, of the same kind (an ingredient's count is a mutable field, so tables that show
     * a row at another scale copy rather than change it).
     */
    public static AStack withCount(AStack stack, int count) {
        if (stack instanceof Exact exact) return new Exact(exact.item, count, exact.data);
        if (stack instanceof ComparableStack comparable) return new ComparableStack(comparable.item, count);
        if (stack instanceof TagStack tag) return new TagStack(tag.tag, count);
        if (stack instanceof MaterialStack material) return new MaterialStack(material.material, material.shape, count);
        if (stack instanceof Cold cold) return new Cold(cold.item, count);
        if (stack instanceof AnyOf any) return new AnyOf(any.choices, count);
        if (stack instanceof FluidItem fluid) return new FluidItem(fluid.item, fluid.fluid, count);
        throw new IllegalArgumentException("cannot rescale ingredient " + stack);
    }
}
