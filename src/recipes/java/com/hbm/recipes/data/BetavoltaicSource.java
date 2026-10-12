package com.hbm.recipes.data;

import com.hbm.api.data.ReactorDataMaps;
import dev.golem.core.data.DataMapSource;

/** 1.7 {@code ItemBatterySC.EnumBatterySC}: HE/t per cell. */
public final class BetavoltaicSource extends DataMapSource<Long> {

    public BetavoltaicSource() {
        super(ReactorDataMaps.BETAVOLTAIC, false);
    }

    @Override
    protected void values() {
        put("hbm:battery_sc_empty", 0L);
        put("hbm:battery_sc_waste", 150L);
        put("hbm:battery_sc_ra226", 200L);
        put("hbm:battery_sc_tc99", 500L);
        put("hbm:battery_sc_co60", 750L);
        put("hbm:battery_sc_pu238", 1_000L);
        put("hbm:battery_sc_po210", 1_250L);
        put("hbm:battery_sc_au198", 1_500L);
        put("hbm:battery_sc_pb209", 2_000L);
        put("hbm:battery_sc_am241", 2_500L);
    }
}
