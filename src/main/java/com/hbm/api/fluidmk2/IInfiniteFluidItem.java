package com.hbm.api.fluidmk2;

import com.hbm.inventory.fluid.FluidType;
import org.jspecify.annotations.Nullable;

/**
 * The 1.7 {@code ItemInfiniteFluid} contract, as read by the 1.7 {@code FluidLoaderInfinite}: an item
 * that, sitting in a tank's load slot, adds {@link #infiniteAmount()} mB of {@link #infiniteType()}
 * (any fluid when null) every tick it wins its {@link #infiniteChance()} roll, and in the unload
 * slot, drains the same amount into nowhere. It is never consumed or moved.
 */
public interface IInfiniteFluidItem {

    @Nullable FluidType infiniteType();

    int infiniteAmount();

    /** 1 = every tick; n = one tick in n. */
    int infiniteChance();

    /** Whether the item may act as a sink for a tank at {@code pressure} (only the infinite barrel may above 0). */
    boolean allowPressure(int pressure);
}
