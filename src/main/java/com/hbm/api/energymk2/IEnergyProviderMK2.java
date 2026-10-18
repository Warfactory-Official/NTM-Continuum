package com.hbm.api.energymk2;

/** If it sends energy, use this. The contract is on {@link IEnergyHandlerMK2}; this adds provision. */
public interface IEnergyProviderMK2 extends IEnergyHandlerMK2 {

    /** 1.7 {@code IEnergyProviderMK2.getProviderSpeed}: a provider sends up to its capacity. */
    @Override
    default long getProviderSpeed() {
        return this.getMaxPower();
    }
}
