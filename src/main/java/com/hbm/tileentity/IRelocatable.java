package com.hbm.tileentity;

import dev.golem.core.world.SubLevels;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

/**
 * A block entity that names other blocks by position (a multiblock controller, a wire's far end, a target) and is
 * told when a sub-level system (Create Aeronautics, through {@link SubLevels}) has carried it somewhere else. Its
 * saved data arrived at the new position verbatim, so every stored position still points at the old place.
 *
 * <p>The rule every implementation follows: a stored position moves with this block entity only when the block it
 * names was carried by the same {@link SubLevels.Move}. A position naming a block that stayed behind keeps pointing
 * at it, unless the link cannot survive the separation (a wire), in which case it is dropped.
 */
public interface IRelocatable {

    /** Called once, after the whole move, on this block entity at its new position. */
    void relocate(SubLevels.Move move);

    /** {@code pos} as it follows {@code move}: its new position when that block was carried, else itself. */
    static BlockPos follow(SubLevels.Move move, BlockPos pos) {
        return move.carried(pos) ? move.apply(pos) : pos;
    }

    /** As {@link #follow}, for a position that may be absent. */
    static @Nullable BlockPos followNullable(SubLevels.Move move, @Nullable BlockPos pos) {
        return pos == null ? null : follow(move, pos);
    }
}
