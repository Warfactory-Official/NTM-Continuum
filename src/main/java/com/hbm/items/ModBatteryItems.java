package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.machine.ItemBatteryPack;
import com.hbm.items.machine.ItemBatteryPack.EnumBatteryPack;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

/**
 * The modern HBM battery / capacitor items — the storage medium the {@code machine_battery_socket} holds.
 * One registered item per {@link EnumBatteryPack} variant (the old single metadata item is split into 12
 * in 26.1). Kept out of the generated {@link ModItems} so codegen never clobbers them.
 */
public final class ModBatteryItems {

    private ModBatteryItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** Lookup from variant → its registered item, used by the socket renderer / item-icon JSON wiring. */
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
