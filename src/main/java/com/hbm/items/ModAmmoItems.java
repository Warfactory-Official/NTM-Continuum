package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.Ids;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

/** Every ammunition identity represented by original-hbm's weapon catalogue. */
public final class ModAmmoItems {

    private ModAmmoItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** Returns an already-declared ammo item without creating another registry entry. */
    public static Supplier<Item> ammo(String id) {
        if (!id.startsWith("ammo_")) throw new IllegalArgumentException("Not an ammo id: " + id);
        return () -> BuiltInRegistries.ITEM.getValue(Ids.of(Constants.MOD_ID, id));
    }

    /** Resolves the flattened identity produced from GunFactory.EnumAmmo. */
    public static Supplier<Item> standardAmmo(String enumName) {
        String suffix = enumName.toLowerCase(Locale.ROOT);
        String id = switch (suffix) {
            // ItemAmmoEnums.AmmoFireExt kept these source ids instead of using the Sedna family prefix.
            case "fext_water" -> "ammo_fireext";
            case "fext_foam" -> "ammo_fireext_foam";
            case "fext_sand" -> "ammo_fireext_sand";
            default -> "ammo_standard_" + suffix;
        };
        return ammo(id);
    }

    /** Resolves the flattened identity produced from GunFactory.EnumAmmoSecret. */
    public static Supplier<Item> secretAmmo(String enumName) {
        return ammo("ammo_secret_" + enumName.toLowerCase(Locale.ROOT));
    }

    /** GunFactory.ammo_debug; the source item is deliberately separate from EnumAmmo. */
    public static final RegistryObject<Item> ammo_debug = ITEMS.registerSimpleItem("ammo_debug");

