package com.hbm.api.fluidmk2;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/**
 * A machine with more than one fluid user. 1.7's networks were subscriptions: a tile could subscribe a second
 * receiver object at some of its connection points (hbm-space's transporter subscribes a {@code FuelReceiver} whose
 * receiving tanks are the fuel tanks at {@code getTankPos}, the transporter itself, receiving into the cargo tanks, at
 * {@code getConPos}), and each subscribed object was served on its own. The port's networks discover users by probing
 * faces and find the machine's block entity, so the machine says here which of its users a face reaches.
 *
 * <p>Implemented by the block entity a duct resolves to (the core of a folded multiblock). Each distinct user a net
 * reaches gets its own share of the round, as each subscribed receiver did in 1.7.
 */
public interface IFluidMultiUserMK2 {

    /**
     * The user a duct meets at {@code pos} (the machine's own block or one of its footprint cells) through its
     * {@code face} side ({@code null}: the cell's own pseudo-face), or {@code null} for the block entity itself.
     */
    @Nullable IFluidUserMK2 fluidUserAt(BlockPos pos, @Nullable Direction face);

    /** The user a face of {@code be} at {@code pos} reaches: its {@link #fluidUserAt} answer, else {@code be} itself. */
    static @Nullable IFluidUserMK2 resolve(Object be, BlockPos pos, @Nullable Direction face) {
        if (be instanceof IFluidMultiUserMK2 multi) {
            IFluidUserMK2 user = multi.fluidUserAt(pos, face);
            if (user != null) return user;
        }
        return be instanceof IFluidUserMK2 user ? user : null;
    }
}
