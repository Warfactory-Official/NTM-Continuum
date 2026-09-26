package com.hbm.items;

import com.hbm.Constants;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.machine.ItemSelfcharger;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;

/** The seven deprecated fixed-output batteries used by powered armour's auxiliary drain. */
@SuppressWarnings("deprecation")
public final class ModSelfchargerItems {
    private ModSelfchargerItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<ItemSelfcharger> battery_sc_uranium = register("battery_sc_uranium", 5);
    public static final RegistryObject<ItemSelfcharger> battery_sc_technetium = register("battery_sc_technetium", 25);
    public static final RegistryObject<ItemSelfcharger> battery_sc_plutonium = register("battery_sc_plutonium", 100);
    public static final RegistryObject<ItemSelfcharger> battery_sc_polonium = register("battery_sc_polonium", 500);
    public static final RegistryObject<ItemSelfcharger> battery_sc_gold = register("battery_sc_gold", 2_500);
    public static final RegistryObject<ItemSelfcharger> battery_sc_lead = register("battery_sc_lead", 5_000);
    public static final RegistryObject<ItemSelfcharger> battery_sc_americium = register("battery_sc_americium", 10_000);

    private static RegistryObject<ItemSelfcharger> register(String name, long charge) {
        RegistryObject<ItemSelfcharger> item = ITEMS.registerItem(name,
                p -> new ItemSelfcharger(p.stacksTo(1), charge));
        ModTabs.addTo(HbmTab.CONTROL, item);
        return item;
    }

    public static void init() {}
}
