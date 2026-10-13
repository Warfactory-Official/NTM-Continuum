package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.ItemFamily;
import com.hbm.items.ModGunItems;
import com.hbm.items.ModWeaponModItems;
import com.hbm.items.ItemEnumMulti;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.inventory.RecipesCommon;
import dev.golem.core.data.Tags;
import com.hbm.util.SavedIds;
import dev.golem.core.world.ItemStacks;
import dev.golem.core.registry.RegistryObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.GunFactory.*;
import com.hbm.items.weapon.sedna.factory.*;
import com.hbm.Constants;
import dev.golem.core.Ids;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Applies installed Sedna modifiers in the exact upstream priority order. Definitions are registered
 * by the item/registration owner; keeping the manager independent of item registries avoids bootstrap
 * order failures on Fabric.
 */
public final class XWeaponModManager {
    public static final String KEY_MOD_LIST = "KEY_MOD_LIST_";
    public static final int ID_SILENCER = 201;
    public static final int ID_SCOPE = 202;
    public static final int ID_SAWED_OFF = 203;
    public static final int ID_NO_SHIELD = 204;
    public static final int ID_NO_STOCK = 205;
    public static final int ID_GREASEGUN_CLEAN = 206;
    public static final int ID_MINIGUN_SPEED = 208;
    public static final int ID_FURNITURE_GREEN = 211;
    public static final int ID_FURNITURE_BLACK = 212;
    public static final int ID_MAS_BAYONET = 213;
    public static final int ID_UZI_SATURN = 215;
    public static final int ID_LAS_SHOTGUN = 216;
    public static final int ID_LAS_CAPACITOR = 217;
    public static final int ID_LAS_AUTO = 218;
    public static final int ID_CARBINE_BAYONET = 219;
    public static final int ID_NI4NI_NICKEL = 220;
    public static final int ID_NI4NI_DOUBLOONS = 221;
    public static final int ID_DRILL_HSS = 222;
    public static final int ID_DRILL_WSTEEL = 223;
    public static final int ID_DRILL_TCALLOY = 224;
    public static final int ID_DRILL_SATURN = 225;
    public static final int ID_ENGINE_DIESEL = 226;
    public static final int ID_ENGINE_AVIATION = 227;
    public static final int ID_ENGINE_ELECTRIC = 228;
    public static final int ID_ENGINE_TURBO = 229;
    /**
     * Installed modifiers are saved by name under this key + config index, comma-separated
     * ({@code "hbm:silencer,hbm:scope"}). 1.7 saved the numeric ids under {@link #KEY_MOD_LIST}; numeric ids are never
     * persisted (AGENTS.md section 8; an addon's number would also collide with HBM's).
     */
    public static final String KEY_MOD_NAMES = "KEY_MOD_NAMES_";
    /**
     * Runtime handles: 1.7's numeric mod id for HBM's modifiers (renderers ask {@code hasUpgrade(stack, cfg, ID_SCOPE)}),
     * a handle from {@link #FIRST_ADDON_HANDLE} up for a modifier registered by name only. Never saved.
     */
    public static final Map<Integer, IWeaponMod> idToMod = new HashMap<>();
    /** Every modifier by its saved name ({@code "hbm:silencer"}, an addon's {@code "<namespace>:<path>"}). */
    private static final Map<String, IWeaponMod> byName = new HashMap<>();
    private static final Map<IWeaponMod, String> nameOf = new HashMap<>();
    /** Handles below this are 1.7's ids; addon modifiers registered by name get handles from here up. */
    public static final int FIRST_ADDON_HANDLE = 1_000_000;
    private static int nextHandle = FIRST_ADDON_HANDLE;
    public static final List<WeaponModDefinition> definitions = new ArrayList<>();
    public static final Map<IWeaponMod, Supplier<ItemStack>> modToStack = new HashMap<>();
    private static boolean changedMagState;
    private static Object previousMagType;
    private static int previousMagCount;

    private XWeaponModManager() {}

    /** The saved name of each of HBM's modifiers, by its 1.7 numeric id (the id stays the runtime handle). */
    private static final Map<Integer, String> NAMES = new HashMap<>();

