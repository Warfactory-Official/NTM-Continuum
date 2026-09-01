package com.hbm.items.tool;

import com.hbm.api.block.IToolable;

/**
 * The ordinary 1.12.2 defuser item. Its block-tool behavior is inherited from {@link ItemTooling}.
 * The old creeper and nuclear-glyphid entity interaction remains deferred with those entity systems.
 */
public final class ItemDefuser extends ItemTooling {

    public ItemDefuser(Properties properties) {
        super(properties, IToolable.ToolType.DEFUSER);
    }
}
