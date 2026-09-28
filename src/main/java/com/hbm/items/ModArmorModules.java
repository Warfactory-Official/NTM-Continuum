package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.armor.ItemModBattery;
import com.hbm.items.armor.ItemModCladding;
import com.hbm.items.armor.ItemModInsert;
import com.hbm.items.armor.ItemModPolish;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.Item;

/** The active armor modules whose legacy behavior is complete on the current item/damage seams. */
public final class ModArmorModules {

    private ModArmorModules() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<ItemModCladding> cladding_paint = cladding("cladding_paint", 0.025D);
    public static final RegistryObject<ItemModCladding> cladding_rubber = cladding("cladding_rubber", 0.05D);
    public static final RegistryObject<ItemModCladding> cladding_lead = cladding("cladding_lead", 0.1D);
    public static final RegistryObject<ItemModCladding> cladding_desh = cladding("cladding_desh", 0.2D);
    public static final RegistryObject<ItemModCladding> cladding_ghiorsium = cladding("cladding_ghiorsium", 0.5D);

    public static final RegistryObject<ItemModInsert> insert_kevlar = insert("insert_kevlar", 1500, 1F, .9F, 1F, 1F);
    public static final RegistryObject<ItemModInsert> insert_sapi = insert("insert_sapi", 1750, 1F, .85F, 1F, 1F);
    public static final RegistryObject<ItemModInsert> insert_esapi = insert("insert_esapi", 2000, .95F, .8F, 1F, 1F);
    public static final RegistryObject<ItemModInsert> insert_xsapi = insert("insert_xsapi", 2500, .9F, .75F, 1F, 1F);
    public static final RegistryObject<ItemModInsert> insert_steel = insert("insert_steel", 1000, 1F, .95F, .75F, .95F);
    public static final RegistryObject<ItemModInsert> insert_du = insert("insert_du", 1500, .9F, .85F, .5F, .9F);
    public static final RegistryObject<ItemModInsert> insert_polonium = insert("insert_polonium", 500, .9F, 1F, .25F, .9F);
    public static final RegistryObject<ItemModInsert> insert_ghiorsium = insert("insert_ghiorsium", 2000, .8F, .75F, .35F, .9F);
    public static final RegistryObject<ItemModInsert> insert_yharonite = insert("insert_yharonite", 9999, .01F, 1F, 1F, 1F);
    public static final RegistryObject<ItemModInsert> insert_doxium = insert("insert_doxium", 9999, 5F, 1F, 1F, 1F);
    public static final RegistryObject<ItemModInsert> insert_era = insert("insert_era", 25, 0.5F, 1F, 0.25F, 1F);

    public static final RegistryObject<ItemModBattery> armor_battery = battery("armor_battery", 1.25D);
    public static final RegistryObject<ItemModBattery> armor_battery_mk2 = battery("armor_battery_mk2", 1.5D);
    public static final RegistryObject<ItemModBattery> armor_battery_mk3 = battery("armor_battery_mk3", 2D);
    public static final RegistryObject<ItemModPolish> armor_polish = ITEMS.registerItem(
            "armor_polish", ItemModPolish::new, p -> p.stacksTo(1));

    static {
        ModTabs.addTo(HbmTab.CONSUMABLES, armor_polish);
    }

    private static RegistryObject<ItemModCladding> cladding(String id, double resistance) {
        RegistryObject<ItemModCladding> item = ITEMS.registerItem(id,
                p -> new ItemModCladding(resistance, p), p -> p.stacksTo(1));
        ModTabs.addTo(HbmTab.CONSUMABLES, item);
        return item;
    }

    private static RegistryObject<ItemModInsert> insert(String id, int durability, float damage,
                                                         float projectile, float explosion, float speed) {
        RegistryObject<ItemModInsert> item = ITEMS.registerItem(id,
                p -> new ItemModInsert(durability, damage, projectile, explosion, speed, p));
        ModTabs.addTo(HbmTab.CONSUMABLES, item);
        return item;
    }

    private static RegistryObject<ItemModBattery> battery(String id, double multiplier) {
        RegistryObject<ItemModBattery> item = ITEMS.registerItem(id,
                p -> new ItemModBattery(multiplier, p), p -> p.stacksTo(1));
        ModTabs.addTo(HbmTab.CONSUMABLES, item);
        return item;
    }

    public static void init() {}
}
