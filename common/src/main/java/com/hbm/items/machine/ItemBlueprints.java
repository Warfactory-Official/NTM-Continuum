package com.hbm.items.machine;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public class ItemBlueprints extends Item {

    public final String pool;

    public ItemBlueprints(Properties properties, String pool) {
        super(properties);
        this.pool = pool;
    }

    @Nullable
    public static String grabPool(ItemStack stack) {
        return stack.getItem() instanceof ItemBlueprints bp ? bp.pool : null;
    }
}
