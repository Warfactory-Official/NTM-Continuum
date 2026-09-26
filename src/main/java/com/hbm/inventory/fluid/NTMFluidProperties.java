package com.hbm.inventory.fluid;

import com.hbm.api.fluid.FluidType;
/** Trait lookup for HBM's native {@link FluidType} registry. */
public final class NTMFluidProperties {
    private NTMFluidProperties() {}
    public static void register(FluidType fluid, Object property) { fluid.addTraits(property); }
    public static boolean hasTrait(FluidType fluid, Class<?> trait) { return fluid != null && fluid.hasTrait(trait); }
    public static <T> T getTrait(FluidType fluid, Class<T> trait) { return fluid == null ? null : fluid.getTrait(trait); }
}
