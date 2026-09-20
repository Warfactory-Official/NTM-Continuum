package com.hbm.items.tool;

import com.hbm.tileentity.machine.TileEntityLockableBase;
import net.minecraft.world.item.ItemStack;

/**
 * The key: an {@link ItemKeyPin} that opens any {@link TileEntityLockableBase} whose pins match
 * (1.7.10 {@code canAccess} checked {@code instanceof ItemKey}, so padlocks do not open locks).
 */
public class ItemKey extends ItemKeyPin implements TileEntityLockableBase.KeyPins {

    public ItemKey(Properties properties) {
        super(properties);
    }

    @Override
    public int getPins(ItemStack stack) {
        return pins(stack);
    }
}
