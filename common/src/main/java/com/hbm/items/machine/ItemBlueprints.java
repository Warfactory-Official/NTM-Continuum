package com.hbm.items.machine;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * A machine template ("blueprint") — inserting it into a machine unlocks the recipes in its
 * {@link #pool}. The 1.12.2 mod used one item with the pool stored in NBT; for the first functional
 * pass each template is its own item with a fixed pool (a clean subset — multi-pool NBT blueprints can
 * follow). {@link #grabPool} reads the pool from a stack.
 */
public class ItemBlueprints extends Item {

    public final String pool;

    public ItemBlueprints(Properties properties, String pool) {
        super(properties);
        this.pool = pool;
    }

    /** The recipe pool a blueprint stack unlocks, or {@code null} if it isn't a blueprint. */
    @Nullable
    public static String grabPool(ItemStack stack) {
        return stack.getItem() instanceof ItemBlueprints bp ? bp.pool : null;
    }
}