    // GunFactory.EnumAmmo. The field suffix is the lower-case enum constant; the registry id is stable.
    public static final RegistryObject<Item> stone = ITEMS.registerSimpleItem("ammo_standard_stone");
    public static final RegistryObject<Item> stone_ap = ITEMS.registerSimpleItem("ammo_standard_stone_ap");
    public static final RegistryObject<Item> stone_iron = ITEMS.registerSimpleItem("ammo_standard_stone_iron");
    public static final RegistryObject<Item> stone_shot = ITEMS.registerSimpleItem("ammo_standard_stone_shot");
    public static final RegistryObject<Item> m357_bp = ITEMS.registerSimpleItem("ammo_standard_m357_bp");
    public static final RegistryObject<Item> m357_sp = ITEMS.registerSimpleItem("ammo_standard_m357_sp");
    public static final RegistryObject<Item> m357_fmj = ITEMS.registerSimpleItem("ammo_standard_m357_fmj");
    public static final RegistryObject<Item> m357_jhp = ITEMS.registerSimpleItem("ammo_standard_m357_jhp");
    public static final RegistryObject<Item> m357_ap = ITEMS.registerSimpleItem("ammo_standard_m357_ap");
    public static final RegistryObject<Item> m357_express = ITEMS.registerSimpleItem("ammo_standard_m357_express");
    public static final RegistryObject<Item> m44_bp = ITEMS.registerSimpleItem("ammo_standard_m44_bp");
    public static final RegistryObject<Item> m44_sp = ITEMS.registerSimpleItem("ammo_standard_m44_sp");
    public static final RegistryObject<Item> m44_fmj = ITEMS.registerSimpleItem("ammo_standard_m44_fmj");
    public static final RegistryObject<Item> m44_jhp = ITEMS.registerSimpleItem("ammo_standard_m44_jhp");
    public static final RegistryObject<Item> m44_ap = ITEMS.registerSimpleItem("ammo_standard_m44_ap");
    public static final RegistryObject<Item> m44_express = ITEMS.registerSimpleItem("ammo_standard_m44_express");
    public static final RegistryObject<Item> p22_sp = ITEMS.registerSimpleItem("ammo_standard_p22_sp");
    public static final RegistryObject<Item> p22_fmj = ITEMS.registerSimpleItem("ammo_standard_p22_fmj");
    public static final RegistryObject<Item> p22_jhp = ITEMS.registerSimpleItem("ammo_standard_p22_jhp");
    public static final RegistryObject<Item> p22_ap = ITEMS.registerSimpleItem("ammo_standard_p22_ap");
    public static final RegistryObject<Item> p9_sp = ITEMS.registerSimpleItem("ammo_standard_p9_sp");
    public static final RegistryObject<Item> p9_fmj = ITEMS.registerSimpleItem("ammo_standard_p9_fmj");
    public static final RegistryObject<Item> p9_jhp = ITEMS.registerSimpleItem("ammo_standard_p9_jhp");
    public static final RegistryObject<Item> p9_ap = ITEMS.registerSimpleItem("ammo_standard_p9_ap");
    public static final RegistryObject<Item> r556_sp = ITEMS.registerSimpleItem("ammo_standard_r556_sp");
    public static final RegistryObject<Item> r556_fmj = ITEMS.registerSimpleItem("ammo_standard_r556_fmj");
    public static final RegistryObject<Item> r556_jhp = ITEMS.registerSimpleItem("ammo_standard_r556_jhp");
    public static final RegistryObject<Item> r556_ap = ITEMS.registerSimpleItem("ammo_standard_r556_ap");
    public static final RegistryObject<Item> r762_sp = ITEMS.registerSimpleItem("ammo_standard_r762_sp");
    public static final RegistryObject<Item> r762_fmj = ITEMS.registerSimpleItem("ammo_standard_r762_fmj");
    public static final RegistryObject<Item> r762_jhp = ITEMS.registerSimpleItem("ammo_standard_r762_jhp");
    public static final RegistryObject<Item> r762_ap = ITEMS.registerSimpleItem("ammo_standard_r762_ap");
    public static final RegistryObject<Item> r762_du = ITEMS.registerSimpleItem("ammo_standard_r762_du");
    public static final RegistryObject<Item> r762_he = ITEMS.registerSimpleItem("ammo_standard_r762_he");
    public static final RegistryObject<Item> bmg50_sp = ITEMS.registerSimpleItem("ammo_standard_bmg50_sp");
    public static final RegistryObject<Item> bmg50_fmj = ITEMS.registerSimpleItem("ammo_standard_bmg50_fmj");
    public static final RegistryObject<Item> bmg50_jhp = ITEMS.registerSimpleItem("ammo_standard_bmg50_jhp");
    public static final RegistryObject<Item> bmg50_ap = ITEMS.registerSimpleItem("ammo_standard_bmg50_ap");
    public static final RegistryObject<Item> bmg50_du = ITEMS.registerSimpleItem("ammo_standard_bmg50_du");
    public static final RegistryObject<Item> bmg50_he = ITEMS.registerSimpleItem("ammo_standard_bmg50_he");
    public static final RegistryObject<Item> b75 = ITEMS.registerSimpleItem("ammo_standard_b75");
    public static final RegistryObject<Item> b75_inc = ITEMS.registerSimpleItem("ammo_standard_b75_inc");
    public static final RegistryObject<Item> b75_exp = ITEMS.registerSimpleItem("ammo_standard_b75_exp");
    public static final RegistryObject<Item> g12_bp = ITEMS.registerSimpleItem("ammo_standard_g12_bp");
    public static final RegistryObject<Item> g12_bp_magnum = ITEMS.registerSimpleItem("ammo_standard_g12_bp_magnum");
    public static final RegistryObject<Item> g12_bp_slug = ITEMS.registerSimpleItem("ammo_standard_g12_bp_slug");
    public static final RegistryObject<Item> g12 = ITEMS.registerSimpleItem("ammo_standard_g12");
    public static final RegistryObject<Item> g12_slug = ITEMS.registerSimpleItem("ammo_standard_g12_slug");
    public static final RegistryObject<Item> g12_flechette = ITEMS.registerSimpleItem("ammo_standard_g12_flechette");
    public static final RegistryObject<Item> g12_magnum = ITEMS.registerSimpleItem("ammo_standard_g12_magnum");
    public static final RegistryObject<Item> g12_explosive = ITEMS.registerSimpleItem("ammo_standard_g12_explosive");
    public static final RegistryObject<Item> g12_phosphorus = ITEMS.registerSimpleItem("ammo_standard_g12_phosphorus");
    public static final RegistryObject<Item> g26_flare = ITEMS.registerSimpleItem("ammo_standard_g26_flare");
    public static final RegistryObject<Item> g26_flare_supply = ITEMS.registerSimpleItem("ammo_standard_g26_flare_supply");
    public static final RegistryObject<Item> g26_flare_weapon = ITEMS.registerSimpleItem("ammo_standard_g26_flare_weapon");
    public static final RegistryObject<Item> g40_he = ITEMS.registerSimpleItem("ammo_standard_g40_he");
    public static final RegistryObject<Item> g40_heat = ITEMS.registerSimpleItem("ammo_standard_g40_heat");
    public static final RegistryObject<Item> g40_demo = ITEMS.registerSimpleItem("ammo_standard_g40_demo");
    public static final RegistryObject<Item> g40_inc = ITEMS.registerSimpleItem("ammo_standard_g40_inc");
    public static final RegistryObject<Item> g40_phosphorus = ITEMS.registerSimpleItem("ammo_standard_g40_phosphorus");
    public static final RegistryObject<Item> rocket_he = ITEMS.registerSimpleItem("ammo_standard_rocket_he");
    public static final RegistryObject<Item> rocket_heat = ITEMS.registerSimpleItem("ammo_standard_rocket_heat");
    public static final RegistryObject<Item> rocket_demo = ITEMS.registerSimpleItem("ammo_standard_rocket_demo");
    public static final RegistryObject<Item> rocket_inc = ITEMS.registerSimpleItem("ammo_standard_rocket_inc");
    public static final RegistryObject<Item> rocket_phosphorus = ITEMS.registerSimpleItem("ammo_standard_rocket_phosphorus");
    public static final RegistryObject<Item> flame_diesel = ITEMS.registerSimpleItem("ammo_standard_flame_diesel");
    public static final RegistryObject<Item> flame_gas = ITEMS.registerSimpleItem("ammo_standard_flame_gas");
    public static final RegistryObject<Item> flame_napalm = ITEMS.registerSimpleItem("ammo_standard_flame_napalm");
    public static final RegistryObject<Item> flame_balefire = ITEMS.registerSimpleItem("ammo_standard_flame_balefire");
    public static final RegistryObject<Item> capacitor = ITEMS.registerSimpleItem("ammo_standard_capacitor");
    public static final RegistryObject<Item> capacitor_overcharge = ITEMS.registerSimpleItem("ammo_standard_capacitor_overcharge");
    public static final RegistryObject<Item> capacitor_ir = ITEMS.registerSimpleItem("ammo_standard_capacitor_ir");
    public static final RegistryObject<Item> tau_uranium = ITEMS.registerSimpleItem("ammo_standard_tau_uranium");
    public static final RegistryObject<Item> coil_tungsten = ITEMS.registerSimpleItem("ammo_standard_coil_tungsten");
    public static final RegistryObject<Item> coil_ferrouranium = ITEMS.registerSimpleItem("ammo_standard_coil_ferrouranium");
    public static final RegistryObject<Item> nuke_standard = ITEMS.registerSimpleItem("ammo_standard_nuke_standard");
    public static final RegistryObject<Item> nuke_demo = ITEMS.registerSimpleItem("ammo_standard_nuke_demo");
    public static final RegistryObject<Item> nuke_high = ITEMS.registerSimpleItem("ammo_standard_nuke_high");
    /** Existing factory integration name; this aliases the flattened nuke_high entry. */
    public static final RegistryObject<Item> ammo_standard_nuke_high = nuke_high;
    public static final RegistryObject<Item> nuke_tots = ITEMS.registerSimpleItem("ammo_standard_nuke_tots");
    public static final RegistryObject<Item> nuke_hive = ITEMS.registerSimpleItem("ammo_standard_nuke_hive");
    public static final RegistryObject<Item> g10 = ITEMS.registerSimpleItem("ammo_standard_g10");
    public static final RegistryObject<Item> g10_shrapnel = ITEMS.registerSimpleItem("ammo_standard_g10_shrapnel");
    public static final RegistryObject<Item> g10_du = ITEMS.registerSimpleItem("ammo_standard_g10_du");
    public static final RegistryObject<Item> g10_slug = ITEMS.registerSimpleItem("ammo_standard_g10_slug");
    public static final RegistryObject<Item> g10_explosive = ITEMS.registerSimpleItem("ammo_standard_g10_explosive");
    public static final RegistryObject<Item> p45_sp = ITEMS.registerSimpleItem("ammo_standard_p45_sp");
    public static final RegistryObject<Item> p45_fmj = ITEMS.registerSimpleItem("ammo_standard_p45_fmj");
    public static final RegistryObject<Item> p45_jhp = ITEMS.registerSimpleItem("ammo_standard_p45_jhp");
    public static final RegistryObject<Item> p45_ap = ITEMS.registerSimpleItem("ammo_standard_p45_ap");
    public static final RegistryObject<Item> p45_du = ITEMS.registerSimpleItem("ammo_standard_p45_du");
    public static final RegistryObject<Item> ct_hook = ITEMS.registerSimpleItem("ammo_standard_ct_hook");
    public static final RegistryObject<Item> ct_mortar = ITEMS.registerSimpleItem("ammo_standard_ct_mortar");
    public static final RegistryObject<Item> ct_mortar_charge = ITEMS.registerSimpleItem("ammo_standard_ct_mortar_charge");
    public static final RegistryObject<Item> nuke_balefire = ITEMS.registerSimpleItem("ammo_standard_nuke_balefire");
    public static final RegistryObject<Item> bmg50_sm = ITEMS.registerSimpleItem("ammo_standard_bmg50_sm");

