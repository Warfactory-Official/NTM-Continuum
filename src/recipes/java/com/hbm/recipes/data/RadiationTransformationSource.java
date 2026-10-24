package com.hbm.recipes.data;

import com.hbm.api.data.RadiationDataMaps;
import com.hbm.api.data.RadiationDataMaps.Transformation;
import dev.golem.core.data.DataMapSource;

/** 1.7 {@code handleRadiationEffect}; 1.7 matched the cow and the villager by subclass, the port by type. */
public final class RadiationTransformationSource extends DataMapSource<Transformation> {

    public RadiationTransformationSource() {
        super(RadiationDataMaps.TRANSFORMATIONS, false);
    }

    @Override
    protected void values() {
        put("minecraft:creeper", new Transformation(200D, 3, "hbm:entity_mob_nuclear_creeper", 100F));
        put("minecraft:cow", new Transformation(50D, 1, "minecraft:mooshroom", 0F));
        // 1.7 spawns a plain zombie, not a zombie villager
        put("minecraft:villager", new Transformation(500D, 1, "minecraft:zombie", 0F));
        put("hbm:entity_fucc_a_ducc", new Transformation(200D, 1, "hbm:entity_elder_one", 0F));
    }
}
