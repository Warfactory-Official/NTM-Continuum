package com.hbm.inventory;

import dev.golem.core.world.ItemStacks;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code api.hbm.ntl.StackCache}: all combined slots available to this endpoint. Each endpoint (an access
 * terminal or an automation output) has one stack cache, regularly updated so it knows what stacks it can access.
 */
public class StackCache {

    /**
     * 1.7 keyed a type by a hash of (item, meta, nbt), re-derived to re-validate a monitor, so a collision merged
     * two types into one slot and served a request the other one. The key here is the stack itself.
     */
    public static final Hash.Strategy<ItemStack> TYPE_AND_COMPONENTS = new Hash.Strategy<>() {

        @Override
        public int hashCode(ItemStack stack) {
            return stack == null ? 0 : ItemStacks.hashItemAndData(stack);
        }

        @Override
        public boolean equals(ItemStack a, ItemStack b) {
            if (a == b) return true;
            if (a == null || b == null) return false;
            if (a.isEmpty() || b.isEmpty()) return a.isEmpty() == b.isEmpty();
            return ItemStacks.sameItemAndData(a, b);
        }
    };

    public final BlockPos pos;
    /** Maps a type to the actual cache slot; the EMPTY key is 1.7's null identity, the cache of empty slots. */
    public final Object2ObjectLinkedOpenCustomHashMap<ItemStack, CacheSlot> cacheSlots =
            new Object2ObjectLinkedOpenCustomHashMap<>(TYPE_AND_COMPONENTS);
    public boolean hasExpired = false;

    public StackCache(BlockPos pos) {
        this.pos = pos.immutable();
    }

    public void addToCache(SlotMonitor monitor) {
        CacheSlot cache = cacheSlots.get(monitor.stack);

        if (cache == null) {
            cache = new CacheSlot(monitor.stack);
            cacheSlots.put(cache.displayStack, cache);
        }

        cache.addMonitor(monitor);
    }

    /** The slot holding this type, or null. An EMPTY argument answers with the cache of empty slots. */
    public CacheSlot getSlotFromStack(ItemStack stack) {
        return cacheSlots.get(stack);
    }

    /**
     * Uses up items and returns how many of the requested items could be removed; with no desyncs that number
     * should always be equal to the supplied amount.
     */
    public long consumeItemsAndReturnQuantity(ItemStack stack, long amount) {
        CacheSlot cache = this.cacheSlots.get(stack);
        if (cache == null) return 0;
        long originalAmount = amount;

        for (SlotMonitor monitor : cache.monitors.toArray(new SlotMonitor[0])) {
            if (!TYPE_AND_COMPONENTS.equals(monitor.parent.getSlotAt(monitor.index), stack)) continue;
            amount = monitor.parent.useUpItem(monitor.index, amount);
            if (amount <= 0) break;
        }

        return originalAmount - amount;
    }

    /** Adds a stack to the system and returns the amount that did not fit. */
    public long addItemsAndReturnQuantity(ItemStack stack, long amount) {
        CacheSlot cache = this.cacheSlots.get(stack);

        if (cache != null) for (SlotMonitor monitor : cache.monitors.toArray(new SlotMonitor[0])) {
            if (!TYPE_AND_COMPONENTS.equals(monitor.parent.getSlotAt(monitor.index), stack)) continue;
            amount = monitor.parent.addItem(monitor.index, amount);
            if (amount <= 0) break;
        }

        if (amount > 0) {
            CacheSlot nullCache = this.cacheSlots.get(ItemStack.EMPTY);
            if (nullCache != null) { // quite ironic, isn't it?
                for (SlotMonitor monitor : nullCache.monitors.toArray(new SlotMonitor[0])) {
                    if (!monitor.parent.allowTypeSetting()) continue;
                    if (!monitor.parent.getSlotAt(monitor.index).isEmpty()) continue;
                    amount = monitor.parent.setupType(monitor.index, stack, amount);
                    if (amount <= 0) break;
                }
            }
        }

        return amount;
    }

    public void dissolveCache() {
        for (CacheSlot cacheSlot : cacheSlots.values()) {
            cacheSlot.destroy();
        }
        this.cacheSlots.clear();
        this.hasExpired = true;
    }

    /**
     * A cache slot represents multiple accessible slots combined into one by type; in essence a single slot with
     * an uncapped max stack size, which references multiple slot monitor instances to figure out its total.
     */
    public class CacheSlot {

        /** Also this slot's key in {@link #cacheSlots}; never mutated. */
        public final ItemStack displayStack;
        public final ObjectLinkedOpenHashSet<SlotMonitor> monitors = new ObjectLinkedOpenHashSet<>();
        public long stacksize;

        public CacheSlot(ItemStack stack) {
            this.displayStack = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
            this.stacksize = 0;
        }

        public void addMonitor(SlotMonitor monitor) {
            if (this.monitors.add(monitor)) {
                monitor.viewedBy.add(this);
                this.changeAmounts(monitor.stacksize);
            }
        }

        public void removeMonitor(SlotMonitor monitor) {
            if (this.monitors.remove(monitor)) {
                this.changeAmounts(-monitor.stacksize);
            }
        }

        public void destroy() {
            for (SlotMonitor monitor : monitors) {
                monitor.viewedBy.remove(this);
            }
            this.stacksize = 0;
        }

        public void changeAmounts(long delta) {
            this.stacksize += delta;
        }

        public StackCache getStackCache() {
            return StackCache.this;
        }
    }
}
