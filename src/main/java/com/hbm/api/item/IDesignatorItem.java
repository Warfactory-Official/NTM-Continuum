package com.hbm.api.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * An item that can tell a launch pad where to aim: the 1.12.2 {@code IDesignatorItem}.
 *
 * <p>The launch pad's own coordinates are passed in because a designator is free to answer relative to
 * them (the artillery designators do) or to refuse based on where the pad is.
 */
public interface IDesignatorItem {

    /** Whether this designator currently holds a usable target. */
    boolean isReady(Level world, ItemStack stack, BlockPos padPos);

    /** The target. Only X and Z are read by a launch pad; Y is carried for the designators that set it. */
    Vec3 getCoords(Level world, ItemStack stack, BlockPos padPos);
}
