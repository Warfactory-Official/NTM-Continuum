package com.hbm.api.fluidmk2;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;

/**
 * The conductor datum for the fluid graph: the one fluid a duct carries.
 *
 * <p>Unlike the power graph's, this one partitions. {@link FluidGraphProvider#dataCompatible} refuses
 * to join two ducts carrying different fluids, so one saved graph holds a level's whole plumbing without
 * the steam line and the oil line becoming one network (UNINOS gave every {@link FluidType} a nodespace
 * of its own).
 *
 * <p>One datum is not one fluid: {@link #SMOKE_ANY}, the exhaust duct's. 1.7's
 * {@code TileEntityPipeExhaust} minted a node in each of the three smoke nodespaces, so one exhaust
 * run carried {@code SMOKE}, {@code SMOKE_LEADED} and {@code SMOKE_POISON} at once. The graph keys a
 * cell by position, so the exhaust is one cell whose datum is compatible with all three smokes
 * and whose net {@link FluidNetwork} rounds once per smoke (see {@link #types()}).
 *
 * <p>The datum is saved by the fluid's registry name ({@code "hbm:oil"}, {@code "minecraft:water"}), or
 * {@link #SMOKE_ANY_NAME} for the exhaust, never by a numeric fluid id. An int in an older save reads as
 * NONE with one WARN.
 */
public record DuctData(FluidType fluid, boolean smokeAny) {

    /** What {@link #SMOKE_ANY} is saved as; no fluid has this name ({@code #} is not a valid id character). */
    public static final String SMOKE_ANY_NAME = "#smoke_any";

    public static final DuctData NONE = new DuctData(Fluids.NONE, false);

    /** The exhaust duct's datum: every smoke at once. */
    public static final DuctData SMOKE_ANY = new DuctData(Fluids.NONE, true);

    public static final Codec<DuctData> CODEC = Codec.either(Codec.STRING, Codec.INT).xmap(
            either -> either.map(
                    name -> SMOKE_ANY_NAME.equals(name) ? SMOKE_ANY : of(Fluids.byRegistryNameExact(name)),
                    number -> of(Fluids.fromOldNumericSave(number))),
            data -> Either.left(data.smokeAny ? SMOKE_ANY_NAME : data.fluid.getRegistryName()));

    public static DuctData of(FluidType type) {
        return type == null || type == Fluids.NONE ? NONE : new DuctData(type, false);
    }

    /** Is this fluid one of the three 1.7 exhaust smokes? */
    public static boolean isSmoke(FluidType type) {
        return type == Fluids.SMOKE || type == Fluids.SMOKE_LEADED || type == Fluids.SMOKE_POISON;
    }

    public boolean isSmokeAny() {
        return smokeAny;
    }

    /** A duct carrying nothing (not the exhaust, which carries every smoke). */
    public boolean isNone() {
        return !smokeAny && fluid == Fluids.NONE;
    }

    /** The one fluid carried; for {@link #SMOKE_ANY} the representative {@code SMOKE} (the exhaust's default skin). */
    public FluidType type() {
        return smokeAny ? Fluids.SMOKE : fluid;
    }

    /** Every fluid this datum carries: one for a typed duct, the three smokes for {@link #SMOKE_ANY}. */
    public FluidType[] types() {
        return smokeAny ? new FluidType[]{Fluids.SMOKE, Fluids.SMOKE_LEADED, Fluids.SMOKE_POISON}
                : new FluidType[]{fluid};
    }

    /** Does this datum carry {@code type}? A typed duct carries its one fluid; the exhaust carries any smoke. */
    public boolean carries(FluidType type) {
        return smokeAny ? isSmoke(type) : fluid == type;
    }
}
