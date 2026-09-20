package com.hbm.inventory;

import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import com.hbm.uninos.networkproviders.PneumaticNetwork;

/**
 * 1.7 {@code api.hbm.ntl.ISlotMonitorProvider}: storage tile entities provide the access terminals with slot
 * monitors, and slot monitors with ways of accessing the underlying stacks.
 */
public interface ISlotMonitorProvider {

    /** An array of available slot monitors, which should ideally mirror the available slots of that container. */
    SlotMonitor[] getMonitors();

    /** The ORIGINAL ItemStack of that index, so the monitors can detect changes. */
    ItemStack getSlotAt(int index);

    /** The amount of that slot; some storages use int64, which ItemStacks cannot hold. */
    long getAmountAt(int index);

    /** Removes items from that slot; returns the amount left to remove if the stack was smaller. */
    long useUpItem(int index, long amount);

    /** Adds items to that slot; returns the amount that couldn't be added due to stack limits. */
    long addItem(int index, long amount);

    /** Sets the slot contents, returns the number of items that couldn't be added. */
    long setupType(int index, ItemStack zeroStack, long amount);

    /** Whether this container allows types to be set via the access terminal. */
    boolean allowTypeSetting();

    /** Whether this storage unit is reachable by the access point. */
    boolean isAvailableToCache(StackCache cache);

    /** Lets slot monitors find the network, and by extension all cached slots. */
    @Nullable PneumaticNetwork getRelevantNetwork();

    /** Runs when a new stack cache user (i.e. an access point) joins the network, to grab all the stack monitors. */
    default void onNewCacheHasJoined(StackCache stackCache, PneumaticNetwork network) {
        for (SlotMonitor monitor : getMonitors()) {
            if (!stackCache.hasExpired && isAvailableToCache(stackCache)) {
                stackCache.addToCache(monitor);
            }
        }
    }

    default void updateMonitors() {
        PneumaticNetwork net = getRelevantNetwork();
        for (SlotMonitor monitor : getMonitors()) monitor.checkUpdate(net);
    }
}
