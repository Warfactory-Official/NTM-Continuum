package com.hbm.inventory;

import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.special.ItemAutogen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class RecipesCommon {

    public static abstract class AStack {
        public int stacksize = 1;

        public AStack count(int n) {
            this.stacksize = n;
            return this;
        }

        public abstract boolean matchesRecipe(ItemStack stack, boolean ignoreSize);

        public abstract List<ItemStack> getDisplayStacks();

        public ItemStack getDisplayStack() {
            List<ItemStack> list = getDisplayStacks();
            return list.isEmpty() ? ItemStack.EMPTY : list.get(0);
        }

        public ItemStack getCyclingDisplayStack() {
            List<ItemStack> list = getDisplayStacks();
            if (list.isEmpty()) return ItemStack.EMPTY;
            if (list.size() == 1) return list.get(0);
            int idx = (int) (System.currentTimeMillis() % (CYCLE_MS * list.size()) / CYCLE_MS);
            return list.get(idx);
        }

        private static final long CYCLE_MS = 1000L;
    }

    public static class ComparableStack extends AStack {
        public final Item item;

        public ComparableStack(Item item) { this(item, 1); }
        public ComparableStack(Item item, int stacksize) { this.item = item; this.stacksize = stacksize; }

        @Override
        public boolean matchesRecipe(ItemStack stack, boolean ignoreSize) {
            if (stack.isEmpty() || stack.getItem() != item) return false;
            return ignoreSize || stack.getCount() >= stacksize;
        }

        @Override
        public List<ItemStack> getDisplayStacks() {
            return List.of(new ItemStack(item, stacksize));
        }
    }

    public static class TagStack extends AStack {
        public final TagKey<Item> tag;

        private List<ItemStack> displayCache;

        public TagStack(TagKey<Item> tag) { this(tag, 1); }
        public TagStack(TagKey<Item> tag, int stacksize) { this.tag = tag; this.stacksize = stacksize; }

        @Override
        public boolean matchesRecipe(ItemStack stack, boolean ignoreSize) {
            if (stack.isEmpty() || !stack.is(tag)) return false;
            return ignoreSize || stack.getCount() >= stacksize;
        }

        @Override
        public List<ItemStack> getDisplayStacks() {
            if (displayCache != null && !displayCache.isEmpty()) return displayCache;
            List<ItemStack> list = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                ItemStack s = new ItemStack(item, stacksize);
                if (s.is(tag)) list.add(s);
            }

            if (!list.isEmpty()) displayCache = list;
            return list;
        }
    }

    public static class MaterialStack extends AStack {
        public final NTMMaterial material;
        public final MaterialShapes shape;

        public MaterialStack(NTMMaterial material, MaterialShapes shape) { this(material, shape, 1); }
        public MaterialStack(NTMMaterial material, MaterialShapes shape, int stacksize) {
            this.material = material;
            this.shape = shape;
            this.stacksize = stacksize;
        }

        @Override
        public boolean matchesRecipe(ItemStack stack, boolean ignoreSize) {
            ItemAutogen item = ModAutogenItems.get(shape, material);
            if (item == null || stack.isEmpty() || stack.getItem() != item) return false;
            return ignoreSize || stack.getCount() >= stacksize;
        }

        @Override
        public List<ItemStack> getDisplayStacks() {
            ItemAutogen item = ModAutogenItems.get(shape, material);
            if (item == null) return List.of();
            return List.of(new ItemStack(item, stacksize));
        }
    }
}
