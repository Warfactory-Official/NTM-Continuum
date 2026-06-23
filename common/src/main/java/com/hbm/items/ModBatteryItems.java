package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.machine.ItemBatteryPack;
import com.hbm.items.machine.ItemBatteryPack.EnumBatteryPack;
import com.hbm.registry.DeferredRegister;
import com.hbm.registry.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

public final class ModBatteryItems {

    private ModBatteryItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final Map<EnumBatteryPack, RegistryObject<ItemBatteryPack>> PACKS = new EnumMap<>(EnumBatteryPack.class);

    static {
        for (EnumBatteryPack pack : EnumBatteryPack.VALUES) {
            RegistryObject<ItemBatteryPack> item = ITEMS.registerItem(
                    pack.name, p -> new ItemBatteryPack(p, pack), p -> p.stacksTo(1));
            PACKS.put(pack, item);
            ModTabs.addTo(HbmTab.CONTROL, item);
        }
    }

    public static void init() {}
}
