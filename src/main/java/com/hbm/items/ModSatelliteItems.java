package com.hbm.items;

import com.hbm.Constants;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.machine.ItemSatellite;
import com.hbm.items.machine.ItemSatellite.EnumSatType;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;

import java.util.Arrays;

/** The launchable satellites, one item with a variant per kind. */
public final class ModSatelliteItems {

    private ModSatelliteItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<ItemSatellite> satellite =
            ITEMS.registerItem("satellite", ItemSatellite::new, p -> p.stacksTo(1));

    static {
        ModTabs.addStacksTo(HbmTab.MISSILES, () -> Arrays.stream(EnumSatType.VALUES)
                .map(type -> satellite.get().of(type)).toList());
    }

    /** Class-load hook so the register is populated during bootstrap. */
    public static void init() {}
}