    // GunFactory.EnumAmmoSecret. These are registered but intentionally not placed in the creative tab.
    public static final RegistryObject<Item> folly_sm = ITEMS.registerSimpleItem("ammo_secret_folly_sm");
    public static final RegistryObject<Item> folly_nuke = ITEMS.registerSimpleItem("ammo_secret_folly_nuke");
    public static final RegistryObject<Item> m44_equestrian = ITEMS.registerSimpleItem("ammo_secret_m44_equestrian");
    public static final RegistryObject<Item> g12_equestrian = ITEMS.registerSimpleItem("ammo_secret_g12_equestrian");
    public static final RegistryObject<Item> bmg50_equestrian = ITEMS.registerSimpleItem("ammo_secret_bmg50_equestrian");
    public static final RegistryObject<Item> p35_800 = ITEMS.registerSimpleItem("ammo_secret_p35_800");
    public static final RegistryObject<Item> bmg50_black = ITEMS.registerSimpleItem("ammo_secret_bmg50_black");
    public static final RegistryObject<Item> p35_800_bl = ITEMS.registerSimpleItem("ammo_secret_p35_800_bl");

    // ItemAmmoEnums.AmmoFireExt. The primary enum value keeps original-hbm's ammo_fireext id.
    public static final RegistryObject<Item> fext_water = ITEMS.registerSimpleItem(
            ItemAmmoEnums.AmmoFireExt.WATER.getInternalName());
    public static final RegistryObject<Item> fext_foam = ITEMS.registerSimpleItem(
            ItemAmmoEnums.AmmoFireExt.FOAM.getInternalName());
    public static final RegistryObject<Item> fext_sand = ITEMS.registerSimpleItem(
            ItemAmmoEnums.AmmoFireExt.SAND.getInternalName());

