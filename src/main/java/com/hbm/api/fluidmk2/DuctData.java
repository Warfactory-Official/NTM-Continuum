package com.hbm.api.fluidmk2;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.mojang.serialization.Codec;

/**
 * The conductor datum for the fluid graph: the one fluid a duct carries.
 *
 * <p>Unlike the power graph's, this one partitions. {@link FluidGraphProvider#dataCompatible} refuses
 * to join two ducts carrying different fluids, which is what lets a level's whole plumbing live in one
 * save without the steam line and the oil line becoming one network — the job UNINOS did by giving
 * every {@link FluidType} a nodespace of its own.
 */
public record DuctData(int typeId) {

    public static final DuctData NONE = new DuctData(Fluids.NONE.getID());

    public static final Codec<DuctData> CODEC = Codec.INT.xmap(DuctData::new, DuctData::typeId);

    public static DuctData of(FluidType type) {
        return type == Fluids.NONE ? NONE : new DuctData(type.getID());
    }

    public FluidType type() {
        return Fluids.fromID(typeId);
    }
}
