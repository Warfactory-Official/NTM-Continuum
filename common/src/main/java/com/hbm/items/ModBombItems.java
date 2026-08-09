package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.special.ItemCustomLore;
import com.hbm.registry.DeferredRegister;
import com.hbm.registry.RegistryObject;

/**
 * Hand-registered bomb crafting components (igniters, cores, explosive lenses) used to arm HBM bombs.
 * Kept separate from the generated {@link ModItems} so codegen never clobbers them. The "used in: ..."
 * tooltips come from {@code item.hbm.<id>.desc.N} lang lines, read by {@link ItemCustomLore}.
 */
public final class ModBombItems {

    private ModBombItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** Fat Man / Gadget explosive lens. Keeps the original id {@code gadget_explosive8}. */
    public static final RegistryObject<ItemCustomLore> early_explosive_lenses =
            ITEMS.registerItem("gadget_explosive8", ItemCustomLore::new, p -> p.stacksTo(1));

    /** Fat Man / Mike / Tsar fissile core. */
    public static final RegistryObject<ItemCustomLore> man_core =
            ITEMS.registerItem("man_core", ItemCustomLore::new, p -> p.stacksTo(1));

    /** Fat Man igniter assembly. */
    public static final RegistryObject<ItemCustomLore> man_igniter =
            ITEMS.registerItem("man_igniter", ItemCustomLore::new, p -> p.stacksTo(1));

    static {
        ModTabs.addTo(HbmTab.BOMBS, early_explosive_lenses);
        ModTabs.addTo(HbmTab.BOMBS, man_core);
        ModTabs.addTo(HbmTab.BOMBS, man_igniter);
    }

    public static void init() {}
}
