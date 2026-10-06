package com.hbm.client.render.weapon;

import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The weapon table's per-gun framing: each 1.7 renderer's {@code ItemRenderWeaponBase.setupModTable}.
 *
 * <p>1.7 {@code GUIWeaponTable} translated to the preview centre (gui + 88, gui + 57), turned the gun by the
 * mouse ({@code glRotated(yaw, 0, 1, 0)} then {@code glRotated(pitch, 1, 0, 0)}), and then let the gun's own
 * renderer frame itself. Every one of the 53 renderers wrote that hook as the same three calls with its own
 * numbers: {@code glScaled(-s, -s, -s)}, {@code glRotated(yaw, 0, 1, 0)} (90, except the Stinger's -90) and an
 * optional {@code glTranslated(x, y, z)} in model units. The negative scale is not a slip: the 1.7 GUI frame is
 * y-down, so it is mirrored, and the reflection turns the model back the right way round.
 *
 * <p>Keyed by item, because 1.7 registered one renderer instance per item and several items share a class.
 * An item that is a Sedna gun but had no {@code ItemRenderWeaponBase} in 1.7 ({@code gun_fireext}, drawn by
 * {@code ItemRenderFireExt}) has no row, and 1.7 drew no preview for it. Data only, with no client types, so a
 * GameTest can check it. The rows were extracted from the 1.7 sources ({@code GunFactoryClient} for the item to
 * renderer map, each class's {@code setupModTable}) and compared row by row (History §19es-weapontable).
 */
public final class WeaponTableFrames {

    /** {@code glScaled(-scale, -scale, -scale)}, {@code glRotated(yaw, 0, 1, 0)}, {@code glTranslated(x, y, z)}. */
    public record Frame(double scale, double yaw, double x, double y, double z) {}

    private static final Map<String, Frame> BY_ITEM = build();

    private WeaponTableFrames() {}

    /** The 1.7 frame for this item id path ({@code "gun_uzi"}), or null when 1.7 drew no table preview. */
    public static @Nullable Frame forItem(String path) {
        return BY_ITEM.get(path);
    }

    public static Map<String, Frame> all() {
        return java.util.Collections.unmodifiableMap(BY_ITEM);
    }

    private static Map<String, Frame> build() {
        Map<String, Frame> m = new HashMap<>();
        put(m, "gun_debug", 5, 90, 0, 0, 0); // ItemRenderDebug
        put(m, "gun_pepperbox", 7.5, 90, 0, 0, 0); // ItemRenderPepperbox
        put(m, "gun_light_revolver", 5, 90, 0, 1.5, 0); // ItemRenderAtlas
        put(m, "gun_light_revolver_atlas", 5, 90, 0, 1.5, 0); // ItemRenderAtlas
        put(m, "gun_light_revolver_dani", 5, 90, 0, 1.5, 0); // ItemRenderDANI
        put(m, "gun_henry", 7.5, 90, 0, 0, 0); // ItemRenderHenry
        put(m, "gun_henry_lincoln", 7.5, 90, 0, 0, 0); // ItemRenderHenry
        put(m, "gun_greasegun", 7.5, 90, 0, 2, 0); // ItemRenderGreasegun
        put(m, "gun_maresleg", 8.75, 90, 0, 0, 0); // ItemRenderMaresleg
        put(m, "gun_maresleg_akimbo", 12.5, 90, 0, -0.5, 1); // ItemRenderMareslegAkimbo
        put(m, "gun_maresleg_broken", 8.75, 90, 0, 0, 0); // ItemRenderMaresleg
        put(m, "gun_flaregun", 5, 90, 0, 0, 0); // ItemRenderFlaregun
        put(m, "gun_heavy_revolver", 5, 90, 0, -0.5, 0); // ItemRenderHeavyRevolver
        put(m, "gun_heavy_revolver_lilmac", 5, 90, 0, -0.5, 0); // ItemRenderHeavyRevolver
        put(m, "gun_heavy_revolver_protege", 5, 90, 0, -0.5, 0); // ItemRenderHeavyRevolver
        put(m, "gun_carbine", 7.75, 90, 0, 0, -1.75); // ItemRenderCarbine
        put(m, "gun_am180", 5, 90, 0, 0, -2); // ItemRenderAm180
        put(m, "gun_liberator", 8.75, 90, 0, 0, 0); // ItemRenderLiberator
        put(m, "gun_congolake", 15, 90, 0, -1.25, 0); // ItemRenderCongoLake
        put(m, "gun_flamer", 7.5, 90, 0, 0, 1); // ItemRenderFlamer
        put(m, "gun_flamer_topaz", 7.5, 90, 0, 0, 1); // ItemRenderFlamer
        put(m, "gun_flamer_daybreaker", 7.5, 90, 0, 0, 1); // ItemRenderFlamer
        put(m, "gun_lag", 7.5, 90, 0, 0.5, -2); // ItemRenderLAG
        put(m, "gun_uzi", 6.25, 90, 0, 1, -4); // ItemRenderUzi
        put(m, "gun_uzi_akimbo", 6.25, 90, 0, 1, -4); // ItemRenderUziAkimbo
        put(m, "gun_spas12", 10, 90, 0, -0.5, -4.25); // ItemRenderSPAS12
        put(m, "gun_panzerschreck", 10, 90, 0, 0, 0); // ItemRenderPanzerschreck
        put(m, "gun_star_f", 6.25, 90, 0, -0.25, -5); // ItemRenderStarF
        put(m, "gun_star_f_akimbo", 6.25, 90, 0, -0.25, -5); // ItemRenderStarFAkimbo
        put(m, "gun_g3", 5, 90, 0, 0.5, -0.5); // ItemRenderG3
        put(m, "gun_g3_zebra", 5, 90, 0, 0.5, -0.5); // ItemRenderG3
        put(m, "gun_stinger", 7.5, -90, 0, -4, 0); // ItemRenderStinger
        put(m, "gun_mk108", 9.5, 90, 0, 0.5, -0.25); // ItemRenderMK108
        put(m, "gun_chemthrower", 10, 90, 0, -0.5, -0.5); // ItemRenderChemthrower
        put(m, "gun_amat", 5.75, 90, 0, -0.25, -1.5); // ItemRenderAmat
        put(m, "gun_amat_subtlety", 5.75, 90, 0, -0.25, -1.5); // ItemRenderAmat
        put(m, "gun_amat_penance", 5.75, 90, 0, -0.25, -1.5); // ItemRenderAmat
        put(m, "gun_m2", 15, 90, 0, -1.5, -0.5); // ItemRenderM2
        put(m, "gun_autoshotgun", 7.5, 90, 0, 0, 1.5); // ItemRenderShredder
        put(m, "gun_autoshotgun_shredder", 7.5, 90, 0, 0, 1.5); // ItemRenderShredder
        put(m, "gun_autoshotgun_sexy", 9.5, 90, 0, 0, 0); // ItemRenderSexy
        put(m, "gun_autoshotgun_heretic", 9.5, 90, 0, 0, 0); // ItemRenderSexy
        put(m, "gun_quadro", 30, 90, 0, -1.125, 0); // ItemRenderQuadro
        put(m, "gun_minigun", 6.25, 90, 0, 0, 0); // ItemRenderMinigun
        put(m, "gun_minigun_lacunae", 6.25, 90, 0, 0, 0); // ItemRenderMinigun
        put(m, "gun_minigun_dual", 6.25, 90, 0, 0, 0); // ItemRenderMinigunDual
        put(m, "gun_missile_launcher", 10, 90, 0, -1, 0); // ItemRenderMissileLauncher
        put(m, "gun_tesla_cannon", 8.75, 90, 0, 0.5, 0); // ItemRenderTeslaCannon
        put(m, "gun_laser_pistol", 10, 90, 0, -0.5, 0); // ItemRenderLaserPistol
        put(m, "gun_laser_pistol_pew_pew", 10, 90, 0, -0.5, 0); // ItemRenderLaserPistol
        put(m, "gun_laser_pistol_morning_glory", 10, 90, 0, -0.5, 0); // ItemRenderLaserPistol
        put(m, "gun_stg77", 7.5, 90, 0, 0, 0); // ItemRenderSTG77
        put(m, "gun_tau", 10, 90, 0, 0, 0); // ItemRenderTau
        put(m, "gun_fatman", 8.75, 90, 0, 0, 0); // ItemRenderFatMan
        put(m, "gun_lasrifle", 6.25, 90, 0, -1, -1); // ItemRenderLasrifle
        put(m, "gun_coilgun", 20, 90, 0, -0.25, 0.5); // ItemRenderCoilgun
        put(m, "gun_hangman", 2.5, 90, 0, 0, 0); // ItemRenderHangman
        put(m, "gun_mas36", 7.5, 90, 0, -0.25, -2.5); // ItemRenderMAS36
        put(m, "gun_bolter", 12.5, 90, 0, -0.5, 0); // ItemRenderBolter
        put(m, "gun_folly", 8.75, 90, 0, -1, 0); // ItemRenderFolly
        put(m, "gun_aberrator", 12.5, 90, 0, -1, 0.5); // ItemRenderAberrator
        put(m, "gun_aberrator_eott", 12.5, 90, 0, -1, 0.5); // ItemRenderEOTT
        put(m, "gun_double_barrel", 8.75, 90, 0, 0, 0); // ItemRenderDoubleBarrel
        put(m, "gun_double_barrel_sacred_dragon", 8.75, 90, 0, 0, 0); // ItemRenderDoubleBarrel
        put(m, "gun_charge_thrower", 8.5, 90, 0, 0, -1); // ItemRenderChargeThrower
        put(m, "gun_drill", 8.75, 90, 0, 0, 0); // ItemRenderDrill
        put(m, "gun_n_i_4_n_i", 15, 90, 0, -0.5, 0); // ItemRenderNI4NI
        put(m, "gun_pa_melee", 12.5, 90, 0, -0.5, 1); // ItemRenderPAMelee
        return m;
    }

    private static void put(Map<String, Frame> m, String item, double scale, double yaw, double x, double y, double z) {
        m.put(item, new Frame(scale, yaw, x, y, z));
    }
}
