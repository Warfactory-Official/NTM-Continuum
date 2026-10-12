package com.hbm.recipes.data;

import com.hbm.api.data.FluidDataMaps;
import dev.golem.core.data.DataMapSource;

/** 1.7 {@code TileEntityMachineTurbineGas.fuelMaxCons}: mB per tick at full throttle; a fuel not listed burns 5. */
public final class GasTurbineFuelSource extends DataMapSource<Double> {

    public GasTurbineFuelSource() {
        super(FluidDataMaps.GAS_TURBINE_FUEL, false);
    }

    @Override
    protected void values() {
        put("hbm:gas", 50D);           // natgas doesn't burn well so it burns faster to compensate
        put("hbm:syngas", 10D);        // syngas just fucks
        put("hbm:oxyhydrogen", 100D);  // oxyhydrogen is terrible so it needs to burn a ton for the bare minimum
        put("hbm:reformgas", 5D);      // fuck it we ball
    }
}
