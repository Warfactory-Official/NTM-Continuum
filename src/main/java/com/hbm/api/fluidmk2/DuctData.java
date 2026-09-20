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
 *
 * <p>One datum is not one fluid: {@link #SMOKE_ANY}, the exhaust duct's. 1.7's
 * {@code TileEntityPipeExhaust} minted a node in each of the three smoke nodespaces, so one exhaust
 * run carried {@code SMOKE}, {@code SMOKE_LEADED} and {@code SMOKE_POISON} at once. The graph keys a
 * cell by position, so the exhaust is instead one cell whose datum is compatible with all three smokes
 * and whose net {@link FluidNetwork} rounds once per smoke (see {@link #types()}).
 */
public record DuctData(int typeId) {

    /** A reserved id no {@link FluidType} is ever registered under ({@code Fluids.fromID} answers NONE for it). */
    public static final int SMOKE_ANY_ID = -2;

    public static final DuctData NONE = new DuctData(Fluids.NONE.getID());

    /** The exhaust duct's datum: every smoke at once. */
    public static final DuctData SMOKE_ANY = new DuctData(SMOKE_ANY_ID);

    public static final Codec<DuctData> CODEC = Codec.INT.xmap(DuctData::new, DuctData::typeId);

    public static DuctData of(FluidType type) {
        return type == Fluids.NONE ? NONE : new DuctData(type.getID());
    }

    /** Is this fluid one of the three 1.7 exhaust smokes? */
    public static boolean isSmoke(FluidType type) {
        return type == Fluids.SMOKE || type == Fluids.SMOKE_LEADED || type == Fluids.SMOKE_POISON;
    }

    public boolean isSmokeAny() {
        return typeId == SMOKE_ANY_ID;
    }

    /** The one fluid carried; for {@link #SMOKE_ANY} the representative {@code SMOKE} (the exhaust's default skin). */
    public FluidType type() {
        return isSmokeAny() ? Fluids.SMOKE : Fluids.fromID(typeId);
    }

    /** Every fluid this datum carries: one for a typed duct, the three smokes for {@link #SMOKE_ANY}. */
    public FluidType[] types() {
        return isSmokeAny() ? new FluidType[]{Fluids.SMOKE, Fluids.SMOKE_LEADED, Fluids.SMOKE_POISON}
                : new FluidType[]{type()};
    }

    /** Does this datum carry {@code type}? A typed duct carries its one fluid; the exhaust carries any smoke. */
    public boolean carries(FluidType type) {
        return isSmokeAny() ? isSmoke(type) : typeId == type.getID();
    }
}