    // ItemAmmoEnums.Ammo240Shell remains a one-item family in the source port's turret API.
    public static final RegistryObject<Item> shell_normal = ITEMS.registerSimpleItem(
            ItemAmmoEnums.Ammo240Shell.STOCK.getInternalName());
    public static final RegistryObject<Item> shell_explosive = ITEMS.registerSimpleItem(
            ItemAmmoEnums.Ammo240Shell.EXPLOSIVE.getInternalName());
    public static final RegistryObject<Item> shell_ap = ITEMS.registerSimpleItem(
            ItemAmmoEnums.Ammo240Shell.APFSDS_T.getInternalName());
    public static final RegistryObject<Item> shell_du = ITEMS.registerSimpleItem(
            ItemAmmoEnums.Ammo240Shell.APFSDS_DU.getInternalName());
    public static final RegistryObject<Item> shell_w9 = ITEMS.registerSimpleItem(
            ItemAmmoEnums.Ammo240Shell.W9.getInternalName());

    public static final RegistryObject<com.hbm.items.weapon.ItemAmmoHIMARS> ammo_himars =
            ITEMS.registerItem("ammo_himars", com.hbm.items.weapon.ItemAmmoHIMARS::new);
    public static final RegistryObject<com.hbm.items.weapon.ItemAmmoArty> ammo_arty =
            ITEMS.registerItem("ammo_arty", com.hbm.items.weapon.ItemAmmoArty::new);
    public static final RegistryObject<com.hbm.items.tool.ItemDesignatorArtyRange> designator_arty_range =
            ITEMS.registerItem("designator_arty_range", com.hbm.items.tool.ItemDesignatorArtyRange::new);

