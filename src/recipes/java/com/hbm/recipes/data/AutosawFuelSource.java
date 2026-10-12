package com.hbm.recipes.data;

import com.hbm.api.data.FluidDataMaps;
import dev.golem.core.data.DataMapSource;

/** 1.7 {@code TileEntityMachineAutosaw.acceptedFuels}. */
public final class AutosawFuelSource extends DataMapSource<Boolean> {

    public AutosawFuelSource() {
        super(FluidDataMaps.AUTOSAW_FUEL, false);
    }

    @Override
    protected void values() {
        put("hbm:woodoil", true);
        put("hbm:ethanol", true);
        put("hbm:fishoil", true);
        put("hbm:heavyoil", true);
        // 1.7 acceptedFuels.add(Fluids.COALCREOSOTE)
        put("hbm:coalcreosote", true);
    }
}
