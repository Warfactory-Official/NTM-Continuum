package com.hbm.items;

import com.hbm.Constants;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.Item;

/**
 * Ammunition for the Sedna weapons.
 *
 * <p>1.12.2 keeps every round on one {@code ammo_standard} item separated by metadata (its
 * {@code GunFactory.EnumAmmo} has around a hundred entries, with a standing warning to only ever append
 * to it). Metadata is gone, so each variant is registered as its own item named
 * {@code ammo_standard_<calibre>_<variant>} — the convention already used for the recipe intermediates.
 * Assets come from {@code port-tools/gen_ammo.py}, which reuses the original dot-named textures.
 *
 * <p>Only the calibres whose guns are ported live here; the rest arrive with their {@code XFactory}.
 */
public final class ModAmmoItems {

    private ModAmmoItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** The debug gun's round — upstream's {@code ammo_debug}, which reuses the .45 sprite. */
    public static final RegistryObject<Item> ammo_debug = ITEMS.registerSimpleItem("ammo_debug");

    // .357 Magnum, in the order upstream declares them: this is also reload priority, so an empty
    // magazine picks up black powder before it picks up armour piercing.
    public static final RegistryObject<Item> m357_bp = ITEMS.registerSimpleItem("ammo_standard_m357_bp");
    public static final RegistryObject<Item> m357_sp = ITEMS.registerSimpleItem("ammo_standard_m357_sp");
    public static final RegistryObject<Item> m357_fmj = ITEMS.registerSimpleItem("ammo_standard_m357_fmj");
    public static final RegistryObject<Item> m357_jhp = ITEMS.registerSimpleItem("ammo_standard_m357_jhp");
    public static final RegistryObject<Item> m357_ap = ITEMS.registerSimpleItem("ammo_standard_m357_ap");
    public static final RegistryObject<Item> m357_express = ITEMS.registerSimpleItem("ammo_standard_m357_express");

    static {
        // Old weaponTab, which upstream also files ammo under.
        for (RegistryObject<Item> ammo : new RegistryObject[] {
                ammo_debug, m357_bp, m357_sp, m357_fmj, m357_jhp, m357_ap, m357_express}) {
            com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, ammo);
        }
    }

    public static void init() {}
}
