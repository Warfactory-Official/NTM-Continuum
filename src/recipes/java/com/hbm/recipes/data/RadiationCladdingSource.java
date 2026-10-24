package com.hbm.recipes.data;

import com.hbm.api.data.ArmorDataMaps;
import dev.golem.core.data.DataMapSource;

/** 1.7 {@code ModItems}: the {@code new ItemModCladding(rad)} lines. */
public final class RadiationCladdingSource extends DataMapSource<Double> {

    public RadiationCladdingSource() {
        super(ArmorDataMaps.RADIATION_CLADDING, false);
    }

    @Override
    protected void values() {
        put("hbm:cladding_paint", 0.025D);
        put("hbm:cladding_rubber", 0.005D);
        put("hbm:cladding_lead", 0.1D);
        put("hbm:cladding_desh", 0.2D);
        put("hbm:cladding_ghiorsium", 0.5D);
    }
}