    static {
        NAMES.put(0, "test_firerate");
        NAMES.put(1, "test_damage");
        NAMES.put(2, "test_multi");
        NAMES.put(3, "test_override_2_5");
        NAMES.put(4, "test_override_5");
        NAMES.put(5, "test_override_7_5");
        NAMES.put(6, "test_override_10");
        NAMES.put(7, "test_override_12_5");
        NAMES.put(8, "test_override_15");
        NAMES.put(9, "test_override_20");
        NAMES.put(100, "iron_damage");
        NAMES.put(101, "iron_dura");
        NAMES.put(102, "steel_damage");
        NAMES.put(103, "steel_dura");
        NAMES.put(104, "dura_damage");
        NAMES.put(105, "dura_dura");
        NAMES.put(106, "desh_damage");
        NAMES.put(107, "desh_dura");
        NAMES.put(108, "wsteel_damage");
        NAMES.put(109, "wsteel_dura");
        NAMES.put(110, "ferro_damage");
        NAMES.put(111, "ferro_dura");
        NAMES.put(112, "tcalloy_damage");
        NAMES.put(113, "tcalloy_dura");
        NAMES.put(114, "bigmt_damage");
        NAMES.put(115, "bigmt_dura");
        NAMES.put(116, "bronze_damage");
        NAMES.put(117, "bronze_dura");
        NAMES.put(200, "speedloader");
        NAMES.put(201, "silencer");
        NAMES.put(202, "scope");
        NAMES.put(203, "sawed_off");
        NAMES.put(204, "sawed_off_panzerschreck");
        NAMES.put(205, "sawed_off_g3");
        NAMES.put(206, "greasegun");
        NAMES.put(207, "slowdown");
        NAMES.put(208, "speedup_minigun");
        NAMES.put(209, "speedup_shredder");
        NAMES.put(210, "choke");
        NAMES.put(211, "furniture_green");
        NAMES.put(212, "furniture_black");
        NAMES.put(213, "bayonet_mas36");
        NAMES.put(214, "stack_mag");
        NAMES.put(215, "skin_saturnite");
        NAMES.put(216, "las_shotgun");
        NAMES.put(217, "las_capacitor");
        NAMES.put(218, "las_auto");
        NAMES.put(219, "bayonet_carbine");
        NAMES.put(220, "nickel");
        NAMES.put(221, "doubloons");
        NAMES.put(222, "drill_hss");
        NAMES.put(223, "drill_weaponsteel");
        NAMES.put(224, "drill_tcalloy");
        NAMES.put(225, "drill_saturnite");
        NAMES.put(226, "engine_diesel");
        NAMES.put(227, "engine_aviation");
        NAMES.put(228, "engine_electric");
        NAMES.put(229, "engine_turbo");
        NAMES.put(230, "magnet");
        NAMES.put(231, "sifter");
        NAMES.put(232, "canisters");
        NAMES.put(300, "caliber_p9_henry");
        NAMES.put(301, "caliber_p9_star_f");
        NAMES.put(302, "caliber_p9_star_f_akimbo");
        NAMES.put(310, "caliber_p45_henry");
        NAMES.put(311, "caliber_p45_greasegun");
        NAMES.put(312, "caliber_p45_uzi");
        NAMES.put(313, "caliber_p45_uzi_akimbo");
        NAMES.put(314, "caliber_p45_lag");
        NAMES.put(320, "caliber_p22_henry");
        NAMES.put(321, "caliber_p22_uzi");
        NAMES.put(322, "caliber_p22_uzi_akimbo");
        NAMES.put(330, "caliber_m357_henry");
        NAMES.put(331, "caliber_m357_lag");
        NAMES.put(340, "caliber_m44_lag");
        NAMES.put(350, "caliber_r556_henry");
        NAMES.put(351, "caliber_r556_carbine");
        NAMES.put(352, "caliber_r556_minigun");
        NAMES.put(360, "caliber_r762_henry");
        NAMES.put(361, "caliber_r762_g3");
        NAMES.put(370, "caliber_bmg50_henry");
        NAMES.put(371, "caliber_bmg50_minigun");
    }

    /**
     * Registers a modifier under {@code name} (an addon: {@code "<namespace>:<path>"}) and gives it a runtime handle
     * if it has none. Bind it to its mod item and guns with a {@link WeaponModDefinition}. Call at mod construction.
     */
    public static synchronized <M extends IWeaponMod> M register(String name, M mod) {
        if (byName.containsKey(name)) throw new IllegalStateException("Weapon modifier '" + name + "' is registered twice");
        byName.put(name, mod);
        nameOf.put(mod, name);
        if (idOf(mod) < 0) idToMod.put(nextHandle++, mod);
        return mod;
    }

    /** The modifier saved as {@code name}, or null. */
    public static IWeaponMod byName(String name) {
        return byName.get(name);
    }

    /** The saved name of {@code mod}, or null if it was never registered. */
    public static String nameOf(IWeaponMod mod) {
        return nameOf.get(mod);
    }

