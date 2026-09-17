package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.weapon.sedna.factory.GunFactory;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;

/** The four source ItemEnumMulti weapon-mod carriers, flattened onto ITEM_VARIANT. */
public final class ModWeaponModItems {

    private ModWeaponModItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<ItemEnumMulti> weapon_mod_test = register(
            "weapon_mod_test", GunFactory.EnumModTest.class);
    public static final RegistryObject<ItemEnumMulti> weapon_mod_generic = register(
            "weapon_mod_generic", GunFactory.EnumModGeneric.class);
    public static final RegistryObject<ItemEnumMulti> weapon_mod_special = register(
            "weapon_mod_special", GunFactory.EnumModSpecial.class);
    public static final RegistryObject<ItemEnumMulti> weapon_mod_caliber = register(
            "weapon_mod_caliber", GunFactory.EnumModCaliber.class);

    private static RegistryObject<ItemEnumMulti> register(String id, Class<? extends Enum<?>> variants) {
        RegistryObject<ItemEnumMulti> item = ITEMS.registerItem(id,
                properties -> new ItemEnumMulti(variants, true, true, properties),
                properties -> properties.stacksTo(1));
        ModTabs.addTo(HbmTab.WEAPONS, item);
        return item;
    }

    public static void init() {}
}
