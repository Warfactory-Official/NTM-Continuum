package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.items.machine.ItemBatterySC;
import com.hbm.items.machine.ItemBatterySC.EnumBatterySC;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;

import java.util.EnumMap;
import java.util.Map;
import com.hbm.registration.Reg;
import net.minecraft.world.item.Item;
import com.hbm.registration.Presets;

/**
 * 1.7 {@code ModItems.battery_sc} (an {@code ItemBatterySC} enum-multi, control tab, stack size 1), flattened to
 * one {@code battery_sc_<variant>} item per {@link EnumBatterySC} in enum order.
 */
public final class ModBatterySCItems {
    private ModBatterySCItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    public static final Map<EnumBatterySC, RegistryObject<ItemBatterySC>> BATTERIES = new EnumMap<>(EnumBatterySC.class);

    static {
        for (EnumBatterySC pack : EnumBatterySC.VALUES) {
            // Sprite battery_sc_<variant>; the recipes stay in GapsADeclarations.
            RegistryObject<ItemBatterySC> item = REG.<ItemBatterySC>item(pack.id(),
                            (Item.Properties p) -> new ItemBatterySC(p.stacksTo(1), pack))
                    .transform(Presets.sprite())
                    .tab(HbmTab.CONTROL);
            BATTERIES.put(pack, item);
        }
    }

    public static ItemBatterySC get(EnumBatterySC pack) {
        return BATTERIES.get(pack).get();
    }

    public static void init() {}
}
