package com.hbm.items;

import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
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
    private static final Reg REG = Reg.of(ITEMS);

    private static Reg.Handle<ItemPill> pill(String id, HbmTab tab) {
        Reg.Handle<ItemPill> item = REG.item(id, p -> new ItemPill(p, id),
                p -> CompatConsumables.foodAlwaysEdible(p, 0, 0F, List.of()));
        if (tab != null) ModTabs.addTo(tab, item);
        return item;
    }

    private static Reg.Handle<ItemConsumable> consumable(String id, HbmTab tab, boolean stackOne) {
        Reg.Handle<ItemConsumable> item = REG.item(id, p -> new ItemConsumable(p, id),
                p -> stackOne ? p.stacksTo(1) : p);
        if (tab != null) ModTabs.addTo(tab, item);
        return item;
    }

    private static Reg.Handle<ItemSimpleConsumable> simple(String id, HbmTab tab) {
        Reg.Handle<ItemSimpleConsumable> item = REG.item(id,
                p -> new ItemSimpleConsumable(p, id));
        if (tab != null) ModTabs.addTo(tab, item);
        return item;
    }

    /** 1.7 ItemSimpleConsumable: draws 5 health into a blood bag (the IV bags live in ModArmorSetBItems). */
    public static final RegistryObject<ItemSimpleConsumable> iv_empty = simple("iv_empty", HbmTab.CONSUMABLES)
            .transform(Presets.sprite());

    public static final RegistryObject<ItemConsumable> syringe_awesome = consumable("syringe_awesome", HbmTab.CONSUMABLES, false)
            .transform(Presets.handheld());
    public static final RegistryObject<ItemConsumable> syringe_antidote = consumable("syringe_antidote", HbmTab.CONSUMABLES, false)
            .transform(Presets.handheld());
    public static final RegistryObject<ItemConsumable> syringe_poison = consumable("syringe_poison", HbmTab.CONSUMABLES, false)
            .transform(Presets.handheld());
    public static final RegistryObject<ItemConsumable> syringe_metal_medx = consumable("syringe_metal_medx", HbmTab.CONSUMABLES, false)
            .transform(Presets.handheld());
    public static final RegistryObject<ItemConsumable> syringe_metal_psycho = consumable("syringe_metal_psycho", HbmTab.CONSUMABLES, false)
            .transform(Presets.handheld());
    public static final RegistryObject<ItemConsumable> syringe_metal_stimpak = consumable("syringe_metal_stimpak", HbmTab.CONSUMABLES, false)
            .transform(Presets.handheld());
    public static final RegistryObject<ItemConsumable> syringe_metal_super = consumable("syringe_metal_super", HbmTab.CONSUMABLES, false)
            .transform(Presets.handheld());
    public static final RegistryObject<ItemConsumable> syringe_taint = consumable("syringe_taint", HbmTab.CONSUMABLES, false)
            .transform(Presets.handheld());
    public static final RegistryObject<ItemConsumable> syringe_mkunicorn = consumable("syringe_mkunicorn", null, false)
            .transform(Presets.handheld());
    public static final RegistryObject<ItemConsumable> med_bag = consumable("med_bag", HbmTab.CONSUMABLES, false)
            .transform(Presets.sprite());

    public static final RegistryObject<ItemPill> radx = pill("radx", HbmTab.CONSUMABLES)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemPill> siox = pill("siox", HbmTab.CONSUMABLES)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemPill> pill_herbal = pill("pill_herbal", HbmTab.CONSUMABLES)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemPill> xanax = pill("xanax", HbmTab.CONSUMABLES)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemPill> fmn = pill("fmn", HbmTab.CONSUMABLES)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemPill> five_htp = pill("five_htp", HbmTab.CONSUMABLES)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemPill> pill_iodine = pill("pill_iodine", HbmTab.CONSUMABLES)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemPill> plan_c = pill("plan_c", HbmTab.CONSUMABLES)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemPill> pill_red = pill("pill_red", HbmTab.CONSUMABLES)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemPill> chocolate = pill("chocolate", HbmTab.CONSUMABLES)
            .transform(Presets.sprite());

    public static final RegistryObject<ItemSimpleConsumable> radaway = simple("radaway", HbmTab.CONSUMABLES)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemSimpleConsumable> radaway_strong = simple("radaway_strong", HbmTab.CONSUMABLES)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemSimpleConsumable> radaway_flush = simple("radaway_flush", HbmTab.CONSUMABLES)
            .transform(Presets.sprite());

    public static final RegistryObject<ItemConsumable> jetpack_tank = consumable("jetpack_tank", HbmTab.CONSUMABLES, true)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemConsumable> cbt_device = consumable("cbt_device", HbmTab.CONSUMABLES, true)
            .transform(Presets.sprite());
    public static final RegistryObject<ItemConsumable> euphemium_stopper = consumable("euphemium_stopper", null, true)
            .transform(Presets.sprite());

    public static void init() {}
}
