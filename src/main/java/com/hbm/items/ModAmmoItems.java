package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.Ids;
import dev.golem.core.dejson.Models;
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
    private static final Reg REG = Reg.of(ITEMS);

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
    public static final RegistryObject<Item> ammo_debug = REG.item("ammo_debug").transform(Presets.sprite());

    // GunFactory.EnumAmmo. The field suffix is the lower-case enum constant; the registry id is stable.
    public static final RegistryObject<Item> stone = REG.item("ammo_standard_stone").transform(Presets.sprite());
    public static final RegistryObject<Item> stone_ap = REG.item("ammo_standard_stone_ap").transform(Presets.sprite());
    public static final RegistryObject<Item> stone_iron = REG.item("ammo_standard_stone_iron").transform(Presets.sprite());
    public static final RegistryObject<Item> stone_shot = REG.item("ammo_standard_stone_shot").transform(Presets.sprite());
    public static final RegistryObject<Item> m357_bp = REG.item("ammo_standard_m357_bp").transform(Presets.sprite());
    public static final RegistryObject<Item> m357_sp = REG.item("ammo_standard_m357_sp").transform(Presets.sprite());
    public static final RegistryObject<Item> m357_fmj = REG.item("ammo_standard_m357_fmj").transform(Presets.sprite());
    public static final RegistryObject<Item> m357_jhp = REG.item("ammo_standard_m357_jhp").transform(Presets.sprite());
    public static final RegistryObject<Item> m357_ap = REG.item("ammo_standard_m357_ap").transform(Presets.sprite());
    public static final RegistryObject<Item> m357_express = REG.item("ammo_standard_m357_express").transform(Presets.sprite());
    public static final RegistryObject<Item> m44_bp = REG.item("ammo_standard_m44_bp").transform(Presets.sprite());
    public static final RegistryObject<Item> m44_sp = REG.item("ammo_standard_m44_sp").transform(Presets.sprite());
    public static final RegistryObject<Item> m44_fmj = REG.item("ammo_standard_m44_fmj").transform(Presets.sprite());
    public static final RegistryObject<Item> m44_jhp = REG.item("ammo_standard_m44_jhp").transform(Presets.sprite());
    public static final RegistryObject<Item> m44_ap = REG.item("ammo_standard_m44_ap").transform(Presets.sprite());
    public static final RegistryObject<Item> m44_express = REG.item("ammo_standard_m44_express").transform(Presets.sprite());
    public static final RegistryObject<Item> p22_sp = REG.item("ammo_standard_p22_sp").transform(Presets.sprite());
    public static final RegistryObject<Item> p22_fmj = REG.item("ammo_standard_p22_fmj").transform(Presets.sprite());
    public static final RegistryObject<Item> p22_jhp = REG.item("ammo_standard_p22_jhp").transform(Presets.sprite());
    public static final RegistryObject<Item> p22_ap = REG.item("ammo_standard_p22_ap").transform(Presets.sprite());
    public static final RegistryObject<Item> p9_sp = REG.item("ammo_standard_p9_sp").transform(Presets.sprite());
    public static final RegistryObject<Item> p9_fmj = REG.item("ammo_standard_p9_fmj").transform(Presets.sprite());
    public static final RegistryObject<Item> p9_jhp = REG.item("ammo_standard_p9_jhp").transform(Presets.sprite());
    public static final RegistryObject<Item> p9_ap = REG.item("ammo_standard_p9_ap").transform(Presets.sprite());
    public static final RegistryObject<Item> r556_sp = REG.item("ammo_standard_r556_sp").transform(Presets.sprite());
    public static final RegistryObject<Item> r556_fmj = REG.item("ammo_standard_r556_fmj").transform(Presets.sprite());
    public static final RegistryObject<Item> r556_jhp = REG.item("ammo_standard_r556_jhp").transform(Presets.sprite());
    public static final RegistryObject<Item> r556_ap = REG.item("ammo_standard_r556_ap").transform(Presets.sprite());
    public static final RegistryObject<Item> r762_sp = REG.item("ammo_standard_r762_sp").transform(Presets.sprite());
    public static final RegistryObject<Item> r762_fmj = REG.item("ammo_standard_r762_fmj").transform(Presets.sprite());
    public static final RegistryObject<Item> r762_jhp = REG.item("ammo_standard_r762_jhp").transform(Presets.sprite());
    public static final RegistryObject<Item> r762_ap = REG.item("ammo_standard_r762_ap").transform(Presets.sprite());
    public static final RegistryObject<Item> r762_du = REG.item("ammo_standard_r762_du").transform(Presets.sprite());
    public static final RegistryObject<Item> r762_he = REG.item("ammo_standard_r762_he").transform(Presets.sprite());
    public static final RegistryObject<Item> bmg50_sp = REG.item("ammo_standard_bmg50_sp").transform(Presets.sprite());
    public static final RegistryObject<Item> bmg50_fmj = REG.item("ammo_standard_bmg50_fmj").transform(Presets.sprite());
    public static final RegistryObject<Item> bmg50_jhp = REG.item("ammo_standard_bmg50_jhp").transform(Presets.sprite());
    public static final RegistryObject<Item> bmg50_ap = REG.item("ammo_standard_bmg50_ap").transform(Presets.sprite());
    public static final RegistryObject<Item> bmg50_du = REG.item("ammo_standard_bmg50_du").transform(Presets.sprite());
    public static final RegistryObject<Item> bmg50_he = REG.item("ammo_standard_bmg50_he").transform(Presets.sprite());
    public static final RegistryObject<Item> b75 = REG.item("ammo_standard_b75").transform(Presets.sprite());
    public static final RegistryObject<Item> b75_inc = REG.item("ammo_standard_b75_inc").transform(Presets.sprite());
    public static final RegistryObject<Item> b75_exp = REG.item("ammo_standard_b75_exp").transform(Presets.sprite());
    public static final RegistryObject<Item> g12_bp = REG.item("ammo_standard_g12_bp").transform(Presets.sprite());
    public static final RegistryObject<Item> g12_bp_magnum = REG.item("ammo_standard_g12_bp_magnum").transform(Presets.sprite());
    public static final RegistryObject<Item> g12_bp_slug = REG.item("ammo_standard_g12_bp_slug").transform(Presets.sprite());
    public static final RegistryObject<Item> g12 = REG.item("ammo_standard_g12").transform(Presets.sprite());
    public static final RegistryObject<Item> g12_slug = REG.item("ammo_standard_g12_slug").transform(Presets.sprite());
    public static final RegistryObject<Item> g12_flechette = REG.item("ammo_standard_g12_flechette").transform(Presets.sprite());
    public static final RegistryObject<Item> g12_magnum = REG.item("ammo_standard_g12_magnum").transform(Presets.sprite());
    public static final RegistryObject<Item> g12_explosive = REG.item("ammo_standard_g12_explosive").transform(Presets.sprite());
    public static final RegistryObject<Item> g12_phosphorus = REG.item("ammo_standard_g12_phosphorus").transform(Presets.sprite());
    public static final RegistryObject<Item> g26_flare = REG.item("ammo_standard_g26_flare").transform(Presets.sprite());
    public static final RegistryObject<Item> g26_flare_supply = REG.item("ammo_standard_g26_flare_supply").transform(Presets.sprite());
    public static final RegistryObject<Item> g26_flare_weapon = REG.item("ammo_standard_g26_flare_weapon").transform(Presets.sprite());
    public static final RegistryObject<Item> g40_he = REG.item("ammo_standard_g40_he").transform(Presets.sprite());
    public static final RegistryObject<Item> g40_heat = REG.item("ammo_standard_g40_heat").transform(Presets.sprite());
    public static final RegistryObject<Item> g40_demo = REG.item("ammo_standard_g40_demo").transform(Presets.sprite());
    public static final RegistryObject<Item> g40_inc = REG.item("ammo_standard_g40_inc").transform(Presets.sprite());
    public static final RegistryObject<Item> g40_phosphorus = REG.item("ammo_standard_g40_phosphorus").transform(Presets.sprite());
    public static final RegistryObject<Item> rocket_he = REG.item("ammo_standard_rocket_he").transform(Presets.sprite());
    public static final RegistryObject<Item> rocket_heat = REG.item("ammo_standard_rocket_heat").transform(Presets.sprite());
    public static final RegistryObject<Item> rocket_demo = REG.item("ammo_standard_rocket_demo").transform(Presets.sprite());
    public static final RegistryObject<Item> rocket_inc = REG.item("ammo_standard_rocket_inc").transform(Presets.sprite());
    public static final RegistryObject<Item> rocket_phosphorus = REG.item("ammo_standard_rocket_phosphorus").transform(Presets.sprite());
    public static final RegistryObject<Item> flame_diesel = REG.item("ammo_standard_flame_diesel").transform(Presets.sprite());
    public static final RegistryObject<Item> flame_gas = REG.item("ammo_standard_flame_gas").transform(Presets.sprite());
    public static final RegistryObject<Item> flame_napalm = REG.item("ammo_standard_flame_napalm").transform(Presets.sprite());
    public static final RegistryObject<Item> flame_balefire = REG.item("ammo_standard_flame_balefire").transform(Presets.sprite());
    public static final RegistryObject<Item> capacitor = REG.item("ammo_standard_capacitor").transform(Presets.sprite());
    public static final RegistryObject<Item> capacitor_overcharge = REG.item("ammo_standard_capacitor_overcharge").transform(Presets.sprite());
    public static final RegistryObject<Item> capacitor_ir = REG.item("ammo_standard_capacitor_ir").transform(Presets.sprite());
    public static final RegistryObject<Item> tau_uranium = REG.item("ammo_standard_tau_uranium").transform(Presets.sprite());
    public static final RegistryObject<Item> coil_tungsten = REG.item("ammo_standard_coil_tungsten").transform(Presets.sprite());
    public static final RegistryObject<Item> coil_ferrouranium = REG.item("ammo_standard_coil_ferrouranium").transform(Presets.sprite());
    public static final RegistryObject<Item> nuke_standard = REG.item("ammo_standard_nuke_standard").transform(Presets.sprite());
    public static final RegistryObject<Item> nuke_demo = REG.item("ammo_standard_nuke_demo").transform(Presets.sprite());
    public static final RegistryObject<Item> nuke_high = REG.item("ammo_standard_nuke_high").transform(Presets.sprite());
    /** Existing factory integration name; this aliases the flattened nuke_high entry. */
    public static final RegistryObject<Item> ammo_standard_nuke_high = nuke_high;
    public static final RegistryObject<Item> nuke_tots = REG.item("ammo_standard_nuke_tots").transform(Presets.sprite());
    public static final RegistryObject<Item> nuke_hive = REG.item("ammo_standard_nuke_hive").transform(Presets.sprite());
    public static final RegistryObject<Item> g10 = REG.item("ammo_standard_g10").transform(Presets.sprite());
    public static final RegistryObject<Item> g10_shrapnel = REG.item("ammo_standard_g10_shrapnel").transform(Presets.sprite());
    public static final RegistryObject<Item> g10_du = REG.item("ammo_standard_g10_du").transform(Presets.sprite());
    public static final RegistryObject<Item> g10_slug = REG.item("ammo_standard_g10_slug").transform(Presets.sprite());
    public static final RegistryObject<Item> g10_explosive = REG.item("ammo_standard_g10_explosive").transform(Presets.sprite());
    public static final RegistryObject<Item> p45_sp = REG.item("ammo_standard_p45_sp").transform(Presets.sprite());
    public static final RegistryObject<Item> p45_fmj = REG.item("ammo_standard_p45_fmj").transform(Presets.sprite());
    public static final RegistryObject<Item> p45_jhp = REG.item("ammo_standard_p45_jhp").transform(Presets.sprite());
    public static final RegistryObject<Item> p45_ap = REG.item("ammo_standard_p45_ap").transform(Presets.sprite());
    public static final RegistryObject<Item> p45_du = REG.item("ammo_standard_p45_du").transform(Presets.sprite());
    public static final RegistryObject<Item> ct_hook = REG.item("ammo_standard_ct_hook").transform(Presets.sprite());
    public static final RegistryObject<Item> ct_mortar = REG.item("ammo_standard_ct_mortar").transform(Presets.sprite());
    public static final RegistryObject<Item> ct_mortar_charge = REG.item("ammo_standard_ct_mortar_charge").transform(Presets.sprite());
    public static final RegistryObject<Item> nuke_balefire = REG.item("ammo_standard_nuke_balefire").transform(Presets.sprite());
    public static final RegistryObject<Item> bmg50_sm = REG.item("ammo_standard_bmg50_sm").transform(Presets.sprite());

    // GunFactory.EnumAmmoSecret. These are registered but intentionally not placed in the creative tab.
    public static final RegistryObject<Item> folly_sm = REG.item("ammo_secret_folly_sm").transform(Presets.sprite());
    public static final RegistryObject<Item> folly_nuke = REG.item("ammo_secret_folly_nuke").transform(Presets.sprite());
    public static final RegistryObject<Item> m44_equestrian = REG.item("ammo_secret_m44_equestrian").transform(Presets.sprite());
    public static final RegistryObject<Item> g12_equestrian = REG.item("ammo_secret_g12_equestrian").transform(Presets.sprite());
    public static final RegistryObject<Item> bmg50_equestrian = REG.item("ammo_secret_bmg50_equestrian").transform(Presets.sprite());
    public static final RegistryObject<Item> p35_800 = REG.item("ammo_secret_p35_800").transform(Presets.sprite());
    public static final RegistryObject<Item> bmg50_black = REG.item("ammo_secret_bmg50_black").transform(Presets.sprite());
    public static final RegistryObject<Item> p35_800_bl = REG.item("ammo_secret_p35_800_bl").transform(Presets.sprite());

    // ItemAmmoEnums.AmmoFireExt. The primary enum value keeps original-hbm's ammo_fireext id.
    public static final RegistryObject<Item> fext_water = REG.item(
            ItemAmmoEnums.AmmoFireExt.WATER.getInternalName()).transform(Presets.sprite());
    public static final RegistryObject<Item> fext_foam = REG.item(
            ItemAmmoEnums.AmmoFireExt.FOAM.getInternalName()).transform(Presets.sprite());
    public static final RegistryObject<Item> fext_sand = REG.item(
            ItemAmmoEnums.AmmoFireExt.SAND.getInternalName()).transform(Presets.sprite());

    // ItemAmmoEnums.Ammo240Shell remains a one-item family in the source port's turret API.
    public static final RegistryObject<Item> shell_normal = REG.item(
            ItemAmmoEnums.Ammo240Shell.STOCK.getInternalName()).transform(Presets.sprite());
    public static final RegistryObject<Item> shell_explosive = REG.item(
            ItemAmmoEnums.Ammo240Shell.EXPLOSIVE.getInternalName()).transform(Presets.sprite());
    public static final RegistryObject<Item> shell_ap = REG.item(
            ItemAmmoEnums.Ammo240Shell.APFSDS_T.getInternalName()).transform(Presets.sprite());
    public static final RegistryObject<Item> shell_du = REG.item(
            ItemAmmoEnums.Ammo240Shell.APFSDS_DU.getInternalName()).transform(Presets.sprite());
    public static final RegistryObject<Item> shell_w9 = REG.item(
            ItemAmmoEnums.Ammo240Shell.W9.getInternalName()).transform(Presets.sprite());

    public static final RegistryObject<com.hbm.items.weapon.ItemAmmoHIMARS> ammo_himars =
            REG.item("ammo_himars", com.hbm.items.weapon.ItemAmmoHIMARS::new);
    public static final RegistryObject<com.hbm.items.weapon.ItemAmmoArty> ammo_arty =
            REG.item("ammo_arty", com.hbm.items.weapon.ItemAmmoArty::new)
                    .icon(d -> d.models(artySprites()).selects(Constants.MOD_ID + ":item_variant"));
    public static final RegistryObject<com.hbm.items.tool.ItemDesignatorArtyRange> designator_arty_range =
            REG.item("designator_arty_range", com.hbm.items.tool.ItemDesignatorArtyRange::new)
                    .transform(Presets.handheld());

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
                designator_arty_range);
        addToWeapons(fext_water, fext_foam, fext_sand);
        ModTabs.addVariantsTo(HbmTab.WEAPONS, ammo_himars);
        ModTabs.addVariantsTo(HbmTab.WEAPONS, ammo_arty);
    }

    /**
     * The twelve shells pick their sprite from {@code hbm:item_variant}: case <i>n</i> is {@code ItemAmmoArty}
     * shell <i>n</i>, so this list is the shell order and nothing else. (It shipped as one sprite before, so all
     * twelve drew the plain shell.)
     */
    private static Models.Group artySprites() {
        Models.Group models = Models.group();
        for (String suffix : new String[] {"", "_classic", "_he", "_mini_nuke", "_nuke", "_phosphorus",
                "_mini_nuke_multi", "_phosphorus_multi", "_cargo", "_chlorine", "_phosgene", "_mustard_gas"}) {
            models = models.and(suffix, Models.generated(Constants.MOD_ID + ":items/ammo_arty" + suffix));
        }
        return models;
    }

    @SafeVarargs
    private static void addToWeapons(RegistryObject<? extends Item>... entries) {
        for (RegistryObject<? extends Item> entry : entries) ModTabs.addTo(HbmTab.WEAPONS, entry);
    }

    public static void init() {}
}
