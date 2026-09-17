package com.hbm.entity.cart;

import com.hbm.items.tool.ItemModMinecart.EnumCartBase;
import net.minecraft.world.item.ItemStack;

/**
 * What both cart halves answer. 1.7 got this from a single base class; 26.2 splits plain and container
 * minecarts across two vanilla supertypes, so the shared surface is an interface instead.
 */
public interface ICartChassis {

    EnumCartBase getBase();

    /** The assembled item this cart hands back when broken. */
    ItemStack getCartItem();
}
