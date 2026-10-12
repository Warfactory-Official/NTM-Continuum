package com.hbm.inventory;

import com.hbm.api.data.FluidDataMaps;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;

/**
 * The 1.7 per-fluid container data ({@code CD_Canister(color)} and {@code CD_Gastank(bottleColor, labelColor)} from
 * {@code Fluids}), now the {@code hbm:fluid_containers} fluid data map ({@link FluidDataMaps#FLUID_CONTAINERS}), and the
 * {@code FT_LeadContainer}/{@code FT_NoContainer} trait sets. Read by the container items and the creative-tab variant
 * lists. The data holds 1.7's full list (42 canister and 19 gas-tank fluids); the port's earlier Java table had nine
 * and three.
 */
public final class FluidContainers {

    private FluidContainers() {}

    private static FluidDataMaps.Containers of(FluidType type) {
        if (type == null || type == Fluids.NONE) return null;
        return FluidDataMaps.FLUID_CONTAINERS.get(Fluids.toVanilla(type));
    }

    public static boolean hasCanister(FluidType type) { var c = of(type); return c != null && c.canister().isPresent(); }
    public static boolean hasGasTank(FluidType type) { var c = of(type); return c != null && c.gasBottle().isPresent(); }
    /** 1.7 {@code FT_LeadContainer}: only the lead tank may hold it (the trait is assigned in {@code Fluids}). */
    public static boolean needsLeadContainer(FluidType type) { return type.needsLeadContainer(); }
    /** 1.7 {@code FT_NoContainer}: no portable container at all. */
    public static boolean hasNoContainer(FluidType type) { return type.hasNoContainer(); }

    public static int canisterColor(FluidType type) { var c = of(type); return c == null ? 0xFFFFFF : c.canister().orElse(0xFFFFFF); }
    public static int gasBottleColor(FluidType type) { var c = of(type); return c == null ? 0xFFFFFF : c.gasBottle().orElse(0xFFFFFF); }
    public static int gasLabelColor(FluidType type) { var c = of(type); return c == null ? 0xFFFFFF : c.gasLabel().orElse(0xFFFFFF); }
}
