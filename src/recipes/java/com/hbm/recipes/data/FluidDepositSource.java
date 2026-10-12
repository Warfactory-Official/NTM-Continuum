package com.hbm.recipes.data;

import com.hbm.api.data.DepositDataMaps;
import com.hbm.api.data.DepositDataMaps.Deposit;
import dev.golem.core.data.DataMapSource;
import java.util.Optional;

/**
 * 1.7 {@code TileEntityOilDrillBase} and its drills: {@code ore_oil} gives oil and gas and drains into
 * {@code ore_oil_empty}; {@code ore_bedrock_oil} is fracking-only, a tenth of the yield, and never drains.
 */
public final class FluidDepositSource extends DataMapSource<Deposit> {

    public FluidDepositSource() {
        super(DepositDataMaps.FLUID_DEPOSIT, false);
    }

    @Override
    protected void values() {
        put("hbm:ore_oil", new Deposit("hbm:oil", "hbm:gas", Optional.of("hbm:ore_oil_empty"), 1D, Optional.empty(), 1D, false));
        put("hbm:ore_bedrock_oil", new Deposit("hbm:oil", "hbm:gas", Optional.empty(), 0.1D, Optional.empty(), 1D, true));
    }
}