    /** The runtime handle of the modifier saved as {@code name}, or -1. */
    public static int handle(String name) {
        IWeaponMod mod = byName.get(name);
        return mod == null ? -1 : idOf(mod);
    }
    /** Registers the source's complete modifier table after every gun and ammo supplier is declared. */
    public static synchronized void init() {
        if (nameOf(idToMod.get(0)) != null) return;

        def(ModWeaponModItems.weapon_mod_test, EnumModTest.FIRERATE).addDefault(new WeaponModTestFirerate(0, "FIRERATE"));
        def(ModWeaponModItems.weapon_mod_test, EnumModTest.DAMAGE).addDefault(new WeaponModTestDamage(1, "DAMAGE"));
        def(ModWeaponModItems.weapon_mod_test, EnumModTest.MULTI).addDefault(new WeaponModTestMulti(2, "MULTI"));
        def(ModWeaponModItems.weapon_mod_test, EnumModTest.OVERRIDE_2_5).addDefault(new WeaponModOverride(3, 2.5F, "OVERRIDE"));
        def(ModWeaponModItems.weapon_mod_test, EnumModTest.OVERRIDE_5).addDefault(new WeaponModOverride(4, 5F, "OVERRIDE"));
        def(ModWeaponModItems.weapon_mod_test, EnumModTest.OVERRIDE_7_5).addDefault(new WeaponModOverride(5, 7.5F, "OVERRIDE"));
        def(ModWeaponModItems.weapon_mod_test, EnumModTest.OVERRIDE_10).addDefault(new WeaponModOverride(6, 10F, "OVERRIDE"));
        // The source's 12_5F literal is 125F, not 12.5F; preserve the old save/gameplay behavior.
        def(ModWeaponModItems.weapon_mod_test, EnumModTest.OVERRIDE_12_5).addDefault(new WeaponModOverride(7, 125F, "OVERRIDE"));
        def(ModWeaponModItems.weapon_mod_test, EnumModTest.OVERRIDE_15).addDefault(new WeaponModOverride(8, 15F, "OVERRIDE"));
        def(ModWeaponModItems.weapon_mod_test, EnumModTest.OVERRIDE_20).addDefault(new WeaponModOverride(9, 20F, "OVERRIDE"));

        def(ModWeaponModItems.weapon_mod_generic, EnumModGeneric.IRON_DAMAGE).addMod(ModGunItems.gun_pepperbox, new WeaponModGenericDamage(100));
        def(ModWeaponModItems.weapon_mod_generic, EnumModGeneric.IRON_DURA).addMod(ModGunItems.gun_pepperbox, new WeaponModGenericDurability(101));
        Supplier<Item>[] steel = items(ModGunItems.gun_light_revolver, ModGunItems.gun_light_revolver_atlas, ModGunItems.gun_henry,
                ModGunItems.gun_henry_lincoln, ModGunItems.gun_greasegun, ModGunItems.gun_maresleg, ModGunItems.gun_maresleg_akimbo,
                ModGunItems.gun_flaregun);
        Supplier<Item>[] dura = items(ModGunItems.gun_am180, ModGunItems.gun_liberator, ModGunItems.gun_congolake, item("gun_flamer"),
                item("gun_flamer_topaz"));
        Supplier<Item>[] desh = items(ModGunItems.gun_heavy_revolver, ModGunItems.gun_carbine, ModGunItems.gun_uzi,
                ModGunItems.gun_uzi_akimbo, ModGunItems.gun_spas12, item("gun_panzerschreck"));
        Supplier<Item>[] wsteel = items(ModGunItems.gun_star_f, ModGunItems.gun_star_f_akimbo, ModGunItems.gun_g3, ModGunItems.gun_g3_zebra,
                ModGunItems.gun_mk108, ModGunItems.gun_chemthrower);
        Supplier<Item>[] ferro = items(ModGunItems.gun_amat, ModGunItems.gun_m2, ModGunItems.gun_autoshotgun, ModGunItems.gun_autoshotgun_shredder,
                item("gun_quadro"));
        Supplier<Item>[] tcalloy = items(ModGunItems.gun_lag, ModGunItems.gun_minigun, item("gun_missile_launcher"),
                ModGunItems.gun_tesla_cannon);
        Supplier<Item>[] bigmt = items(ModGunItems.gun_laser_pistol, ModGunItems.gun_laser_pistol_pew_pew, ModGunItems.gun_stg77,
                item("gun_fatman"), ModGunItems.gun_tau);
        Supplier<Item>[] bronze = items(ModGunItems.gun_lasrifle);
        def(ModWeaponModItems.weapon_mod_generic, EnumModGeneric.STEEL_DAMAGE).addMod(steel, new WeaponModGenericDamage(102));
        def(ModWeaponModItems.weapon_mod_generic, EnumModGeneric.STEEL_DURA).addMod(steel, new WeaponModGenericDurability(103));
        def(ModWeaponModItems.weapon_mod_generic, EnumModGeneric.DURA_DAMAGE).addMod(dura, new WeaponModGenericDamage(104));
        def(ModWeaponModItems.weapon_mod_generic, EnumModGeneric.DURA_DURA).addMod(dura, new WeaponModGenericDurability(105));
        def(ModWeaponModItems.weapon_mod_generic, EnumModGeneric.DESH_DAMAGE).addMod(desh, new WeaponModGenericDamage(106));
        def(ModWeaponModItems.weapon_mod_generic, EnumModGeneric.DESH_DURA).addMod(desh, new WeaponModGenericDurability(107));
        def(ModWeaponModItems.weapon_mod_generic, EnumModGeneric.WSTEEL_DAMAGE).addMod(wsteel, new WeaponModGenericDamage(108));
        def(ModWeaponModItems.weapon_mod_generic, EnumModGeneric.WSTEEL_DURA).addMod(wsteel, new WeaponModGenericDurability(109));
        def(ModWeaponModItems.weapon_mod_generic, EnumModGeneric.FERRO_DAMAGE).addMod(ferro, new WeaponModGenericDamage(110));
        def(ModWeaponModItems.weapon_mod_generic, EnumModGeneric.FERRO_DURA).addMod(ferro, new WeaponModGenericDurability(111));
        def(ModWeaponModItems.weapon_mod_generic, EnumModGeneric.TCALLOY_DAMAGE).addMod(tcalloy, new WeaponModGenericDamage(112));
        def(ModWeaponModItems.weapon_mod_generic, EnumModGeneric.TCALLOY_DURA).addMod(tcalloy, new WeaponModGenericDurability(113));
        def(ModWeaponModItems.weapon_mod_generic, EnumModGeneric.BIGMT_DAMAGE).addMod(bigmt, new WeaponModGenericDamage(114));
        def(ModWeaponModItems.weapon_mod_generic, EnumModGeneric.BIGMT_DURA).addMod(bigmt, new WeaponModGenericDurability(115));
        def(ModWeaponModItems.weapon_mod_generic, EnumModGeneric.BRONZE_DAMAGE).addMod(bronze, new WeaponModGenericDamage(116));
        def(ModWeaponModItems.weapon_mod_generic, EnumModGeneric.BRONZE_DURA).addMod(bronze, new WeaponModGenericDurability(117));

        def(ModWeaponModItems.weapon_mod_special, EnumModSpecial.SPEEDLOADER).addMod(ModGunItems.gun_liberator, new WeaponModLiberatorSpeedloader(200));
        def(ModWeaponModItems.weapon_mod_special, EnumModSpecial.SILENCER).addMod(items(ModGunItems.gun_am180, ModGunItems.gun_uzi, ModGunItems.gun_uzi_akimbo,
                ModGunItems.gun_star_f, ModGunItems.gun_star_f_akimbo, ModGunItems.gun_g3, ModGunItems.gun_amat), new WeaponModSilencer(ID_SILENCER));
        def(ModWeaponModItems.weapon_mod_special, EnumModSpecial.SCOPE).addMod(items(ModGunItems.gun_heavy_revolver, ModGunItems.gun_carbine,
                ModGunItems.gun_g3, ModGunItems.gun_mas36, ModGunItems.gun_charge_thrower), new WeaponModScope(ID_SCOPE));
        def(ModWeaponModItems.weapon_mod_special, EnumModSpecial.SAW).addMod(items(ModGunItems.gun_maresleg, ModGunItems.gun_double_barrel), new WeaponModSawedOff(ID_SAWED_OFF))
                .addMod(item("gun_panzerschreck"), new WeaponModPanzerschreckSawedOff(ID_NO_SHIELD))
                .addMod(items(ModGunItems.gun_g3, ModGunItems.gun_g3_zebra), new WeapnModG3SawedOff(ID_NO_STOCK));
        def(ModWeaponModItems.weapon_mod_special, EnumModSpecial.GREASEGUN).addMod(ModGunItems.gun_greasegun, new WeaponModGreasegun(ID_GREASEGUN_CLEAN));
        def(ModWeaponModItems.weapon_mod_special, EnumModSpecial.SLOWDOWN).addMod(items(ModGunItems.gun_minigun, ModGunItems.gun_minigun_dual), new WeaponModSlowdown(207));
        def(ModWeaponModItems.weapon_mod_special, EnumModSpecial.SPEEDUP).addMod(items(ModGunItems.gun_minigun, ModGunItems.gun_minigun_dual), new WeaponModMinigunSpeedup(ID_MINIGUN_SPEED))
                .addMod(items(ModGunItems.gun_autoshotgun, ModGunItems.gun_autoshotgun_shredder, ModGunItems.gun_mk108), new WeaponModShredderSpeedup(209));
        def(ModWeaponModItems.weapon_mod_special, EnumModSpecial.CHOKE).addMod(items(ModGunItems.gun_pepperbox, ModGunItems.gun_maresleg,
                ModGunItems.gun_double_barrel, ModGunItems.gun_liberator, ModGunItems.gun_spas12, ModGunItems.gun_autoshotgun_sexy,
                ModGunItems.gun_autoshotgun_heretic), new WeaponModChoke(210));
        def(ModWeaponModItems.weapon_mod_special, EnumModSpecial.FURNITURE_GREEN).addMod(ModGunItems.gun_g3, new WeaponModPolymerFurniture(ID_FURNITURE_GREEN));
        def(ModWeaponModItems.weapon_mod_special, EnumModSpecial.FURNITURE_BLACK).addMod(ModGunItems.gun_g3, new WeaponModPolymerFurniture(ID_FURNITURE_BLACK));
        def(ModWeaponModItems.weapon_mod_special, EnumModSpecial.BAYONET).addMod(ModGunItems.gun_mas36, new WeaponModMASBayonet(ID_MAS_BAYONET))
                .addMod(ModGunItems.gun_carbine, new WeaponModCarbineBayonet(ID_CARBINE_BAYONET));
        def(ModWeaponModItems.weapon_mod_special, EnumModSpecial.STACK_MAG).addMod(items(ModGunItems.gun_greasegun, ModGunItems.gun_uzi,
                ModGunItems.gun_uzi_akimbo, ModGunItems.gun_aberrator, ModGunItems.gun_aberrator_eott), new WeaponModStackMag(214));
        def(ModWeaponModItems.weapon_mod_special, EnumModSpecial.SKIN_SATURNITE).addMod(items(ModGunItems.gun_uzi, ModGunItems.gun_uzi_akimbo), new WeaponModUziSaturnite(ID_UZI_SATURN));
        def(ModWeaponModItems.weapon_mod_special, EnumModSpecial.LAS_SHOTGUN).addMod(ModGunItems.gun_lasrifle, new WeaponModLasShotgun(ID_LAS_SHOTGUN));
        def(ModWeaponModItems.weapon_mod_special, EnumModSpecial.LAS_CAPACITOR).addMod(ModGunItems.gun_lasrifle, new WeaponModLasCapacitor(ID_LAS_CAPACITOR));
        def(ModWeaponModItems.weapon_mod_special, EnumModSpecial.LAS_AUTO).addMod(ModGunItems.gun_lasrifle, new WeaponModLasAuto(ID_LAS_AUTO));
        def(ModWeaponModItems.weapon_mod_special, EnumModSpecial.NICKEL).addMod(ModGunItems.gun_n_i_4_n_i, new WeaponModNickel(ID_NI4NI_NICKEL, "COIN1"));
        def(ModWeaponModItems.weapon_mod_special, EnumModSpecial.DOUBLOONS).addMod(ModGunItems.gun_n_i_4_n_i, new WeaponModNickel(ID_NI4NI_DOUBLOONS, "COIN2"));
        def(ModWeaponModItems.weapon_mod_special, EnumModSpecial.DRILL_HSS).addMod(ModGunItems.gun_drill, new WeaponModDrill(ID_DRILL_HSS).damage(1.25F).dt(3F).pierce(0.15F).harvest(3));
        def(ModWeaponModItems.weapon_mod_special, EnumModSpecial.DRILL_WEAPONSTEEL).addMod(ModGunItems.gun_drill, new WeaponModDrill(ID_DRILL_WSTEEL).damage(1.5F).dt(5F).pierce(0.2F).aoe(2).harvest(3));
        def(ModWeaponModItems.weapon_mod_special, EnumModSpecial.DRILL_TCALLOY).addMod(ModGunItems.gun_drill, new WeaponModDrill(ID_DRILL_TCALLOY).damage(2F).dt(7.5F).pierce(0.2F).reach(2).aoe(3).harvest(4));
        def(ModWeaponModItems.weapon_mod_special, EnumModSpecial.DRILL_SATURNITE).addMod(ModGunItems.gun_drill, new WeaponModDrill(ID_DRILL_SATURN).damage(3F).dt(10F).pierce(0.25F).reach(2).aoe(3).harvest(5));
        def(ModWeaponModItems.weapon_mod_special, EnumModSpecial.ENGINE_DIESEL).addMod(ModGunItems.gun_drill, new WeaponModEngine(ID_ENGINE_DIESEL).mag(WeaponModEngine.ENGINE_DIESEL).delay(15));
        def(ModWeaponModItems.weapon_mod_special, EnumModSpecial.ENGINE_AVIATION).addMod(ModGunItems.gun_drill, new WeaponModEngine(ID_ENGINE_AVIATION).mag(WeaponModEngine.ENGINE_AVIATION).delay(10));
        def(ModWeaponModItems.weapon_mod_special, EnumModSpecial.ENGINE_ELECTRIC).addMod(ModGunItems.gun_drill, new WeaponModEngine(ID_ENGINE_ELECTRIC).mag(WeaponModEngine.ENGINE_ELECTRIC).delay(15));
        def(ModWeaponModItems.weapon_mod_special, EnumModSpecial.ENGINE_TURBO).addMod(ModGunItems.gun_drill, new WeaponModEngine(ID_ENGINE_TURBO).mag(WeaponModEngine.ENGINE_TURBO).delay(5));
        def(ModWeaponModItems.weapon_mod_special, EnumModSpecial.MAGNET).addMod(ModGunItems.gun_drill, new WeaponModDrillFortune(230, "MAGNET", 2));
        def(ModWeaponModItems.weapon_mod_special, EnumModSpecial.SIFTER).addMod(ModGunItems.gun_drill, new WeaponModDrillFortune(231, "SIFTER", 1));
        def(ModWeaponModItems.weapon_mod_special, EnumModSpecial.CANISTERS).addMod(ModGunItems.gun_drill, new WeaponModCanisters(232));

        var p9 = new BulletConfig[]{XFactory9mm.p9_sp, XFactory9mm.p9_fmj, XFactory9mm.p9_jhp, XFactory9mm.p9_ap};
        var p45 = new BulletConfig[]{XFactory45.p45_sp, XFactory45.p45_fmj, XFactory45.p45_jhp, XFactory45.p45_ap, XFactory45.p45_du};
        var p22 = new BulletConfig[]{XFactory22lr.p22_sp, XFactory22lr.p22_fmj, XFactory22lr.p22_jhp, XFactory22lr.p22_ap};
        var m357 = new BulletConfig[]{XFactory357.m357_sp, XFactory357.m357_fmj, XFactory357.m357_jhp, XFactory357.m357_ap, XFactory357.m357_express};
        var m44 = new BulletConfig[]{XFactory44.m44_sp, XFactory44.m44_fmj, XFactory44.m44_jhp, XFactory44.m44_ap, XFactory44.m44_express};
        var r556 = new BulletConfig[]{XFactory556mm.r556_sp, XFactory556mm.r556_fmj, XFactory556mm.r556_jhp, XFactory556mm.r556_ap};
        var r762 = new BulletConfig[]{XFactory762mm.r762_sp, XFactory762mm.r762_fmj, XFactory762mm.r762_jhp, XFactory762mm.r762_ap, XFactory762mm.r762_du, XFactory762mm.r762_he};
        var bmg50 = new BulletConfig[]{XFactory50.bmg50_sp, XFactory50.bmg50_fmj, XFactory50.bmg50_jhp, XFactory50.bmg50_ap, XFactory50.bmg50_du, XFactory50.bmg50_he};
        def(ModWeaponModItems.weapon_mod_caliber, EnumModCaliber.P9).addMod(ModGunItems.gun_henry, new WeaponModCaliber(300, 28, 10F, p9)).addMod(ModGunItems.gun_star_f, new WeaponModCaliber(301, 12, 15F, p9)).addMod(ModGunItems.gun_star_f_akimbo, new WeaponModCaliber(302, 12, 15F, p9));
        def(ModWeaponModItems.weapon_mod_caliber, EnumModCaliber.P45).addMod(ModGunItems.gun_henry, new WeaponModCaliber(310, 28, 10F, p45)).addMod(ModGunItems.gun_greasegun, new WeaponModCaliber(311, 24, 3F, p45)).addMod(ModGunItems.gun_uzi, new WeaponModCaliber(312, 24, 3F, p45)).addMod(ModGunItems.gun_uzi_akimbo, new WeaponModCaliber(313, 24, 3F, p45)).addMod(ModGunItems.gun_lag, new WeaponModCaliber(314, 15, 25F, p45));
        def(ModWeaponModItems.weapon_mod_caliber, EnumModCaliber.P22).addMod(ModGunItems.gun_henry, new WeaponModCaliber(320, 28, 10F, p22)).addMod(ModGunItems.gun_uzi, new WeaponModCaliber(321, 40, 3F, p22)).addMod(ModGunItems.gun_uzi_akimbo, new WeaponModCaliber(322, 40, 3F, p22));
        def(ModWeaponModItems.weapon_mod_caliber, EnumModCaliber.M357).addMod(ModGunItems.gun_henry, new WeaponModCaliber(330, 20, 10F, m357)).addMod(ModGunItems.gun_lag, new WeaponModCaliber(331, 15, 25F, m357));
        def(ModWeaponModItems.weapon_mod_caliber, EnumModCaliber.M44).addMod(ModGunItems.gun_lag, new WeaponModCaliber(340, 13, 25F, m44));
        def(ModWeaponModItems.weapon_mod_caliber, EnumModCaliber.R556).addMod(ModGunItems.gun_henry, new WeaponModCaliber(350, 10, 10F, r556)).addMod(ModGunItems.gun_carbine, new WeaponModCaliber(351, 20, 15F, r556)).addMod(items(ModGunItems.gun_minigun, ModGunItems.gun_minigun_dual), new WeaponModCaliber(352, 0, 6F, r556));
        def(ModWeaponModItems.weapon_mod_caliber, EnumModCaliber.R762).addMod(ModGunItems.gun_henry, new WeaponModCaliber(360, 8, 10F, r762)).addMod(ModGunItems.gun_g3, new WeaponModCaliber(361, 24, 5F, r762));
        def(ModWeaponModItems.weapon_mod_caliber, EnumModCaliber.BMG50).addMod(ModGunItems.gun_henry, new WeaponModCaliber(370, 5, 10F, bmg50)).addMod(items(ModGunItems.gun_minigun, ModGunItems.gun_minigun_dual), new WeaponModCaliber(371, 0, 6F, bmg50));
        // HBM's own modifiers take their saved names from the 1.7 id table
        for (Map.Entry<Integer, String> entry : NAMES.entrySet()) {
            IWeaponMod mod = idToMod.get(entry.getKey());
            if (mod == null) throw new IllegalStateException("No weapon modifier has the 1.7 id " + entry.getKey());
            register(Constants.MOD_ID + ":" + entry.getValue(), mod);
        }
    }

