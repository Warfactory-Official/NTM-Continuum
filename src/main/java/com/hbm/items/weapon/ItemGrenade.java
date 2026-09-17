package com.hbm.items.weapon;

import net.minecraft.world.item.Item;
import dev.golem.core.world.CompatItem;

/**
 * 1.7 ItemGrenade: the fuse (in seconds) carrier shared by the non-modular thrown grenades.
 */
public class ItemGrenade extends CompatItem {

    public final int fuse;

    public ItemGrenade(Properties properties, int fuse) {
        super(properties);
        this.fuse = fuse;
    }
}
