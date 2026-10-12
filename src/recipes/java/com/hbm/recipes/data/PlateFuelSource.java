package com.hbm.recipes.data;

import com.hbm.api.data.ReactorDataMaps;
import com.hbm.api.data.ReactorDataMaps.PlateFuel;
import com.hbm.items.machine.ItemPlateFuel.FunctionEnum;
import dev.golem.core.data.DataMapSource;

/** 1.7 {@code ModItems}: {@code new ItemPlateFuel(lifeTime).setFunction(function, reactivity)}. */
public final class PlateFuelSource extends DataMapSource<PlateFuel> {

    public PlateFuelSource() {
        super(ReactorDataMaps.PLATE_FUEL, false);
    }

    @Override
    protected void values() {
        put("hbm:plate_fuel_u233", new PlateFuel(2200000, FunctionEnum.SQUARE_ROOT, 50));
        put("hbm:plate_fuel_u235", new PlateFuel(2200000, FunctionEnum.SQUARE_ROOT, 40));
        put("hbm:plate_fuel_mox", new PlateFuel(2400000, FunctionEnum.LOGARITHM, 50));
        put("hbm:plate_fuel_pu239", new PlateFuel(2000000, FunctionEnum.NEGATIVE_QUADRATIC, 50));
        put("hbm:plate_fuel_sa326", new PlateFuel(2000000, FunctionEnum.LINEAR, 80));
        put("hbm:plate_fuel_ra226be", new PlateFuel(1300000, FunctionEnum.PASSIVE, 30));
        put("hbm:plate_fuel_pu238be", new PlateFuel(1000000, FunctionEnum.PASSIVE, 50));
    }
}
