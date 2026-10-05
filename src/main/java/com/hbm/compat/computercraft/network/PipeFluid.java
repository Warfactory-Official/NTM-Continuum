package com.hbm.compat.computercraft.network;

//? if computercraft {
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import org.jspecify.annotations.Nullable;

/** 1.7 {@code TileEntityPipeBaseNT.getType().getName()}: the port's ducts and valves keep the type on themselves. */
final class PipeFluid {

    private PipeFluid() {}

    static String name(@Nullable FluidType type) {
        return (type == null ? Fluids.NONE : type).getName();
    }
}
//?}
