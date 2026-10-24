package com.hbm.recipes.data;

import com.hbm.api.data.ArmorDataMaps;
import com.hbm.util.ArmorRegistry.HazardClass;
import dev.golem.core.data.DataMapSource;
import java.util.List;

/** 1.7 {@code ArmorGasMask.getBlacklist} and {@code ItemModGasmask.getBlacklist}; the hazmat hoods have none. */
public final class GasMaskBlacklistSource extends DataMapSource<List<HazardClass>> {

    private static final List<HazardClass> MONO = List.of(HazardClass.GAS_LUNG, HazardClass.GAS_BLISTERING, HazardClass.BACTERIA);
    private static final List<HazardClass> OTHERS = List.of(HazardClass.GAS_BLISTERING);

    public GasMaskBlacklistSource() {
        super(ArmorDataMaps.GAS_MASK_BLACKLIST, false);
    }

    @Override
    protected void values() {
        put("hbm:gas_mask", OTHERS);
        put("hbm:gas_mask_m65", OTHERS);
        put("hbm:gas_mask_mono", MONO);
        put("hbm:gas_mask_olde", OTHERS);
        put("hbm:attachment_mask", OTHERS);
        put("hbm:attachment_mask_mono", MONO);
    }
}
