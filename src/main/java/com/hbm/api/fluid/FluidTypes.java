package com.hbm.api.fluid;

import com.hbm.inventory.fluid.Fluids;
import java.util.List;
import net.minecraft.resources.Identifier;

/** Fluid lookups. Unknown ids answer {@link #none()}. */
public final class FluidTypes {

    private FluidTypes() {}

    public static FluidType none() {
        return Fluids.NONE;
    }

    public static FluidType byId(Identifier id) {
        return Fluids.byId(id);
    }

    /** {@code hbm:water}, or a built-in name ({@code WATER}). */
    public static FluidType byName(String idOrName) {
        return Fluids.fromName(idOrName);
    }

    public static List<FluidType> all() {
        return List.of(Fluids.getAll());
    }
}
