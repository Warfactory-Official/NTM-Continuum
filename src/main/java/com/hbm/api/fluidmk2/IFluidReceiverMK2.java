package com.hbm.api.fluidmk2;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.inventory.fluid.FluidType;
import org.jetbrains.annotations.Contract;

/** A tile that draws fluid from a network. */
public interface IFluidReceiverMK2 extends IFluidUserMK2 {

    /**
     * Sends fluid of the desired type and pressure to the receiver, returns the remainder that couldn't
     * be accepted.
     */
    @Contract(mutates = "this")
    long transferFluid(FluidType type, int pressure, long amount);

    default long getReceiverSpeed(FluidType type, int pressure) { return 1_000_000_000; }

    @Contract(pure = true)
    long getDemand(FluidType type, int pressure);

    default int[] getReceivingPressureRange(FluidType type) { return DEFAULT_PRESSURE_RANGE; }

    default IEnergyReceiverMK2.ConnectionPriority getFluidPriority() {
        return IEnergyReceiverMK2.ConnectionPriority.NORMAL;
    }
}
