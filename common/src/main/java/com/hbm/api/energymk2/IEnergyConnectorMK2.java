package com.hbm.api.energymk2;

import com.hbm.lib.ForgeDirection;

public interface IEnergyConnectorMK2 {

    default boolean canConnect(ForgeDirection dir) {
        return dir != ForgeDirection.UNKNOWN;
    }
}
