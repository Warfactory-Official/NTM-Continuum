package com.hbm.items;

import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Model;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Slots;

/**
 * Item models no single item declaration owns: select cases, shared parents and the legacy
 * {@code builtin/entity} markers the OBJ and gun items resolve through below 26.1.
 *
 * <p>Touches no Minecraft class: Dejson runs {@link #declare()} in a plain JVM at build time.
 */
public final class ItemModelDeclarations {

    private static final String NS = "hbm";
    private static boolean declared;

    private ItemModelDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;
        part0();
        part1();
        part2();
    }

    private static void part0() {
        Dejson.model("hbm:item/ammo_container_alt", Models.generated("hbm:items/ammo_container_alt"));
        Dejson.model("hbm:item/gun_b92_base", Model.parent("hbm:item/gun_obj_base").guiLight("side")
                .textures(Slots.of("particle", "hbm:items/gun_b92")));
        Dejson.model("hbm:item/gun_obj_dani_base", Model.parent("hbm:item/gun_obj_base").guiLight("side"));
        Dejson.model("hbm:item/gun_obj_g3_base", Model.parent("hbm:item/gun_obj_base").guiLight("side"));
        Dejson.model("hbm:item/gun_obj_legacy", Model.parent("minecraft:builtin/entity").guiLight("side"));
        Dejson.model("hbm:item/gun_obj_pepperbox_base", Model.parent("hbm:item/gun_obj_base").guiLight("side"));
        Dejson.model("hbm:item/machine_obj_base", Models.parented("minecraft:block/block", Slots.of("particle", "hbm:blocks/block_steel")));
        Dejson.model("hbm:item/machine_obj_legacy", Model.parent("minecraft:builtin/entity").guiLight("side")
                .display("gui", Model.xyz(30, 225, 0), Model.xyz(0, 0, 0), Model.xyz(0.625, 0.625, 0.625))
                .display("ground", Model.xyz(0, 0, 0), Model.xyz(0, 3, 0), Model.xyz(0.25, 0.25, 0.25))
                .display("fixed", Model.xyz(0, 0, 0), Model.xyz(0, 0, 0), Model.xyz(0.5, 0.5, 0.5))
                .display("thirdperson_righthand", Model.xyz(75, 45, 0), Model.xyz(0, 2.5, 0), Model.xyz(0.375, 0.375, 0.375))
                .display("firstperson_righthand", Model.xyz(0, 45, 0), Model.xyz(0, 0, 0), Model.xyz(0.4, 0.4, 0.4))
                .display("firstperson_lefthand", Model.xyz(0, 225, 0), Model.xyz(0, 0, 0), Model.xyz(0.4, 0.4, 0.4)));
        Dejson.model("hbm:item/machine_weapon_table", Models.parented("hbm:block/machine_weapon_table", Slots.EMPTY));
        Dejson.model("hbm:item/weapon_mod_caliber.bmg50", Models.generated("hbm:items/weapon_mod_caliber.bmg50"));
        Dejson.model("hbm:item/weapon_mod_caliber.m357", Models.generated("hbm:items/weapon_mod_caliber.m357"));
        Dejson.model("hbm:item/weapon_mod_caliber.m44", Models.generated("hbm:items/weapon_mod_caliber.m44"));
        Dejson.model("hbm:item/weapon_mod_caliber.p22", Models.generated("hbm:items/weapon_mod_caliber.p22"));
        Dejson.model("hbm:item/weapon_mod_caliber.p45", Models.generated("hbm:items/weapon_mod_caliber.p45"));
        Dejson.model("hbm:item/weapon_mod_caliber.p9", Models.generated("hbm:items/weapon_mod_caliber.p9"));
        Dejson.model("hbm:item/weapon_mod_caliber.r556", Models.generated("hbm:items/weapon_mod_caliber.r556"));
        Dejson.model("hbm:item/weapon_mod_caliber.r762", Models.generated("hbm:items/weapon_mod_caliber.r762"));
        Dejson.model("hbm:item/weapon_mod_generic.bigmt_damage", Models.generated("hbm:items/weapon_mod_generic.bigmt_damage"));
        Dejson.model("hbm:item/weapon_mod_generic.bigmt_dura", Models.generated("hbm:items/weapon_mod_generic.bigmt_dura"));
        Dejson.model("hbm:item/weapon_mod_generic.bronze_damage", Models.generated("hbm:items/weapon_mod_generic.bronze_damage"));
        Dejson.model("hbm:item/weapon_mod_generic.bronze_dura", Models.generated("hbm:items/weapon_mod_generic.bronze_dura"));
        Dejson.model("hbm:item/weapon_mod_generic.desh_damage", Models.generated("hbm:items/weapon_mod_generic.desh_damage"));
        Dejson.model("hbm:item/weapon_mod_generic.desh_dura", Models.generated("hbm:items/weapon_mod_generic.desh_dura"));
        Dejson.model("hbm:item/weapon_mod_generic.dura_damage", Models.generated("hbm:items/weapon_mod_generic.dura_damage"));
        Dejson.model("hbm:item/weapon_mod_generic.dura_dura", Models.generated("hbm:items/weapon_mod_generic.dura_dura"));
        Dejson.model("hbm:item/weapon_mod_generic.ferro_damage", Models.generated("hbm:items/weapon_mod_generic.ferro_damage"));
        Dejson.model("hbm:item/weapon_mod_generic.ferro_dura", Models.generated("hbm:items/weapon_mod_generic.ferro_dura"));
        Dejson.model("hbm:item/weapon_mod_generic.iron_damage", Models.generated("hbm:items/weapon_mod_generic.iron_damage"));
        Dejson.model("hbm:item/weapon_mod_generic.iron_dura", Models.generated("hbm:items/weapon_mod_generic.iron_dura"));
        Dejson.model("hbm:item/weapon_mod_generic.steel_damage", Models.generated("hbm:items/weapon_mod_generic.steel_damage"));
    }

    private static void part1() {
        Dejson.model("hbm:item/weapon_mod_generic.steel_dura", Models.generated("hbm:items/weapon_mod_generic.steel_dura"));
        Dejson.model("hbm:item/weapon_mod_generic.tcalloy_damage", Models.generated("hbm:items/weapon_mod_generic.tcalloy_damage"));
        Dejson.model("hbm:item/weapon_mod_generic.tcalloy_dura", Models.generated("hbm:items/weapon_mod_generic.tcalloy_dura"));
        Dejson.model("hbm:item/weapon_mod_generic.wsteel_damage", Models.generated("hbm:items/weapon_mod_generic.wsteel_damage"));
        Dejson.model("hbm:item/weapon_mod_generic.wsteel_dura", Models.generated("hbm:items/weapon_mod_generic.wsteel_dura"));
        Dejson.model("hbm:item/weapon_mod_special.bayonet", Models.generated("hbm:items/weapon_mod_special.bayonet"));
        Dejson.model("hbm:item/weapon_mod_special.canisters", Models.generated("hbm:items/weapon_mod_special.canisters"));
        Dejson.model("hbm:item/weapon_mod_special.choke", Models.generated("hbm:items/weapon_mod_special.choke"));
        Dejson.model("hbm:item/weapon_mod_special.doubloons", Models.generated("hbm:items/weapon_mod_special.doubloons"));
        Dejson.model("hbm:item/weapon_mod_special.drill_hss", Models.generated("hbm:items/weapon_mod_special.drill_hss"));
        Dejson.model("hbm:item/weapon_mod_special.drill_saturnite", Models.generated("hbm:items/weapon_mod_special.drill_saturnite"));
        Dejson.model("hbm:item/weapon_mod_special.drill_tcalloy", Models.generated("hbm:items/weapon_mod_special.drill_tcalloy"));
        Dejson.model("hbm:item/weapon_mod_special.drill_weaponsteel", Models.generated("hbm:items/weapon_mod_special.drill_weaponsteel"));
        Dejson.model("hbm:item/weapon_mod_special.engine_aviation", Models.generated("hbm:items/weapon_mod_special.engine_aviation"));
        Dejson.model("hbm:item/weapon_mod_special.engine_diesel", Models.generated("hbm:items/weapon_mod_special.engine_diesel"));
        Dejson.model("hbm:item/weapon_mod_special.engine_electric", Models.generated("hbm:items/weapon_mod_special.engine_electric"));
        Dejson.model("hbm:item/weapon_mod_special.engine_turbo", Models.generated("hbm:items/weapon_mod_special.engine_turbo"));
        Dejson.model("hbm:item/weapon_mod_special.furniture_black", Models.generated("hbm:items/weapon_mod_special.furniture_black"));
        Dejson.model("hbm:item/weapon_mod_special.furniture_green", Models.generated("hbm:items/weapon_mod_special.furniture_green"));
        Dejson.model("hbm:item/weapon_mod_special.greasegun", Models.generated("hbm:items/weapon_mod_special.greasegun"));
        Dejson.model("hbm:item/weapon_mod_special.las_auto", Models.generated("hbm:items/weapon_mod_special.las_auto"));
        Dejson.model("hbm:item/weapon_mod_special.las_capacitor", Models.generated("hbm:items/weapon_mod_special.las_capacitor"));
        Dejson.model("hbm:item/weapon_mod_special.las_shotgun", Models.generated("hbm:items/weapon_mod_special.las_shotgun"));
        Dejson.model("hbm:item/weapon_mod_special.magnet", Models.generated("hbm:items/weapon_mod_special.magnet"));
        Dejson.model("hbm:item/weapon_mod_special.nickel", Models.generated("hbm:items/weapon_mod_special.nickel"));
        Dejson.model("hbm:item/weapon_mod_special.saw", Models.generated("hbm:items/weapon_mod_special.saw"));
        Dejson.model("hbm:item/weapon_mod_special.scope", Models.generated("hbm:items/weapon_mod_special.scope"));
        Dejson.model("hbm:item/weapon_mod_special.sifter", Models.generated("hbm:items/weapon_mod_special.sifter"));
        Dejson.model("hbm:item/weapon_mod_special.silencer", Models.generated("hbm:items/weapon_mod_special.silencer"));
        Dejson.model("hbm:item/weapon_mod_special.skin_saturnite", Models.generated("hbm:items/weapon_mod_special.skin_saturnite"));
    }

    private static void part2() {
        Dejson.model("hbm:item/weapon_mod_special.slowdown", Models.generated("hbm:items/weapon_mod_special.slowdown"));
        Dejson.model("hbm:item/weapon_mod_special.speedloader", Models.generated("hbm:items/weapon_mod_special.speedloader"));
        Dejson.model("hbm:item/weapon_mod_special.speedup", Models.generated("hbm:items/weapon_mod_special.speedup"));
        Dejson.model("hbm:item/weapon_mod_special.stack_mag", Models.generated("hbm:items/weapon_mod_special.stack_mag"));
        Dejson.model("hbm:item/weapon_mod_test.damage", Models.generated("hbm:items/weapon_mod_test.damage"));
        Dejson.model("hbm:item/weapon_mod_test.firerate", Models.generated("hbm:items/weapon_mod_test.firerate"));
        Dejson.model("hbm:item/weapon_mod_test.multi", Models.generated("hbm:items/weapon_mod_test.multi"));
        Dejson.model("hbm:item/weapon_mod_test.override_10", Models.generated("hbm:items/weapon_mod_test.override_10"));
        Dejson.model("hbm:item/weapon_mod_test.override_12_5", Models.generated("hbm:items/weapon_mod_test.override_12_5"));
        Dejson.model("hbm:item/weapon_mod_test.override_15", Models.generated("hbm:items/weapon_mod_test.override_15"));
        Dejson.model("hbm:item/weapon_mod_test.override_20", Models.generated("hbm:items/weapon_mod_test.override_20"));
        Dejson.model("hbm:item/weapon_mod_test.override_2_5", Models.generated("hbm:items/weapon_mod_test.override_2_5"));
        Dejson.model("hbm:item/weapon_mod_test.override_5", Models.generated("hbm:items/weapon_mod_test.override_5"));
        Dejson.model("hbm:item/weapon_mod_test.override_7_5", Models.generated("hbm:items/weapon_mod_test.override_7_5"));
    }
}
