package com.hbm.api.fluidmk2;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import org.jspecify.annotations.NonNull;

/** {@link IFluidProviderMK2} backed by the tile's own sending tanks. */
public interface IFluidStandardSenderMK2 extends IFluidProviderMK2 {

    @NonNull FluidTankNTM[] getSendingTanks();

    @Override
    default long getFluidAvailable(FluidType type, int pressure) {
        long amount = 0;
        for (FluidTankNTM tank : getSendingTanks()) {
            if (tank.provides(type) && tank.getPressure() == pressure) amount += tank.getFill();
        }
        return amount;
    }

    @Override
    default void useUpFluid(FluidType type, int pressure, long amount) {
        int tanks = 0;
        for (FluidTankNTM tank : getSendingTanks()) {
            if (tank.provides(type) && tank.getPressure() == pressure) tanks++;
        }
        if (tanks > 1) {
            int firstRound = (int) Math.floor((double) amount / (double) tanks);
            for (FluidTankNTM tank : getSendingTanks()) {
                if (tank.provides(type) && tank.getPressure() == pressure) {
                    int toRem = Math.min(firstRound, tank.getFill());
                    tank.setFill(tank.getFill() - toRem);
                    amount -= toRem;
                }
            }
        }
        if (amount > 0) for (FluidTankNTM tank : getSendingTanks()) {
            if (tank.provides(type) && tank.getPressure() == pressure) {
                int toRem = (int) Math.min(amount, tank.getFill());
                tank.setFill(tank.getFill() - toRem);
                amount -= toRem;
            }
        }
    }

    @Override
    default int[] getProvidingPressureRange(FluidType type) {
        int lowest = HIGHEST_VALID_PRESSURE;
        int highest = 0;

        for (FluidTankNTM tank : getSendingTanks()) {
            if (tank.provides(type)) {
                if (tank.getPressure() < lowest) lowest = tank.getPressure();
                if (tank.getPressure() > highest) highest = tank.getPressure();
            }
        }

        return lowest <= highest ? new int[]{lowest, highest} : DEFAULT_PRESSURE_RANGE;
    }

    @Override
    default long getProviderSpeed(FluidType type, int pressure) {
        return 1_000_000_000;
    }
}
