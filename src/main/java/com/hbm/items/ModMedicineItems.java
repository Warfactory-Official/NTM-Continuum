package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.food.ItemPill;
import com.hbm.items.special.ItemConsumable;
import com.hbm.items.special.ItemSimpleConsumable;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.CompatConsumables;
import net.minecraft.world.item.Item;

import java.util.List;

/** Hand-registered medicine and injection items formerly constructed in {@code ModItems}. */
public final class ModMedicineItems {

    private ModMedicineItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    private static RegistryObject<ItemPill> pill(String id, HbmTab tab) {
        RegistryObject<ItemPill> item = ITEMS.registerItem(id, p -> new ItemPill(p, id),
                p -> CompatConsumables.foodAlwaysEdible(p, 0, 0F, List.of()));
        if (tab != null) ModTabs.addTo(tab, item);
        return item;
    }

    private static RegistryObject<ItemConsumable> consumable(String id, HbmTab tab, boolean stackOne) {
        RegistryObject<ItemConsumable> item = ITEMS.registerItem(id, p -> new ItemConsumable(p, id),
                p -> stackOne ? p.stacksTo(1) : p);
        if (tab != null) ModTabs.addTo(tab, item);
        return item;
    }

    private static RegistryObject<ItemSimpleConsumable> simple(String id, HbmTab tab) {
        RegistryObject<ItemSimpleConsumable> item = ITEMS.registerItem(id,
                p -> new ItemSimpleConsumable(p, id));
        if (tab != null) ModTabs.addTo(tab, item);
        return item;
    }

    public static final RegistryObject<Item> iv_empty = ITEMS.registerSimpleItem("iv_empty");
    static {
        ModTabs.addTo(HbmTab.CONSUMABLES, iv_empty);
    }

    public static final RegistryObject<ItemConsumable> syringe_awesome = consumable("syringe_awesome", HbmTab.CONSUMABLES, false);
    public static final RegistryObject<ItemConsumable> syringe_antidote = consumable("syringe_antidote", HbmTab.CONSUMABLES, false);
    public static final RegistryObject<ItemConsumable> syringe_poison = consumable("syringe_poison", HbmTab.CONSUMABLES, false);
    public static final RegistryObject<ItemConsumable> syringe_metal_medx = consumable("syringe_metal_medx", HbmTab.CONSUMABLES, false);
    public static final RegistryObject<ItemConsumable> syringe_metal_psycho = consumable("syringe_metal_psycho", HbmTab.CONSUMABLES, false);
    public static final RegistryObject<ItemConsumable> syringe_metal_stimpak = consumable("syringe_metal_stimpak", HbmTab.CONSUMABLES, false);
    public static final RegistryObject<ItemConsumable> syringe_metal_super = consumable("syringe_metal_super", HbmTab.CONSUMABLES, false);
    public static final RegistryObject<ItemConsumable> syringe_taint = consumable("syringe_taint", HbmTab.CONSUMABLES, false);
    public static final RegistryObject<ItemConsumable> syringe_mkunicorn = consumable("syringe_mkunicorn", null, false);
    public static final RegistryObject<ItemConsumable> med_bag = consumable("med_bag", HbmTab.CONSUMABLES, false);

    public static final RegistryObject<ItemPill> radx = pill("radx", HbmTab.CONSUMABLES);
    public static final RegistryObject<ItemPill> siox = pill("siox", HbmTab.CONSUMABLES);
    public static final RegistryObject<ItemPill> pill_herbal = pill("pill_herbal", HbmTab.CONSUMABLES);
    public static final RegistryObject<ItemPill> xanax = pill("xanax", HbmTab.CONSUMABLES);
    public static final RegistryObject<ItemPill> fmn = pill("fmn", HbmTab.CONSUMABLES);
    public static final RegistryObject<ItemPill> five_htp = pill("five_htp", HbmTab.CONSUMABLES);
    public static final RegistryObject<ItemPill> pill_iodine = pill("pill_iodine", HbmTab.CONSUMABLES);
    public static final RegistryObject<ItemPill> plan_c = pill("plan_c", HbmTab.CONSUMABLES);
    public static final RegistryObject<ItemPill> pill_red = pill("pill_red", HbmTab.CONSUMABLES);
    public static final RegistryObject<ItemPill> chocolate = pill("chocolate", HbmTab.CONSUMABLES);

    public static final RegistryObject<ItemSimpleConsumable> radaway = simple("radaway", HbmTab.CONSUMABLES);
    public static final RegistryObject<ItemSimpleConsumable> radaway_strong = simple("radaway_strong", HbmTab.CONSUMABLES);
    public static final RegistryObject<ItemSimpleConsumable> radaway_flush = simple("radaway_flush", HbmTab.CONSUMABLES);

    public static final RegistryObject<ItemConsumable> jetpack_tank = consumable("jetpack_tank", HbmTab.CONSUMABLES, true);
    public static final RegistryObject<ItemConsumable> cbt_device = consumable("cbt_device", HbmTab.CONSUMABLES, true);
    public static final RegistryObject<ItemConsumable> euphemium_stopper = consumable("euphemium_stopper", null, true);

    public static void init() {}
}
