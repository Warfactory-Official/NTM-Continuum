package com.hbm.api.energymk2;

import com.hbm.lib.ForgeDirection;

public interface IEnergyConnectorMK2 {

    /**
     * Whether the given side can be connected to. {@code dir} refers to the side of this block, not
     * the connecting block doing the check.
     */
    default boolean canConnect(ForgeDirection dir) {
        return dir != ForgeDirection.UNKNOWN;
    }
}
