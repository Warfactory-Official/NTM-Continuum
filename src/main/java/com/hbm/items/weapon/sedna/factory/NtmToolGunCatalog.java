package com.hbm.items.weapon.sedna.factory;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.weapon.sedna.impl.ItemGunDrill;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;

/** Registry boundary for the tool and power-armour guns restored from ntm-next. */
public final class NtmToolGunCatalog {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static RegistryObject<ItemGunDrill> gun_drill;
    public static RegistryObject<ItemGunBaseNT> gun_charge_thrower;
    public static RegistryObject<XFactoryPA.ItemGunPA> gun_pa_melee;
    public static RegistryObject<XFactoryPA.ItemGunPA> gun_pa_ranged;
    public static RegistryObject<ItemGunBaseNT> gun_fireext;

    private NtmToolGunCatalog() {}

    /** Resolves the flattened ammo identity without constructing an ItemStack during bootstrap. */
    public static Supplier<Item> standardAmmo(String enumName) {
        return ModAmmoItems.standardAmmo(enumName);
    }

    /** Declares a tool gun and keeps its source order in the ordinary weapons tab. */
    public static <T extends Item> RegistryObject<T> weapon(String id, Function<Item.Properties, ? extends T> factory) {
        RegistryObject<T> item = ITEMS.registerItem(id, factory);
        ModTabs.addTo(HbmTab.WEAPONS, item);
        return item;
    }
}
