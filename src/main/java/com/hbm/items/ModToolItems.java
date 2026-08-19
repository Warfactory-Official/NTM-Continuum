package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.tool.ItemBlowtorch;
import com.hbm.items.tool.ItemBoltgun;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;

/**
 * HBM's in-world construction tools — the items that drive {@link com.hbm.api.block.IToolable}
 * conversions. Upstream put these on the {@code controlTab}, which is {@link HbmTab#CONTROL} here.
 */
public final class ModToolItems {

    private ModToolItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<ItemBlowtorch> blowtorch =
            ITEMS.registerItem("blowtorch", ItemBlowtorch::new, p -> p.stacksTo(1));
    public static final RegistryObject<ItemBoltgun> boltgun =
            ITEMS.registerItem("boltgun", ItemBoltgun::new, p -> p.stacksTo(1));

    static {
        ModTabs.addTo(HbmTab.CONTROL, blowtorch);
        ModTabs.addTo(HbmTab.CONTROL, boltgun);
    }

    public static void init() {}
}
