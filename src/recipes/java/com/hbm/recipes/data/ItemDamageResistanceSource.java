package com.hbm.recipes.data;

import static com.hbm.util.DamageResistanceHandler.CATEGORY_PHYSICAL;

import com.hbm.api.data.ArmorDataMaps;
import com.hbm.util.DamageResistanceHandler.ResistanceStats;
import dev.golem.core.data.DataMapSource;

/** 1.7 {@code DamageResistanceHandler.initDefaults}: the {@code itemStats} lines (single pieces, set or no set). */
public final class ItemDamageResistanceSource extends DataMapSource<ResistanceStats> {

    public ItemDamageResistanceSource() {
        super(ArmorDataMaps.DAMAGE_RESISTANCE, false);
    }

    @Override
    protected void values() {
        put("hbm:jackt", new ResistanceStats().addCategory(CATEGORY_PHYSICAL, 1F, 0.20F));
        put("hbm:jackt2", new ResistanceStats().addCategory(CATEGORY_PHYSICAL, 2F, 0.25F));
    }
}
