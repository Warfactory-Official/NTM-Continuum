package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.tool.ItemCMStructure;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;

/** 1.7 {@code structure_custommachine}: stack size 1, consumable tab, {@code setFull3D}. */
public final class ModCustomMachineItems {

    private ModCustomMachineItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<ItemCMStructure> structure_custommachine = ITEMS.registerItem(
            "structure_custommachine", ItemCMStructure::new, p -> p.stacksTo(1));

    static {
        ModTabs.addTo(HbmTab.CONSUMABLES, structure_custommachine);
    }

    public static void init() {}
}
