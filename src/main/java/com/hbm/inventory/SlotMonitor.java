package com.hbm.inventory;

import com.hbm.inventory.StackCache.CacheSlot;
import dev.golem.core.world.ItemStacks;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import com.hbm.uninos.networkproviders.PneumaticNetwork;

/**
 * 1.7 {@code api.hbm.ntl.SlotMonitor}: the access points of the storage system to the actual item stacks. Each
 * storage unit provides all its stacks as monitors, whose main job is detecting changes in the underlying stack
 * so the system's wider data structures can be notified.
 */
public class SlotMonitor {

    /** The index of the slot in the inventory this monitor... monitors. */
    public final int index;
    public final ISlotMonitorProvider parent;

    /** If this monitor detects a change, all cache slots need to be notified. */
    public final ObjectLinkedOpenHashSet<CacheSlot> viewedBy = new ObjectLinkedOpenHashSet<>();

    /** 1.7 kept (item, meta, nbt); all three fold into a single count-1 "zero stack" here. */
    public ItemStack stack = ItemStack.EMPTY;
    public long stacksize;

    protected boolean hasAvailabilityChanged;
    protected boolean forceTypeUpdate;

    public SlotMonitor(int index, ISlotMonitorProvider parent) {
        this.hasAvailabilityChanged = true;
        this.forceTypeUpdate = true;
        this.index = index;
        this.parent = parent;
    }

    /**
     * Monitor providers need to keep track of whether availability has changed, i.e. compair has run out,
     * compression setting has changed, etc. This means availability is not re-checked every single tick.
     */
    public void availabilityHasChanged() {
        this.hasAvailabilityChanged = true;
    }

    /** 1.7 resolves the net per monitor; here that is a saved-data lookup, so the caller resolves it once per inventory. */
    public void checkUpdate(@Nullable PneumaticNetwork pneumoNet) {

        if (hasAvailabilityChanged) {

            if (pneumoNet != null) {
                for (StackCache cache : pneumoNet.accessors) {
                    if (!cache.hasExpired && parent.isAvailableToCache(cache)) {
                        cache.addToCache(this);
                    }
                }
            }

            // if this monitor is not available to some caches, remove them
            Iterator<CacheSlot> iterator = viewedBy.iterator();
            while (iterator.hasNext()) {
                CacheSlot slot = iterator.next();
                StackCache cache = slot.getStackCache();
                if (cache.hasExpired || !parent.isAvailableToCache(cache)) {
                    slot.removeMonitor(this);
                    iterator.remove();
                }
            }

            hasAvailabilityChanged = false;
        }

        ItemStack live = parent.getSlotAt(index);
        long amount = parent.getAmountAt(index);

        boolean hasTypeChanged = live.isEmpty() != this.stack.isEmpty()
                || (!live.isEmpty() && !ItemStacks.sameItemAndData(live, this.stack));

        if (hasTypeChanged || forceTypeUpdate) {

            // remove from all existing cache slots
            Iterator<CacheSlot> iterator = viewedBy.iterator();
            while (iterator.hasNext()) {
                CacheSlot slot = iterator.next();
                slot.removeMonitor(this);
                iterator.remove();
            }

            // set updated traits
            if (live.isEmpty()) {
                this.stack = ItemStack.EMPTY;
                this.stacksize = 0;
            } else {
                this.stack = live.copyWithCount(1);
                this.stacksize = amount;
            }

            // find new cache slots
            if (pneumoNet != null) {
                for (StackCache cache : pneumoNet.accessors) {
                    if (!cache.hasExpired && parent.isAvailableToCache(cache)) {
                        cache.addToCache(this);
                    }
                }
            }

            forceTypeUpdate = false;
            return;
        }

        if (stacksize != amount) {
            long delta = amount - stacksize;
            for (CacheSlot slot : viewedBy) slot.changeAmounts(delta);

            this.stacksize = amount;
        }
    }
}
