package com.hbm.inventory.fluid;

import com.hbm.api.fluid.FluidType;
/**
 * An amount of a {@link FluidType} at a given pressure. Ported verbatim from 1.12.2.
 *
 * @author hbm
 */
public class FluidStack {

    public FluidType type;
    public int fill;
    public int pressure;

    public FluidStack(int fill, FluidType type) {
        this.fill = fill;
        this.type = type;
    }

    public FluidStack(FluidType type, int fill) {
        this(type, fill, 0);
    }

    public FluidStack(FluidType type, int fill, int pressure) {
        this.fill = fill;
        this.type = type;
        this.pressure = pressure;
    }

    public FluidStack copy() {
        return new FluidStack(this.type, this.fill, this.pressure);
    }
}
