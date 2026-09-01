package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.tool.ItemBlowtorch;
import com.hbm.items.tool.ItemBoltgun;
import com.hbm.items.tool.ItemDefuser;
import com.hbm.items.tool.ItemTooling;
import com.hbm.api.block.IToolable.ToolType;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;

/**
 * HBM's in-world construction tools — the items that drive {@link com.hbm.api.block.IToolable}
 * conversions. The ordinary screwdriver and hand drill remain on the consumables tab; the defuser
 * remains on the bombs tab, matching their separate 1.12.2 registrations.
 */
public final class ModToolItems {

    private ModToolItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<ItemBlowtorch> blowtorch =
            ITEMS.registerItem("blowtorch", ItemBlowtorch::new, p -> p.stacksTo(1));
    public static final RegistryObject<ItemBoltgun> boltgun =
            ITEMS.registerItem("boltgun", ItemBoltgun::new, p -> p.stacksTo(1));
    public static final RegistryObject<ItemTooling> screwdriver =
            ITEMS.registerItem("screwdriver", p -> new ItemTooling(p, ToolType.SCREWDRIVER),
                    p -> p.stacksTo(1).durability(100));
    public static final RegistryObject<ItemTooling> hand_drill =
            ITEMS.registerItem("hand_drill", p -> new ItemTooling(p, ToolType.HAND_DRILL),
                    p -> p.stacksTo(1).durability(100));
    public static final RegistryObject<ItemDefuser> defuser =
            ITEMS.registerItem("defuser", ItemDefuser::new,
                    p -> p.stacksTo(1).durability(100));

    static {
        ModTabs.addTo(HbmTab.CONTROL, blowtorch);
        ModTabs.addTo(HbmTab.CONTROL, boltgun);
        ModTabs.addTo(HbmTab.CONSUMABLES, screwdriver);
        ModTabs.addTo(HbmTab.CONSUMABLES, hand_drill);
        ModTabs.addTo(HbmTab.BOMBS, defuser);

        ToolType.SCREWDRIVER.register(screwdriver);
        ToolType.HAND_DRILL.register(hand_drill);
        ToolType.DEFUSER.register(defuser);
    }

    public static void init() {}
}
