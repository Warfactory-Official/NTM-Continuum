package com.hbm.recipes.data;

import static com.hbm.util.DamageResistanceHandler.CATEGORY_EXPLOSION;

import com.hbm.api.data.ArmorDataMaps;
import com.hbm.util.DamageResistanceHandler.ResistanceStats;
import dev.golem.core.data.DataMapSource;

/** 1.7 {@code DamageResistanceHandler.initDefaults}: the {@code entityStats} lines (keyed by entity class there). */
public final class EntityDamageResistanceSource extends DataMapSource<ResistanceStats> {

    public EntityDamageResistanceSource() {
        super(ArmorDataMaps.ENTITY_DAMAGE_RESISTANCE, false);
    }

    @Override
    protected void values() {
        put("minecraft:creeper", new ResistanceStats().addCategory(CATEGORY_EXPLOSION, 2F, 0.25F));
        // 1.7 EntityCreeperNuclear: the port's hbm:entity_mob_nuclear_creeper (1.7 matched the exact class, the port the type)
        put("hbm:entity_mob_nuclear_creeper", new ResistanceStats().addCategory(CATEGORY_EXPLOSION, 5F, 0.35F));
    }
}
