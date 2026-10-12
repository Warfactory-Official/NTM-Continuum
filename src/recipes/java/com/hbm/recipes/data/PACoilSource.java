package com.hbm.recipes.data;

import com.hbm.api.data.MachinePartDataMaps;
import com.hbm.api.data.MachinePartDataMaps.Coil;
import dev.golem.core.data.DataMapSource;

/** 1.7 {@code ItemPACoil.EnumCoilType} (quadrupole min/max, dipole min/max, dipole minimum distance). */
public final class PACoilSource extends DataMapSource<Coil> {

    public PACoilSource() {
        super(MachinePartDataMaps.PA_COIL, false);
    }

    @Override
    protected void values() {
        put("hbm:pa_coil_gold", new Coil(0, 2_200, 0, 2_200, 15));
        put("hbm:pa_coil_niobium", new Coil(1_500, 8_400, 1_500, 8_400, 21));
        put("hbm:pa_coil_bscco", new Coil(7_500, 15_000, 7_500, 15_000, 27));
        put("hbm:pa_coil_chlorophyte", new Coil(14_500, 75_000, 14_500, 75_000, 51));
    }
}
