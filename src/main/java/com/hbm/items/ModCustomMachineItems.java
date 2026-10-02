package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.tool.ItemCMStructure;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;

/** 1.7 {@code structure_custommachine}: stack size 1, consumable tab, {@code setFull3D}. */
public final class ModCustomMachineItems {

    private ModCustomMachineItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    public static final RegistryObject<ItemCMStructure> structure_custommachine = REG.<ItemCMStructure>item(
                    "structure_custommachine", ItemCMStructure::new, p -> p.stacksTo(1))
            .transform(Presets.handheld()); // 1.7 setFull3D

    static {
        ModTabs.addTo(HbmTab.CONSUMABLES, structure_custommachine);
    }

    public static void init() {}
}
