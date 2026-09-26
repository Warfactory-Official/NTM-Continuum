package com.hbm.api.fluidmk2;

import com.hbm.api.fluid.FluidType;
import net.minecraft.core.Direction;

/** A tile that can be connected to by a fluid network of the given type/side. */
public interface IFluidConnectorMK2 {
    /** Whether the given side can be connected to; null = a network pseudo-face. */
    default boolean canConnect(FluidType type, @org.jspecify.annotations.Nullable Direction dir) {
        return dir != null;
    }
}