    static {
        addToWeapons(
                ammo_debug,
                stone, stone_ap, stone_iron, stone_shot,
                m357_bp, m357_sp, m357_fmj, m357_jhp, m357_ap, m357_express,
                m44_bp, m44_sp, m44_fmj, m44_jhp, m44_ap, m44_express,
                p22_sp, p22_fmj, p22_jhp, p22_ap,
                p9_sp, p9_fmj, p9_jhp, p9_ap,
                r556_sp, r556_fmj, r556_jhp, r556_ap,
                r762_sp, r762_fmj, r762_jhp, r762_ap, r762_du, r762_he,
                bmg50_sp, bmg50_fmj, bmg50_jhp, bmg50_ap, bmg50_du, bmg50_he, bmg50_sm,
                b75, b75_inc, b75_exp,
                g12_bp, g12_bp_magnum, g12_bp_slug, g12, g12_slug, g12_flechette, g12_magnum,
                g12_explosive, g12_phosphorus,
                g26_flare, g26_flare_supply, g26_flare_weapon,
                g40_he, g40_heat, g40_demo, g40_inc, g40_phosphorus,
                rocket_he, rocket_heat, rocket_demo, rocket_inc, rocket_phosphorus,
                flame_diesel, flame_gas, flame_napalm, flame_balefire,
                capacitor, capacitor_overcharge, capacitor_ir, tau_uranium,
                coil_tungsten, coil_ferrouranium,
                nuke_standard, nuke_demo, nuke_high, nuke_tots, nuke_hive, nuke_balefire,
                g10, g10_shrapnel, g10_du, g10_slug, g10_explosive,
                p45_sp, p45_fmj, p45_jhp, p45_ap, p45_du,
                ct_hook, ct_mortar, ct_mortar_charge,
                shell_normal, shell_explosive, shell_ap, shell_du, shell_w9,
                ammo_himars, ammo_arty, designator_arty_range);
        addToWeapons(fext_water, fext_foam, fext_sand);
    }

    @SafeVarargs
    private static void addToWeapons(RegistryObject<? extends Item>... entries) {
        for (RegistryObject<? extends Item> entry : entries) ModTabs.addTo(HbmTab.WEAPONS, entry);
    }

    public static void init() {}
}
