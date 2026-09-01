package com.hbm.items.solar;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.tool.ItemMirrorTool;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.Item;

/** Solar-only hand item register. */
public final class ModSolarItems {

    private ModSolarItems() {}

    static {
        SolarDataComponents.init();
    }

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<ItemMirrorTool> mirror_tool = ITEMS.registerItem(
            "mirror_tool", ItemMirrorTool::new, p -> p.stacksTo(1));

    static {
        ModTabs.addTo(HbmTab.CONSUMABLES, mirror_tool);
    }

    public static void init() {}
}
