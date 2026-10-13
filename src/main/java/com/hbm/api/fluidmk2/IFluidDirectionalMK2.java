package com.hbm.api.fluidmk2;

import com.hbm.lib.ForgeDirection;

/**
 * A fluid user whose role depends on which of its sides the net reaches it through. 1.7 expressed this
 * with two different calls: {@code trySubscribe} on the intake side makes the tile a receiver of that
 * net only, {@code tryProvide} on the outlet side a provider only. The flow-control pump is the one
 * machine built entirely on that asymmetry. The port's net discovers users by probing faces, so the
 * face is known at discovery and this is where the pump says which role each face carries.
 *
 * <p>{@code side} is the user's own side the duct touches ({@link ForgeDirection#UNKNOWN} for a cell's
 * pseudo-face). A user that does not implement this is offered in every role it has on every face.
 */
public interface IFluidDirectionalMK2 extends IFluidUserMK2 {

    boolean providesOn(ForgeDirection side);

    boolean receivesOn(ForgeDirection side);
}
