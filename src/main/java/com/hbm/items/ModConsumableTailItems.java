package com.hbm.items;

import com.hbm.Constants;
import com.hbm.capability.HbmLivingProps;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.special.ItemEnergy;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.Item;

/** Source-backed drink and stimulant family from old-hbm's ItemEnergy. */
public final class ModConsumableTailItems {
    private ModConsumableTailItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    private static RegistryObject<ItemEnergy> can(String id) {
        RegistryObject<ItemEnergy> item = ITEMS.registerItem(id, p -> new ItemEnergy(p, id, ItemEnergy.Container.CAN));
        ModTabs.addTo(HbmTab.CONSUMABLES, item);
        return item;
    }

    private static RegistryObject<ItemEnergy> bottle(String id) {
        RegistryObject<ItemEnergy> item = ITEMS.registerItem(id, p -> new ItemEnergy(p, id, ItemEnergy.Container.BOTTLE));
        ModTabs.addTo(HbmTab.CONSUMABLES, item);
        return item;
    }

    private static RegistryObject<ItemEnergy> drink(String id) {
        RegistryObject<ItemEnergy> item = ITEMS.registerItem(id, p -> new ItemEnergy(p, id, ItemEnergy.Container.NONE));
        ModTabs.addTo(HbmTab.CONSUMABLES, item);
        return item;
    }

    public static final RegistryObject<Item> bottle_opener = ITEMS.registerSimpleItem("bottle_opener", p -> p.stacksTo(1));
    static { ModTabs.addTo(HbmTab.CONSUMABLES, bottle_opener); }
    public static final RegistryObject<ItemEnergy> can_smart = can("can_smart");
    public static final RegistryObject<ItemEnergy> can_creature = can("can_creature");
    public static final RegistryObject<ItemEnergy> can_redbomb = can("can_redbomb");
    public static final RegistryObject<ItemEnergy> can_mrsugar = can("can_mrsugar");
    public static final RegistryObject<ItemEnergy> can_overcharge = can("can_overcharge");
    public static final RegistryObject<ItemEnergy> can_luna = can("can_luna");
    public static final RegistryObject<ItemEnergy> can_bepis = can("can_bepis");
    public static final RegistryObject<ItemEnergy> can_breen = can("can_breen");
    public static final RegistryObject<ItemEnergy> can_mug = can("can_mug");

    public static final RegistryObject<ItemEnergy> bottle_nuka = bottle("bottle_nuka");
    public static final RegistryObject<ItemEnergy> bottle_cherry = bottle("bottle_cherry");
    public static final RegistryObject<ItemEnergy> bottle_quantum = bottle("bottle_quantum");
    public static final RegistryObject<ItemEnergy> bottle_sparkle = bottle("bottle_sparkle");
    public static final RegistryObject<ItemEnergy> bottle_rad = bottle("bottle_rad");
    public static final RegistryObject<ItemEnergy> bottle2_korl = bottle("bottle2_korl");
    public static final RegistryObject<ItemEnergy> bottle2_fritz = bottle("bottle2_fritz");

    public static final RegistryObject<ItemEnergy> coffee = drink("coffee");
    public static final RegistryObject<ItemEnergy> coffee_radium = drink("coffee_radium");

    public static void init() {}
}




