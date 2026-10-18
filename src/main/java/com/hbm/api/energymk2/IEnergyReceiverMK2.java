package com.hbm.api.energymk2;

/**
 * If it receives energy, use this. The whole contract is on {@link IEnergyHandlerMK2}; this says which
 * of the two roles the machine means to play.
 */
public interface IEnergyReceiverMK2 extends IEnergyHandlerMK2 {

    /** 1.7 {@code IEnergyReceiverMK2.getReceiverSpeed}: a receiver takes up to its capacity. */
    @Override
    default long getReceiverSpeed() {
        return this.getMaxPower();
    }
}
