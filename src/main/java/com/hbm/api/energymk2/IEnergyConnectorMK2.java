package com.hbm.api.energymk2;

import net.minecraft.core.Direction;

public interface IEnergyConnectorMK2 {

    /**
     * Whether the given side can be connected to. {@code dir} refers to the side of this block, not
     * the connecting block doing the check; null = a network pseudo-face.
     */
    default boolean canConnect(@org.jspecify.annotations.Nullable Direction dir) {
        return dir != null;
    }
}
