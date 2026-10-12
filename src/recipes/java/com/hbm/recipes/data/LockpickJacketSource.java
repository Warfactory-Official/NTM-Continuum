package com.hbm.recipes.data;

import com.hbm.api.data.ArmorDataMaps;
import dev.golem.core.data.DataMapSource;

/** 1.7 {@code TileEntityLockableBase}: {@code checkArmorPiece(player, jackt, 2) || checkArmorPiece(player, jackt2, 2)}. */
public final class LockpickJacketSource extends DataMapSource<Boolean> {

    public LockpickJacketSource() {
        super(ArmorDataMaps.LOCKPICK_JACKET, false);
    }

    @Override
    protected void values() {
        put("hbm:jackt", true);
        put("hbm:jackt2", true);
    }
}
