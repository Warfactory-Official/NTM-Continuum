package com.hbm.items.weapon;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 1.7 {@code render/util/MissilePart.registerAllParts}, transcribed row for row: for every custom missile part
 * that has a body, how tall it stacks in an assembled missile ({@code height}), how tall its own icon frames
 * itself ({@code guiHeight}), and which OBJ and skin it is drawn with.
 *
 * <p>Plain data with no Minecraft class in reach: the item registration reads it to give each part its assembly
 * mesh, and the Dejson declarations read it in a plain JVM to frame each part's 3D icon. The five guidance chips
 * are absent exactly as they were upstream, which is why they stay flat sprites.
 *
 * <p>Oddities kept from upstream: {@code mp_thruster_15_hydrogen} reuses the kerosene body, several fuselages
 * share one mesh with a different skin, and {@code mp_s_20} wears {@code TheGadget3_.png} (lower-cased here,
 * as 26.x resource ids must be).
 */
public final class MissilePartTable {

    private MissilePartTable() {}

    public record Row(String id, float height, float guiHeight, String mesh, String skin) {}

    private static final Map<String, Row> ROWS = new LinkedHashMap<>();

    public static Row get(String id) {
        return ROWS.get(id);
    }

    public static Map<String, Row> rows() {
        return Collections.unmodifiableMap(ROWS);
    }

    public static void overrideAssembly(String id, float height, float guiHeight) {
        var row = ROWS.get(id);
        ROWS.put(id, new Row(id, height, guiHeight, row.mesh(), row.skin()));
    }

    private static void put(String id, float height, float guiHeight, String mesh, String skin) {
        ROWS.put(id, new Row(id, height, guiHeight, mesh, skin));
    }

