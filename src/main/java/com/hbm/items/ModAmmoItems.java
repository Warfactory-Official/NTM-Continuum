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

    /** The old {@code GunFactory.EnumAmmo.NUKE_HIGH}, dropped by a destroyed micro missile. */
    public static final RegistryObject<Item> ammo_standard_nuke_high =
            ITEMS.registerSimpleItem("ammo_standard_nuke_high");

    // .357 Magnum, in the order upstream declares them: this is also reload priority, so an empty
    // magazine picks up black powder before it picks up armour piercing.
    public static final RegistryObject<Item> m357_bp = ITEMS.registerSimpleItem("ammo_standard_m357_bp");
    public static final RegistryObject<Item> m357_sp = ITEMS.registerSimpleItem("ammo_standard_m357_sp");
    public static final RegistryObject<Item> m357_fmj = ITEMS.registerSimpleItem("ammo_standard_m357_fmj");
    public static final RegistryObject<Item> m357_jhp = ITEMS.registerSimpleItem("ammo_standard_m357_jhp");
    public static final RegistryObject<Item> m357_ap = ITEMS.registerSimpleItem("ammo_standard_m357_ap");
    public static final RegistryObject<Item> m357_express = ITEMS.registerSimpleItem("ammo_standard_m357_express");

    // .44 Magnum, in the order upstream declares them.
    public static final RegistryObject<Item> m44_bp = ITEMS.registerSimpleItem("ammo_standard_m44_bp");
    public static final RegistryObject<Item> m44_sp = ITEMS.registerSimpleItem("ammo_standard_m44_sp");
    public static final RegistryObject<Item> m44_fmj = ITEMS.registerSimpleItem("ammo_standard_m44_fmj");
    public static final RegistryObject<Item> m44_jhp = ITEMS.registerSimpleItem("ammo_standard_m44_jhp");
    public static final RegistryObject<Item> m44_ap = ITEMS.registerSimpleItem("ammo_standard_m44_ap");
    public static final RegistryObject<Item> m44_express = ITEMS.registerSimpleItem("ammo_standard_m44_express");
    /** The old metadata item {@code ammo_secret:M44_EQUESTRIAN}, flattened to one item. */
    public static final RegistryObject<Item> m44_equestrian = ITEMS.registerSimpleItem("ammo_secret_m44_equestrian");

    // .22 LR, in the order upstream declares them: magazines prefer soft point first.
    public static final RegistryObject<Item> p22_sp = ITEMS.registerSimpleItem("ammo_standard_p22_sp");
    public static final RegistryObject<Item> p22_fmj = ITEMS.registerSimpleItem("ammo_standard_p22_fmj");
    public static final RegistryObject<Item> p22_jhp = ITEMS.registerSimpleItem("ammo_standard_p22_jhp");
    public static final RegistryObject<Item> p22_ap = ITEMS.registerSimpleItem("ammo_standard_p22_ap");

    // 9 mm, in the order upstream declares them: magazines prefer soft point first.
    public static final RegistryObject<Item> p9_sp = ITEMS.registerSimpleItem("ammo_standard_p9_sp");
    public static final RegistryObject<Item> p9_fmj = ITEMS.registerSimpleItem("ammo_standard_p9_fmj");
    public static final RegistryObject<Item> p9_jhp = ITEMS.registerSimpleItem("ammo_standard_p9_jhp");
    public static final RegistryObject<Item> p9_ap = ITEMS.registerSimpleItem("ammo_standard_p9_ap");

    // Black-powder Pepperbox loads, in upstream priority order.
    public static final RegistryObject<Item> stone = ITEMS.registerSimpleItem("ammo_standard_stone");
    public static final RegistryObject<Item> stone_ap = ITEMS.registerSimpleItem("ammo_standard_stone_ap");
    public static final RegistryObject<Item> stone_iron = ITEMS.registerSimpleItem("ammo_standard_stone_iron");
    public static final RegistryObject<Item> stone_shot = ITEMS.registerSimpleItem("ammo_standard_stone_shot");

    // 5.56 mm, in the order upstream declares the ordinary G3 family.
    public static final RegistryObject<Item> r556_sp = ITEMS.registerSimpleItem("ammo_standard_r556_sp");
    public static final RegistryObject<Item> r556_fmj = ITEMS.registerSimpleItem("ammo_standard_r556_fmj");
    public static final RegistryObject<Item> r556_jhp = ITEMS.registerSimpleItem("ammo_standard_r556_jhp");
    public static final RegistryObject<Item> r556_ap = ITEMS.registerSimpleItem("ammo_standard_r556_ap");

    // .50 BMG, in the order upstream declares the M2/AMAT family. The two {@code ammo_secret} loads are
    // absent: both hang off entities this port does not have.
    public static final RegistryObject<Item> bmg50_sp = ITEMS.registerSimpleItem("ammo_standard_bmg50_sp");
    public static final RegistryObject<Item> bmg50_fmj = ITEMS.registerSimpleItem("ammo_standard_bmg50_fmj");
    public static final RegistryObject<Item> bmg50_jhp = ITEMS.registerSimpleItem("ammo_standard_bmg50_jhp");
    public static final RegistryObject<Item> bmg50_ap = ITEMS.registerSimpleItem("ammo_standard_bmg50_ap");
    public static final RegistryObject<Item> bmg50_du = ITEMS.registerSimpleItem("ammo_standard_bmg50_du");
    public static final RegistryObject<Item> bmg50_he = ITEMS.registerSimpleItem("ammo_standard_bmg50_he");
    public static final RegistryObject<Item> bmg50_sm = ITEMS.registerSimpleItem("ammo_standard_bmg50_sm");

    /** The Tauon turret's uranium slug. The accelerator weapons it also feeds are unported. */
    public static final RegistryObject<Item> tau_uranium = ITEMS.registerSimpleItem("ammo_standard_tau_uranium");

    // 240 mm shells, in the order the Jeremy and the artillery feed them.
    public static final RegistryObject<Item> shell_normal = ITEMS.registerSimpleItem("ammo_shell");
    public static final RegistryObject<Item> shell_explosive = ITEMS.registerSimpleItem("ammo_shell_explosive");
    public static final RegistryObject<Item> shell_ap = ITEMS.registerSimpleItem("ammo_shell_apfsds_t");
    public static final RegistryObject<Item> shell_du = ITEMS.registerSimpleItem("ammo_shell_apfsds_du");
    public static final RegistryObject<Item> shell_w9 = ITEMS.registerSimpleItem("ammo_shell_w9");

    static {
        // Old weaponTab, which upstream also files ammo under.
        for (RegistryObject<Item> ammo : new RegistryObject[] {
                 ammo_debug, ammo_standard_nuke_high, m357_bp, m357_sp, m357_fmj, m357_jhp, m357_ap, m357_express,
                 m44_bp, m44_sp, m44_fmj, m44_jhp, m44_ap, m44_express,
                 p22_sp, p22_fmj, p22_jhp, p22_ap, p9_sp, p9_fmj, p9_jhp, p9_ap,
                 stone, stone_ap, stone_iron, stone_shot, r556_sp, r556_fmj, r556_jhp, r556_ap,
                 bmg50_sp, bmg50_fmj, bmg50_jhp, bmg50_ap, bmg50_du, bmg50_he, bmg50_sm, tau_uranium,
                 shell_normal, shell_explosive, shell_ap, shell_du, shell_w9}) {
            com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, ammo);
        }
    }

    public static void init() {}
}
