package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.items.machine.ItemBatteryPack;
import com.hbm.items.machine.ItemBatteryPack.EnumBatteryPack;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;

import java.util.EnumMap;
import java.util.Map;
import com.hbm.registration.Reg;

/**
 * The HBM battery and capacitor items: the storage medium the {@code machine_battery_socket} holds.
 * One registered item per {@link EnumBatteryPack} variant (the old single metadata item is split into 12
 * in 26.1). Kept out of the generated {@link ModItems} so codegen never clobbers them.
 */
public final class ModBatteryItems {

    private ModBatteryItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    /** Lookup from variant to its registered item, used by the socket renderer and item-icon JSON wiring. */
    public static final Map<EnumBatteryPack, RegistryObject<ItemBatteryPack>> PACKS = new EnumMap<>(EnumBatteryPack.class);

    static {
        for (EnumBatteryPack pack : EnumBatteryPack.VALUES) {
            // The OBJ item definitions are typed data (ObjItemDeclarations), not stated here.
            RegistryObject<ItemBatteryPack> item = REG.<ItemBatteryPack>item(
                    pack.name, p -> new ItemBatteryPack(p, pack), p -> p.stacksTo(1)).tab(HbmTab.CONTROL);
            PACKS.put(pack, item);
        }
    }

    public static void init() {}
}