    private static <E extends Enum<E>> WeaponModDefinition def(ItemFamily<E, ItemEnumMulti> family, E variant) {
        return new WeaponModDefinition(() -> family.stack(variant));
    }

    private static Supplier<Item> item(String id) {
        return () -> BuiltInRegistries.ITEM.getValue(Ids.of(Constants.MOD_ID, id));
    }

    @SafeVarargs
    private static Supplier<Item>[] items(RegistryObject<? extends Item>... handles) {
        @SuppressWarnings("unchecked") Supplier<Item>[] result = new Supplier[handles.length];
        for (int i = 0; i < handles.length; i++) result[i] = handles[i]::get;
        return result;
    }

    @SafeVarargs
    private static Supplier<Item>[] items(Supplier<? extends Item>... handles) {
        @SuppressWarnings("unchecked") Supplier<Item>[] result = new Supplier[handles.length];
        for (int i = 0; i < handles.length; i++) result[i] = handles[i]::get;
        return result;
    }

    public static ItemStack[] getUpgradeItems(ItemStack stack, int cfg) {
        int[] ids = getIds(stack, cfg);
        List<ItemStack> result = new ArrayList<>();
        for (int id : ids) {
            Supplier<ItemStack> supplier = modToStack.get(idToMod.get(id));
            if (supplier != null) result.add(supplier.get().copy());
        }
        return result.toArray(new ItemStack[0]);
    }

