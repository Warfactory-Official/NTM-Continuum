package com.hbm.recipes.data;

import com.hbm.api.data.WorldDataMaps;
import dev.golem.core.data.DataMapSource;

/** 1.7 {@code ExplosionNT.errosion} (the {@code ERRODE} attribute's table). */
public final class ErosionSource extends DataMapSource<String> {

    public ErosionSource() {
        super(WorldDataMaps.EROSION, false);
    }

    @Override
    protected void values() {
        put("hbm:concrete", "minecraft:gravel");
        put("hbm:concrete_smooth", "minecraft:gravel");
        put("hbm:brick_concrete", "hbm:brick_concrete_broken");
        put("hbm:brick_concrete_broken", "minecraft:gravel");
    }
}
