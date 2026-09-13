package com.hbm.api.energymk2;

/**
 * If it receives energy, use this. The whole contract is on {@link IEnergyHandlerMK2}; this says which
 * of the two roles the machine means to play.
 *
 * <p>There is nothing to subscribe to any more. 1.7 had every receiver push itself into the net of each
 * adjacent conductor on every tick, and a net that forgot a receiver three seconds later; the graph
 * walks the faces of its own cells instead and asks what is on the other side, so a machine is found by
 * standing next to a cable and for no other reason. A face that wants nothing this round says so by
 * answering zero to {@link IEnergyHandlerMK2#getReceiverSpeed()}.
 */
public interface IEnergyReceiverMK2 extends IEnergyHandlerMK2 {

}