    /**
     * The source {@code ItemGunBaseNT.recognizedMods}: one stack per modifier definition with a binding
     * for this gun's item. Resolved lazily because gun suppliers cannot be evaluated at definition time.
     */
    public static List<ItemStack> recognizedMods(ItemStack gun) {
        List<ItemStack> result = new ArrayList<>();
        if (gun == null || gun.isEmpty()) return result;
        for (WeaponModDefinition def : definitions) {
            if (def.bindsGun(gun.getItem())) result.add(def.modStack.get().copy());
        }
        return result;
    }

    public static boolean hasUpgrade(ItemStack stack, int cfg, int id) {
        for (int installed : getIds(stack, cfg)) if (installed == id) return true;
        return false;
    }

    public static void changedMagState() { changedMagState = true; }

    public static void install(ItemStack stack, int cfg, ItemStack... mods) {
        install(null, stack, cfg, mods);
    }

    /** Server-aware install path required by the dynamic enchantment registry used by drill fortune mods. */
    public static void install(ServerLevel level, ItemStack stack, int cfg, ItemStack... mods) {
        saveMagState(stack, cfg);
        uninstall(level, stack, cfg);
        List<IWeaponMod> selected = new ArrayList<>();
        for (ItemStack mod : mods) {
            if (mod == null || mod.isEmpty()) continue;
            WeaponModDefinition def = definitionFor(mod);
            if (def == null) continue;
            IWeaponMod implementation = def.forGun(stack);
            if (implementation != null) selected.add(implementation);
        }
        selected.sort(modSorter);
        CompoundTag data = ItemGunBaseNT.getGunData(stack);
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < selected.size(); i++) {
            IWeaponMod selectedMod = selected.get(i);
            String name = nameOf(selectedMod);
            if (name != null) {
                if (names.length() > 0) names.append(',');
                names.append(name);
            }
            Supplier<ItemStack> supplier = modToStack.get(selectedMod);
            if (supplier != null) {
                if (level == null) selectedMod.onInstall(stack, supplier.get(), cfg);
                else selectedMod.onInstall(level, stack, supplier.get(), cfg);
            }
        }
        data.remove(KEY_MOD_LIST + cfg);
        data.putString(KEY_MOD_NAMES + cfg, names.toString());
        ItemGunBaseNT.setGunData(stack, data);
        restoreMagState(stack, cfg);
    }

    public static void uninstall(ItemStack stack, int cfg) {
        uninstall(null, stack, cfg);
    }

    public static void uninstall(ServerLevel level, ItemStack stack, int cfg) {
        for (ItemStack mod : getUpgradeItems(stack, cfg)) onUninstallStack(level, stack, mod, cfg);
        CompoundTag data = ItemGunBaseNT.getGunData(stack);
        data.remove(KEY_MOD_LIST + cfg);
        data.remove(KEY_MOD_NAMES + cfg);
        ItemGunBaseNT.setGunData(stack, data);
    }

    public static void onInstallStack(ItemStack gun, ItemStack mod, int cfg) {
        onInstallStack(null, gun, mod, cfg);
    }

    public static void onInstallStack(ServerLevel level, ItemStack gun, ItemStack mod, int cfg) {
        IWeaponMod implementation = modFromStack(gun, mod, cfg);
        if (implementation != null) {
            if (level == null) implementation.onInstall(gun, mod, cfg);
            else implementation.onInstall(level, gun, mod, cfg);
        }
    }

    public static void onUninstallStack(ItemStack gun, ItemStack mod, int cfg) {
        onUninstallStack(null, gun, mod, cfg);
    }

    public static void onUninstallStack(ServerLevel level, ItemStack gun, ItemStack mod, int cfg) {
        IWeaponMod implementation = modFromStack(gun, mod, cfg);
        if (implementation != null) {
            if (level == null) implementation.onUninstall(gun, mod, cfg);
            else implementation.onUninstall(level, gun, mod, cfg);
        }
    }

    public static IWeaponMod modFromStack(ItemStack gun, ItemStack mod, int cfg) {
        WeaponModDefinition def = definitionFor(mod);
        return def == null ? null : def.forGun(gun);
    }

    public static boolean isApplicable(ItemStack gun, ItemStack mod, int cfg, boolean checkMutex) {
        IWeaponMod candidate = modFromStack(gun, mod, cfg);
        if (candidate == null) return false;
        if (!checkMutex) return true;
        for (int id : getIds(gun, cfg)) {
            IWeaponMod installed = idToMod.get(id);
            if (installed == null) continue;
            for (String left : candidate.getSlots()) for (String right : installed.getSlots())
                if (left.equals(right)) return false;
        }
        return true;
    }

    public static final Comparator<IWeaponMod> modSorter =
            (left, right) -> Integer.compare(right.getModPriority(), left.getModPriority());

    public static <T> T eval(T base, ItemStack stack, String key, Object parent, int cfg) {
        if (stack == null || stack.isEmpty()) return base;
        T value = base;
        for (int id : getIds(stack, cfg)) {
            IWeaponMod mod = idToMod.get(id);
            if (mod != null) value = mod.eval(value, stack, key, parent);
        }
        return value;
    }

    /** The runtime handles of the modifiers installed in config {@code cfg}, in their saved (priority) order. */
    private static int[] getIds(ItemStack stack, int cfg) {
        CompoundTag data = ItemGunBaseNT.getGunData(stack);
        String saved = Tags.getStringOr(data, KEY_MOD_NAMES + cfg, "");
        if (saved.isEmpty()) {
            // a gun carrying 1.7's numeric mod ids is not read, and its mods are dropped (one WARN)
            if (Tags.contains(data, KEY_MOD_LIST + cfg)) SavedIds.warnNumeric("weapon modifier id", KEY_MOD_LIST + cfg);
            return new int[0];
        }
        String[] names = saved.split(",");
        int[] ids = new int[names.length];
        int count = 0;
        for (String name : names) {
            int handle = handle(name);
            if (handle >= 0) ids[count++] = handle;
        }
        return count == ids.length ? ids : Arrays.copyOf(ids, count);
    }

    private static int idOf(IWeaponMod mod) {
        for (Map.Entry<Integer, IWeaponMod> entry : idToMod.entrySet()) if (entry.getValue() == mod) return entry.getKey();
        return -1;
    }

    private static WeaponModDefinition definitionFor(ItemStack stack) {
        for (WeaponModDefinition def : definitions) if (def.matches(stack)) return def;
        return null;
    }

    private static void saveMagState(ItemStack stack, int cfg) {
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        IMagazine<?> mag = gun.getConfig(stack, cfg).getReceivers(stack)[0].getMagazine(stack);
        if (mag == null) return;
        previousMagType = mag.getType(stack, null);
        previousMagCount = mag.getAmount(stack, null);
    }

    private static void restoreMagState(ItemStack stack, int cfg) {
        if (!changedMagState) return;
        changedMagState = false;
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        IMagazine<?> mag = gun.getConfig(stack, cfg).getReceivers(stack)[0].getMagazine(stack);
        if (mag == null) return;
        if (mag.getType(stack, null) == previousMagType)
            mag.setAmount(stack, Math.max(0, Math.min(previousMagCount, mag.getCapacity(stack))));
        else mag.setAmount(stack, 0);
    }

    public static final class WeaponModDefinition {
        private final Supplier<ItemStack> modStack;
        private final List<Binding> bindings = new ArrayList<>();

        public WeaponModDefinition(Supplier<ItemStack> stack) {
            this.modStack = stack;
            definitions.add(this);
        }
        public WeaponModDefinition(ItemStack stack) { this(() -> stack.copyWithCount(1)); }
        public WeaponModDefinition addMod(Item gun, IWeaponMod mod) {
            return addMod(() -> gun, mod);
        }
        public WeaponModDefinition addMod(Supplier<? extends Item> gun, IWeaponMod mod) {
            bindings.add(new Binding(gun, null, mod));
            modToStack.put(mod, modStack);
            return this;
        }
        public WeaponModDefinition addMod(Item[] guns, IWeaponMod mod) { for (Item gun : guns) addMod(gun, mod); return this; }
        public WeaponModDefinition addMod(Supplier<Item>[] guns, IWeaponMod mod) { for (Supplier<Item> gun : guns) addMod(gun, mod); return this; }
        public WeaponModDefinition addMod(ItemStack gun, IWeaponMod mod) {
            bindings.add(new Binding(() -> gun.getItem(), gun.copyWithCount(1), mod));
            modToStack.put(mod, modStack);
            return this;
        }
        public WeaponModDefinition addMod(RecipesCommon.ComparableStack gun, IWeaponMod mod) {
            return gun == null ? addDefault(mod) : addMod(gun.getDisplayStack(), mod);
        }
        public WeaponModDefinition addDefault(IWeaponMod mod) { bindings.add(new Binding(null, null, mod)); modToStack.put(mod, modStack); return this; }
        private boolean bindsGun(Item item) {
            for (Binding binding : bindings) if (binding.gun != null && binding.gun.get() == item) return true;
            return false;
        }
        private boolean matches(ItemStack stack) { return !stack.isEmpty() && ItemStacks.sameItemAndData(modStack.get(), stack); }
        private IWeaponMod forGun(ItemStack gun) {
            IWeaponMod fallback = null;
            for (Binding binding : bindings) {
                if (binding.gun == null) { fallback = binding.mod; continue; }
                if (binding.gun.get() == gun.getItem() && (binding.exactGun == null || ItemStacks.sameItemAndData(binding.exactGun, gun))) return binding.mod;
            }
            return fallback;
        }
        private record Binding(Supplier<? extends Item> gun, ItemStack exactGun, IWeaponMod mod) {}
    }
}
