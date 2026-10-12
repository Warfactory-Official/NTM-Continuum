package com.hbm.recipes.data;

import com.hbm.api.data.MachineDataMaps;
import com.hbm.api.data.MachineDataMaps.RadGenFuel;
import dev.golem.core.data.DataMapSource;
import java.util.Optional;

/**
 * 1.7 {@code TileEntityMachineRadGen} fuels (production, duration, leftover). The waste drums, the metadata loop above
 * these lines in 1.7, stay a rule in the machine.
 */
public final class RadGenFuelSource extends DataMapSource<RadGenFuel> {

    public RadGenFuelSource() {
        super(MachineDataMaps.RADGEN_FUEL, false);
    }

    @Override
    protected void values() {
        // 1.7 TileEntityMachineRadGen.fuels, a HashMap (order means nothing), in its put order
        put("hbm:scrap_nuclear", new RadGenFuel(50, 5 * 60 * 20, Optional.empty(), 1));
        put("hbm:gem_rad", new RadGenFuel(25_000, 30 * 60 * 20, Optional.of("minecraft:diamond"), 1));
    }
}