    static {
        put("mp_thruster_10_kerosene", 1.0F, 1.0F, "models/missile_parts/mp_t_10_kerosene.obj", "textures/models/missile_parts/thrusters/mp_t_10_kerosene.png");
        put("mp_thruster_10_solid", 0.5F, 1.0F, "models/missile_parts/mp_t_10_solid.obj", "textures/models/missile_parts/thrusters/mp_t_10_solid.png");
        put("mp_thruster_10_xenon", 0.5F, 1.0F, "models/missile_parts/mp_t_10_xenon.obj", "textures/models/missile_parts/thrusters/mp_t_10_xenon.png");
        put("mp_thruster_15_kerosene", 1.5F, 1.5F, "models/missile_parts/mp_t_15_kerosene.obj", "textures/models/missile_parts/thrusters/mp_t_15_kerosene.png");
        put("mp_thruster_15_kerosene_dual", 1.0F, 1.5F, "models/missile_parts/mp_t_15_kerosene_dual.obj", "textures/models/missile_parts/thrusters/mp_t_15_kerosene_dual.png");
        put("mp_thruster_15_kerosene_triple", 1.0F, 1.5F, "models/missile_parts/mp_t_15_kerosene_triple.obj", "textures/models/missile_parts/thrusters/mp_t_15_kerosene_dual.png");
        put("mp_thruster_15_solid", 0.5F, 1.0F, "models/missile_parts/mp_t_15_solid.obj", "textures/models/missile_parts/thrusters/mp_t_15_solid.png");
        put("mp_thruster_15_solid_hexdecuple", 0.5F, 1.0F, "models/missile_parts/mp_t_15_solid_hexdecuple.obj", "textures/models/missile_parts/thrusters/mp_t_15_solid_hexdecuple.png");
        put("mp_thruster_15_hydrogen", 1.5F, 1.5F, "models/missile_parts/mp_t_15_kerosene.obj", "textures/models/missile_parts/thrusters/mp_t_15_hydrogen.png");
        put("mp_thruster_15_hydrogen_dual", 1.0F, 1.5F, "models/missile_parts/mp_t_15_kerosene_dual.obj", "textures/models/missile_parts/thrusters/mp_t_15_hydrogen_dual.png");
        put("mp_thruster_15_balefire_short", 2.0F, 2.0F, "models/missile_parts/mp_t_15_balefire_short.obj", "textures/models/missile_parts/thrusters/mp_t_15_balefire_short.png");
        put("mp_thruster_15_balefire", 3.0F, 2.5F, "models/missile_parts/mp_t_15_balefire.obj", "textures/models/missile_parts/thrusters/mp_t_15_balefire.png");
        put("mp_thruster_15_balefire_large", 3.0F, 2.5F, "models/missile_parts/mp_t_15_balefire_large.obj", "textures/models/missile_parts/thrusters/mp_t_15_balefire_large.png");
        put("mp_thruster_15_balefire_large_rad", 3.0F, 2.5F, "models/missile_parts/mp_t_15_balefire_large.obj", "textures/models/missile_parts/thrusters/mp_t_15_balefire_large_rad.png");
        put("mp_thruster_20_kerosene", 3.0F, 2.5F, "models/missile_parts/mp_t_20_kerosene.obj", "textures/models/missile_parts/thrusters/mp_t_20_kerosene.png");
        put("mp_thruster_20_kerosene_dual", 2.0F, 2.0F, "models/missile_parts/mp_t_20_kerosene_dual.obj", "textures/models/missile_parts/thrusters/mp_t_20_kerosene_dual.png");
        put("mp_thruster_20_kerosene_triple", 2.0F, 2.0F, "models/missile_parts/mp_t_20_kerosene_triple.obj", "textures/models/missile_parts/thrusters/mp_t_20_kerosene_dual.png");
        put("mp_thruster_20_solid", 1.0F, 1.75F, "models/missile_parts/mp_t_20_solid.obj", "textures/models/missile_parts/thrusters/mp_t_20_solid.png");
        put("mp_thruster_20_solid_multi", 0.5F, 1.5F, "models/missile_parts/mp_t_20_solid_multi.obj", "textures/models/missile_parts/thrusters/mp_t_20_solid_multi.png");
        put("mp_thruster_20_solid_multier", 0.5F, 1.5F, "models/missile_parts/mp_t_20_solid_multi.obj", "textures/models/missile_parts/thrusters/mp_t_20_solid_multier.png");
        put("mp_stability_10_flat", 0.0F, 2.0F, "models/missile_parts/mp_s_10_flat.obj", "textures/models/missile_parts/stability/mp_s_10_flat.png");
        put("mp_stability_10_cruise", 0.0F, 3.0F, "models/missile_parts/mp_s_10_cruise.obj", "textures/models/missile_parts/stability/mp_s_10_cruise.png");
        put("mp_stability_10_space", 0.0F, 2.0F, "models/missile_parts/mp_s_10_space.obj", "textures/models/missile_parts/stability/mp_s_10_space.png");
        put("mp_stability_15_flat", 0.0F, 3.0F, "models/missile_parts/mp_s_15_flat.obj", "textures/models/missile_parts/stability/mp_s_15_flat.png");
        put("mp_stability_15_thin", 0.0F, 3.0F, "models/missile_parts/mp_s_15_thin.obj", "textures/models/missile_parts/stability/mp_s_15_thin.png");
        put("mp_stability_15_soyuz", 0.0F, 3.0F, "models/missile_parts/mp_s_15_soyuz.obj", "textures/models/missile_parts/stability/mp_s_15_soyuz.png");
        put("mp_s_20", 0.0F, 3.0F, "models/missile_parts/mp_s_20.obj", "textures/models/thegadget3_.png");
        put("mp_fuselage_10_kerosene", 4.0F, 3.0F, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_kerosene.png");
        put("mp_fuselage_10_kerosene_camo", 4.0F, 3.0F, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_kerosene_camo.png");
        put("mp_fuselage_10_kerosene_desert", 4.0F, 3.0F, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_kerosene_desert.png");
        put("mp_fuselage_10_kerosene_sky", 4.0F, 3.0F, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_kerosene_sky.png");
        put("mp_fuselage_10_kerosene_insulation", 4.0F, 3.0F, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_kerosene_insulation.png");
        put("mp_fuselage_10_kerosene_flames", 4.0F, 3.0F, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_kerosene_flames.png");
        put("mp_fuselage_10_kerosene_sleek", 4.0F, 3.0F, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_kerosene_sleek.png");
        put("mp_fuselage_10_kerosene_metal", 4.0F, 3.0F, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_kerosene_metal.png");
        put("mp_fuselage_10_kerosene_taint", 4.0F, 3.0F, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_10_kerosene_taint.png");
        put("mp_fuselage_10_solid", 4.0F, 3.0F, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_solid.png");
        put("mp_fuselage_10_solid_flames", 4.0F, 3.0F, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_solid_flames.png");
        put("mp_fuselage_10_solid_insulation", 4.0F, 3.0F, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_solid_insulation.png");
        put("mp_fuselage_10_solid_sleek", 4.0F, 3.0F, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_solid_sleek.png");
        put("mp_fuselage_10_solid_soviet_glory", 4.0F, 3.0F, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_solid_soviet_glory.png");
        put("mp_fuselage_10_solid_cathedral", 4.0F, 3.0F, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_10_solid_cathedral.png");
        put("mp_fuselage_10_solid_moonlit", 4.0F, 3.0F, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_10_solid_moonlit.png");
        put("mp_fuselage_10_solid_battery", 4.0F, 3.0F, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_10_solid_battery.png");
        put("mp_fuselage_10_solid_duracell", 4.0F, 3.0F, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_solid_duracell.png");
        put("mp_fuselage_10_xenon", 4.0F, 3.0F, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_xenon.png");
        put("mp_fuselage_10_xenon_bhole", 4.0F, 3.0F, "models/missile_parts/mp_f_10_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_10_xenon_bhole.png");
        put("mp_fuselage_10_long_kerosene", 7.0F, 5.0F, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_long_kerosene.png");
        put("mp_fuselage_10_long_kerosene_camo", 7.0F, 5.0F, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_long_kerosene_camo.png");
        put("mp_fuselage_10_long_kerosene_desert", 7.0F, 5.0F, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_long_kerosene_desert.png");
        put("mp_fuselage_10_long_kerosene_sky", 7.0F, 5.0F, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_long_kerosene_sky.png");
        put("mp_fuselage_10_long_kerosene_flames", 7.0F, 5.0F, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_long_kerosene_flames.png");
        put("mp_fuselage_10_long_kerosene_insulation", 7.0F, 5.0F, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_long_kerosene_insulation.png");
        put("mp_fuselage_10_long_kerosene_sleek", 7.0F, 5.0F, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_long_kerosene_sleek.png");
        put("mp_fuselage_10_long_kerosene_metal", 7.0F, 5.0F, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_long_kerosene_metal.png");
        put("mp_fuselage_10_long_kerosene_dash", 7.0F, 5.0F, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_10_long_kerosene_dash.png");
        put("mp_fuselage_10_long_kerosene_taint", 7.0F, 5.0F, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_10_long_kerosene_taint.png");
        put("mp_fuselage_10_long_kerosene_vap", 7.0F, 5.0F, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_10_long_kerosene_vap.png");
        put("mp_fuselage_10_long_solid", 7.0F, 5.0F, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_long_solid.png");
        put("mp_fuselage_10_long_solid_flames", 7.0F, 5.0F, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_long_solid_flames.png");
        put("mp_fuselage_10_long_solid_insulation", 7.0F, 5.0F, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_long_solid_insulation.png");
        put("mp_fuselage_10_long_solid_sleek", 7.0F, 5.0F, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_long_solid_sleek.png");
        put("mp_fuselage_10_long_solid_soviet_glory", 7.0F, 5.0F, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_long_solid_soviet_glory.png");
        put("mp_fuselage_10_long_solid_bullet", 7.0F, 5.0F, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_10_long_solid_bullet.png");
        put("mp_fuselage_10_long_solid_silvermoonlight", 7.0F, 5.0F, "models/missile_parts/mp_f_10_long_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_10_long_solid_silvermoonlight.png");
        put("mp_fuselage_10_15_kerosene", 9.0F, 5.5F, "models/missile_parts/mp_f_10_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_15_kerosene.png");
        put("mp_fuselage_10_15_solid", 9.0F, 5.5F, "models/missile_parts/mp_f_10_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_15_solid.png");
        put("mp_fuselage_10_15_hydrogen", 9.0F, 5.5F, "models/missile_parts/mp_f_10_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_15_hydrogen.png");
        put("mp_fuselage_10_15_balefire", 9.0F, 5.5F, "models/missile_parts/mp_f_10_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_10_15_balefire.png");
        put("mp_fuselage_15_kerosene", 10.0F, 6.0F, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_kerosene.png");
        put("mp_fuselage_15_kerosene_camo", 10.0F, 6.0F, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_kerosene_camo.png");
        put("mp_fuselage_15_kerosene_desert", 10.0F, 6.0F, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_kerosene_desert.png");
        put("mp_fuselage_15_kerosene_sky", 10.0F, 6.0F, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_kerosene_sky.png");
        put("mp_fuselage_15_kerosene_insulation", 10.0F, 6.0F, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_kerosene_insulation.png");
        put("mp_fuselage_15_kerosene_metal", 10.0F, 6.0F, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_kerosene_metal.png");
        put("mp_fuselage_15_kerosene_decorated", 10.0F, 6.0F, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_kerosene_decorated.png");
        put("mp_fuselage_15_kerosene_steampunk", 10.0F, 6.0F, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_kerosene_steampunk.png");
        put("mp_fuselage_15_kerosene_polite", 10.0F, 6.0F, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_kerosene_polite.png");
        put("mp_fuselage_15_kerosene_blackjack", 10.0F, 6.0F, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/base/mp_f_15_kerosene_blackjack.png");
        put("mp_fuselage_15_kerosene_lambda", 10.0F, 6.0F, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_15_kerosene_lambda.png");
        put("mp_fuselage_15_kerosene_minuteman", 10.0F, 6.0F, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_15_kerosene_minuteman.png");
        put("mp_fuselage_15_kerosene_pip", 10.0F, 6.0F, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_15_kerosene_pip.png");
        put("mp_fuselage_15_kerosene_taint", 10.0F, 6.0F, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_15_kerosene_taint.png");
        put("mp_fuselage_15_kerosene_yuck", 10.0F, 6.0F, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_kerosene_yuck.png");
        put("mp_fuselage_15_solid", 10.0F, 6.0F, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_solid.png");
        put("mp_fuselage_15_solid_insulation", 10.0F, 6.0F, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_solid_insulation.png");
        put("mp_fuselage_15_solid_desh", 10.0F, 6.0F, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_solid_desh.png");
        put("mp_fuselage_15_solid_soviet_glory", 10.0F, 6.0F, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_solid_soviet_glory.png");
        put("mp_fuselage_15_solid_soviet_stank", 10.0F, 6.0F, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_solid_soviet_stank.png");
        put("mp_fuselage_15_solid_faust", 10.0F, 6.0F, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_15_solid_faust.png");
        put("mp_fuselage_15_solid_silvermoonlight", 10.0F, 6.0F, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_15_solid_silvermoonlight.png");
        put("mp_fuselage_15_solid_snowy", 10.0F, 6.0F, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/contest/mp_f_15_solid_snowy.png");
        put("mp_fuselage_15_solid_panorama", 10.0F, 6.0F, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_solid_panorama.png");
        put("mp_fuselage_15_solid_roses", 10.0F, 6.0F, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_solid_roses.png");
        put("mp_fuselage_15_solid_mimi", 10.0F, 6.0F, "models/missile_parts/mp_f_15_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_solid_mimi.png");
        put("mp_fuselage_15_hydrogen", 10.0F, 6.0F, "models/missile_parts/mp_f_15_hydrogen.obj", "textures/models/missile_parts/fuselages/mp_f_15_hydrogen.png");
        put("mp_fuselage_15_hydrogen_cathedral", 10.0F, 6.0F, "models/missile_parts/mp_f_15_hydrogen.obj", "textures/models/missile_parts/fuselages/contest/mp_f_15_hydrogen_cathedral.png");
        put("mp_fuselage_15_balefire", 10.0F, 6.0F, "models/missile_parts/mp_f_15_hydrogen.obj", "textures/models/missile_parts/fuselages/mp_f_15_balefire.png");
        put("mp_fuselage_15_20_kerosene", 16.0F, 10.0F, "models/missile_parts/mp_f_15_20_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_20_kerosene.png");
        put("mp_fuselage_15_20_kerosene_magnusson", 16.0F, 10.0F, "models/missile_parts/mp_f_15_20_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_20_kerosene_magnusson.png");
        put("mp_fuselage_15_20_solid", 16.0F, 10.0F, "models/missile_parts/mp_f_15_20_kerosene.obj", "textures/models/missile_parts/fuselages/mp_f_15_20_solid.png");
        put("mp_warhead_10_he", 2.0F, 1.5F, "models/missile_parts/mp_w_10_he.obj", "textures/models/missile_parts/warheads/mp_w_10_he.png");
        put("mp_warhead_10_incendiary", 2.5F, 2.0F, "models/missile_parts/mp_w_10_incendiary.obj", "textures/models/missile_parts/warheads/mp_w_10_incendiary.png");
        put("mp_warhead_10_buster", 0.5F, 1.0F, "models/missile_parts/mp_w_10_buster.obj", "textures/models/missile_parts/warheads/mp_w_10_buster.png");
        put("mp_warhead_10_nuclear", 2.0F, 1.5F, "models/missile_parts/mp_w_10_nuclear.obj", "textures/models/missile_parts/warheads/mp_w_10_nuclear.png");
        put("mp_warhead_10_nuclear_large", 2.5F, 1.5F, "models/missile_parts/mp_w_10_nuclear_large.obj", "textures/models/missile_parts/warheads/mp_w_10_nuclear_large.png");
        put("mp_warhead_10_taint", 2.25F, 1.5F, "models/missile_parts/mp_w_10_taint.obj", "textures/models/missile_parts/warheads/mp_w_10_taint.png");
        put("mp_warhead_10_cloud", 2.25F, 1.5F, "models/missile_parts/mp_w_10_taint.obj", "textures/models/missile_parts/warheads/mp_w_10_cloud.png");
        put("mp_warhead_15_he", 2.0F, 1.5F, "models/missile_parts/mp_w_15_he.obj", "textures/models/missile_parts/warheads/mp_w_15_he.png");
        put("mp_warhead_15_incendiary", 2.0F, 1.5F, "models/missile_parts/mp_w_15_incendiary.obj", "textures/models/missile_parts/warheads/mp_w_15_incendiary.png");
        put("mp_warhead_15_nuclear", 3.5F, 2.0F, "models/missile_parts/mp_w_15_nuclear.obj", "textures/models/missile_parts/warheads/mp_w_15_nuclear.png");
        put("mp_warhead_15_nuclear_shark", 3.5F, 2.0F, "models/missile_parts/mp_w_15_nuclear.obj", "textures/models/missile_parts/warheads/mp_w_15_nuclear_shark.png");
        put("mp_warhead_15_nuclear_mimi", 3.5F, 2.0F, "models/missile_parts/mp_w_15_nuclear.obj", "textures/models/missile_parts/warheads/mp_w_15_nuclear_mimi.png");
        put("mp_warhead_15_boxcar", 2.25F, 7.5F, "models/missile_parts/mp_w_15_boxcar.obj", "textures/models/boxcar.png");
        put("mp_warhead_15_n2", 3.0F, 2.0F, "models/missile_parts/mp_w_15_n2.obj", "textures/models/missile_parts/warheads/mp_w_15_n2.png");
        put("mp_warhead_15_balefire", 2.75F, 2.0F, "models/missile_parts/mp_w_15_balefire.obj", "textures/models/missile_parts/warheads/mp_w_15_balefire.png");
        put("mp_warhead_15_turbine", 2.25F, 2.0F, "models/missile_parts/mp_w_15_turbine.obj", "textures/models/missile_parts/warheads/mp_w_15_turbine.png");
    }
}
