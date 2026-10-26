package com.hbm.inventory;

import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.special.ItemAutogen;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Recipe ingredient matching, ported (and trimmed for 26.1) from HBM's {@code RecipesCommon}. An
 * {@link AStack} is an abstract "ingredient stack" the recipe system matches against real
 * {@link ItemStack}s. The 1.12.2 ore dictionary is replaced by item {@link TagKey tags}
 * ({@link TagStack}); {@link ComparableStack} matches an exact item and can retain its component/NBT
 * data for metadata variants. The legacy JEI display helpers are represented by {@link AStack#getDisplayStacks()}.
 */
public class RecipesCommon {

    public static abstract class AStack {
        public int stacksize = 1;

        /** Fluent setter for the required count, so one ingredient consumes {@code n} from a single stack
         * (e.g. {@code mat.plate().count(3)} = 3 plates in ONE slot, not 3 separate inputs). */
        public AStack count(int n) {
            this.stacksize = n;
            return this;
        }

        /** @param ignoreSize match by item/tag only (for slot validation), else also require enough count. */
        public abstract boolean matchesRecipe(ItemStack stack, boolean ignoreSize);

        /** Every real stack this ingredient matches (one item for {@link ComparableStack}, all tag members
         * for {@link TagStack}), for ghost-slot / icon display. */
        public abstract List<ItemStack> getDisplayStacks();

        /** A single representative stack (the first match). */
        public ItemStack getDisplayStack() {
            List<ItemStack> list = getDisplayStacks();
            return list.isEmpty() ? ItemStack.EMPTY : list.get(0);
        }

        /**
         * The display stack <b>cycling</b> through all matches over time (so a tag ingredient blinks between
         * e.g. the vanilla and HBM copper ingots). This is the 26.1 port of the original {@code extractForCyclingDisplay}.
         * Switches every {@code CYCLE_MS} via the wall clock.
         */
        public ItemStack getCyclingDisplayStack() {
            List<ItemStack> list = getDisplayStacks();
            if (list.isEmpty()) return ItemStack.EMPTY;
            if (list.size() == 1) return list.get(0);
            int idx = (int) (System.currentTimeMillis() % (CYCLE_MS * list.size()) / CYCLE_MS);
            return list.get(idx);
        }

        /** Milliseconds each cycling-display variant is shown (≈ the original's {@code cycle*50}). */
        private static final long CYCLE_MS = 1000L;
    }

    /** Matches one exact {@link Item}, optionally including its component/NBT data. */
    public static class ComparableStack extends AStack {
        public final Item item;
        private final ItemStack matchStack;

        public ComparableStack(Item item) {
            this(item, 1);
        }

        public ComparableStack(Item item, int stacksize) {
            this.item = item;
            this.stacksize = stacksize;
            this.matchStack = null;
        }

        /** Matches an exact stack, retaining the old item-data distinction for metadata variants. */
        public ComparableStack(ItemStack stack) {
            this.item = stack.getItem();
            this.stacksize = stack.getCount();
            this.matchStack = stack.copy();
        }

        /** True when this matches the exact item data of a stack (made from an {@code ItemStack}), not the item alone. */
        public boolean exactData() {
            return matchStack != null;
        }

        @Override
        public boolean matchesRecipe(ItemStack stack, boolean ignoreSize) {
            if (stack.isEmpty() || stack.getItem() != item) return false;
            if (matchStack != null && !ItemStacks.sameItemAndData(matchStack, stack)) return false;
            return ignoreSize || stack.getCount() >= stacksize;
        }

        @Override
        public List<ItemStack> getDisplayStacks() {
            return List.of(matchStack == null
                    ? new ItemStack(item, stacksize)
                    : ItemStacks.copyWithCount(matchStack, stacksize));
        }
    }

    /** Shared tag matching and cycling display for dictionary and material ingredients. */
    public static abstract class TagIngredient extends AStack {
        public final TagKey<Item> tag;

        /** Cached tag membership for display, resolved lazily once the tag is non-empty (tags load with
         * the world; ghost slots reuse the resolved members rather than allocating stacks every frame). */
        private List<ItemStack> displayCache;

        protected TagIngredient(TagKey<Item> tag, int stacksize) {
            this.tag = tag;
            this.stacksize = stacksize;
        }

        @Override
        public boolean matchesRecipe(ItemStack stack, boolean ignoreSize) {
            if (stack.isEmpty() || !stack.is(tag)) return false;
            return ignoreSize || stack.getCount() >= stacksize;
        }

        @Override
        public List<ItemStack> getDisplayStacks() {
            if (displayCache != null && !displayCache.isEmpty()) return displayCache;
            List<ItemStack> list = new ArrayList<>();
            for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                list.add(new ItemStack(holder.value(), stacksize));
            }
            // Only cache once non-empty. Before the world (and its tags) load, there are no members.
            if (!list.isEmpty()) displayCache = list;
            return list;
        }
    }

    /** Matches any item in a {@link TagKey} (the ore-dictionary replacement). */
    public static class TagStack extends TagIngredient {
        public TagStack(TagKey<Item> tag) { this(tag, 1); }
        public TagStack(TagKey<Item> tag, int stacksize) { super(tag, stacksize); }
    }

    /**
     * A material/shape declaration with the same tag-wide acceptance as 1.7's ore-dictionary ingredients.
     * Its typed identity is retained for recipe data and generated row names; cycling displays use all tag members.
     */
    public static class MaterialStack extends TagIngredient {
        public final NTMMaterial material;
        public final MaterialShapes shape;

        public MaterialStack(NTMMaterial material, MaterialShapes shape) { this(material, shape, 1); }
        public MaterialStack(NTMMaterial material, MaterialShapes shape, int stacksize) {
            super(material.tag(shape), stacksize);
            this.material = material;
            this.shape = shape;
        }

        /** Stable owned prototype for generated row names, which are resolved before live tags load. */
        @Override
        public ItemStack getDisplayStack() {
            ItemAutogen item = ModAutogenItems.get(shape, material);
            return item == null ? ItemStack.EMPTY : new ItemStack(item, stacksize);
        }
    }
}
