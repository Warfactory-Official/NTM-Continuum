package com.hbm.api.fluid;

/** A fluid tank as the fluid contracts see it. */
public interface FluidTank {

    FluidType getTankType();

    int getFill();

    void setFill(int fill);

    int getMaxFill();

    int getPressure();
}
