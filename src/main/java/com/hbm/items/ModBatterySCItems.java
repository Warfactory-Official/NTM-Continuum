package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.machine.ItemBatterySC;
import com.hbm.items.machine.ItemBatterySC.EnumBatterySC;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

/**
 * 1.7 {@code ModItems.battery_sc} (an {@code ItemBatterySC} enum-multi, control tab, stack size 1), flattened to
 * one {@code battery_sc_<variant>} item per {@link EnumBatterySC} in enum order.
 */
public final class ModBatterySCItems {
    private ModBatterySCItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final Map<EnumBatterySC, RegistryObject<ItemBatterySC>> BATTERIES = new EnumMap<>(EnumBatterySC.class);

    static {
        for (EnumBatterySC pack : EnumBatterySC.VALUES) {
            RegistryObject<ItemBatterySC> item = ITEMS.registerItem(pack.id(),
                    p -> new ItemBatterySC(p.stacksTo(1), pack));
            ModTabs.addTo(HbmTab.CONTROL, item);
            BATTERIES.put(pack, item);
        }
    }

    public static ItemBatterySC get(EnumBatterySC pack) {
        return BATTERIES.get(pack).get();
    }

    public static void init() {}
}
