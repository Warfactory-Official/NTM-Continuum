package com.hbm.items;

import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Model;
import dev.golem.core.dejson.Model.Element;
import dev.golem.core.dejson.Model.Face;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Slots;

/**
 * Item definitions that name one model: the item's own (declared here with it) or a model another
 * declaration owns. Moved out of {@code assets/hbm/items} and {@code assets/hbm/models/item}; see
 * {@link ModItemModels} for the generated sprite families.
 *
 * <p>Touches no Minecraft class: Dejson runs {@link #declare()} in a plain JVM at build time.
 */
public final class SpriteItemDeclarations {

    private static final String NS = "hbm";
    private static boolean declared;

    private SpriteItemDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;
        part0();
        part1();
        part2();
    }

    private static void part0() {
        Dejson.item(NS, "ammo_bag").models(Models.generated("hbm:items/ammo_bag")).register();
        Dejson.item(NS, "ammo_bag_infinite").models(Models.generated("hbm:items/ammo_bag_infinite")).register();
        Dejson.item(NS, "ammo_secret_bmg50_black").models(Models.generated("hbm:items/ammo_secret_bmg50_black")).register();
        Dejson.item(NS, "ammo_secret_bmg50_equestrian").models(Models.generated("hbm:items/ammo_secret_bmg50_equestrian")).register();
        Dejson.item(NS, "ammo_secret_folly_nuke").models(Models.generated("hbm:items/ammo_secret_folly_nuke")).register();
        Dejson.item(NS, "ammo_secret_folly_sm").models(Models.generated("hbm:items/ammo_secret_folly_sm")).register();
        Dejson.item(NS, "ammo_secret_g12_equestrian").models(Models.generated("hbm:items/ammo_secret_g12_equestrian")).register();
        Dejson.item(NS, "ammo_secret_p35_800").models(Models.generated("hbm:items/ammo_secret_p35_800")).register();
        Dejson.item(NS, "ammo_secret_p35_800_bl").models(Models.generated("hbm:items/ammo_secret_p35_800_bl")).register();
        Dejson.item(NS, "ammo_standard_b75").models(Models.generated("hbm:items/ammo_standard_b75")).register();
        Dejson.item(NS, "ammo_standard_b75_exp").models(Models.generated("hbm:items/ammo_standard_b75_exp")).register();
        Dejson.item(NS, "ammo_standard_b75_inc").models(Models.generated("hbm:items/ammo_standard_b75_inc")).register();
        Dejson.item(NS, "ammo_standard_capacitor").models(Models.generated("hbm:items/ammo_standard_capacitor")).register();
        Dejson.item(NS, "ammo_standard_capacitor_ir").models(Models.generated("hbm:items/ammo_standard_capacitor_ir")).register();
        Dejson.item(NS, "ammo_standard_capacitor_overcharge").models(Models.generated("hbm:items/ammo_standard_capacitor_overcharge")).register();
        Dejson.item(NS, "ammo_standard_coil_ferrouranium").models(Models.generated("hbm:items/ammo_standard_coil_ferrouranium")).register();
        Dejson.item(NS, "ammo_standard_coil_tungsten").models(Models.generated("hbm:items/ammo_standard_coil_tungsten")).register();
        Dejson.item(NS, "ammo_standard_ct_hook").models(Models.generated("hbm:items/ammo_standard_ct_hook")).register();
        Dejson.item(NS, "ammo_standard_ct_mortar").models(Models.generated("hbm:items/ammo_standard_ct_mortar")).register();
        Dejson.item(NS, "ammo_standard_ct_mortar_charge").models(Models.generated("hbm:items/ammo_standard_ct_mortar_charge")).register();
        Dejson.item(NS, "ammo_standard_flame_balefire").models(Models.generated("hbm:items/ammo_standard_flame_balefire")).register();
        Dejson.item(NS, "ammo_standard_flame_gas").models(Models.generated("hbm:items/ammo_standard_flame_gas")).register();
        Dejson.item(NS, "ammo_standard_flame_napalm").models(Models.generated("hbm:items/ammo_standard_flame_napalm")).register();
        Dejson.item(NS, "ammo_standard_g10").models(Models.generated("hbm:items/ammo_standard_g10")).register();
        Dejson.item(NS, "ammo_standard_g10_du").models(Models.generated("hbm:items/ammo_standard_g10_du")).register();
        Dejson.item(NS, "ammo_standard_g10_explosive").models(Models.generated("hbm:items/ammo_standard_g10_explosive")).register();
        Dejson.item(NS, "ammo_standard_g10_shrapnel").models(Models.generated("hbm:items/ammo_standard_g10_shrapnel")).register();
        Dejson.item(NS, "ammo_standard_g10_slug").models(Models.generated("hbm:items/ammo_standard_g10_slug")).register();
        Dejson.item(NS, "ammo_standard_g12").models(Models.generated("hbm:items/ammo_standard_g12")).register();
        Dejson.item(NS, "ammo_standard_g12_bp").models(Models.generated("hbm:items/ammo_standard_g12_bp")).register();
    }

    private static void part1() {
        Dejson.item(NS, "ammo_standard_g12_bp_magnum").models(Models.generated("hbm:items/ammo_standard_g12_bp_magnum")).register();
        Dejson.item(NS, "ammo_standard_g12_bp_slug").models(Models.generated("hbm:items/ammo_standard_g12_bp_slug")).register();
        Dejson.item(NS, "ammo_standard_g12_explosive").models(Models.generated("hbm:items/ammo_standard_g12_explosive")).register();
        Dejson.item(NS, "ammo_standard_g12_flechette").models(Models.generated("hbm:items/ammo_standard_g12_flechette")).register();
        Dejson.item(NS, "ammo_standard_g12_magnum").models(Models.generated("hbm:items/ammo_standard_g12_magnum")).register();
        Dejson.item(NS, "ammo_standard_g12_phosphorus").models(Models.generated("hbm:items/ammo_standard_g12_phosphorus")).register();
        Dejson.item(NS, "ammo_standard_g12_slug").models(Models.generated("hbm:items/ammo_standard_g12_slug")).register();
        Dejson.item(NS, "ammo_standard_g26_flare").models(Models.generated("hbm:items/ammo_standard_g26_flare")).register();
        Dejson.item(NS, "ammo_standard_g26_flare_supply").models(Models.generated("hbm:items/ammo_standard_g26_flare_supply")).register();
        Dejson.item(NS, "ammo_standard_g26_flare_weapon").models(Models.generated("hbm:items/ammo_standard_g26_flare_weapon")).register();
        Dejson.item(NS, "ammo_standard_g40_demo").models(Models.generated("hbm:items/ammo_standard_g40_demo")).register();
        Dejson.item(NS, "ammo_standard_g40_he").models(Models.generated("hbm:items/ammo_standard_g40_he")).register();
        Dejson.item(NS, "ammo_standard_g40_heat").models(Models.generated("hbm:items/ammo_standard_g40_heat")).register();
        Dejson.item(NS, "ammo_standard_g40_inc").models(Models.generated("hbm:items/ammo_standard_g40_inc")).register();
        Dejson.item(NS, "ammo_standard_g40_phosphorus").models(Models.generated("hbm:items/ammo_standard_g40_phosphorus")).register();
        Dejson.item(NS, "ammo_standard_nuke_balefire").models(Models.generated("hbm:items/ammo_standard_nuke_balefire")).register();
        Dejson.item(NS, "ammo_standard_nuke_demo").models(Models.generated("hbm:items/ammo_standard_nuke_demo")).register();
        Dejson.item(NS, "ammo_standard_nuke_hive").models(Models.generated("hbm:items/ammo_standard_nuke_hive")).register();
        Dejson.item(NS, "ammo_standard_nuke_standard").models(Models.generated("hbm:items/ammo_standard_nuke_standard")).register();
        Dejson.item(NS, "ammo_standard_nuke_tots").models(Models.generated("hbm:items/ammo_standard_nuke_tots")).register();
        Dejson.item(NS, "ammo_standard_p45_ap").models(Models.generated("hbm:items/ammo_standard_p45_ap")).register();
        Dejson.item(NS, "ammo_standard_p45_du").models(Models.generated("hbm:items/ammo_standard_p45_du")).register();
        Dejson.item(NS, "ammo_standard_p45_fmj").models(Models.generated("hbm:items/ammo_standard_p45_fmj")).register();
        Dejson.item(NS, "ammo_standard_p45_jhp").models(Models.generated("hbm:items/ammo_standard_p45_jhp")).register();
        Dejson.item(NS, "ammo_standard_p45_sp").models(Models.generated("hbm:items/ammo_standard_p45_sp")).register();
        Dejson.item(NS, "ammo_standard_r762_ap").models(Models.generated("hbm:items/ammo_standard_r762_ap")).register();
        Dejson.item(NS, "ammo_standard_r762_du").models(Models.generated("hbm:items/ammo_standard_r762_du")).register();
        Dejson.item(NS, "ammo_standard_r762_fmj").models(Models.generated("hbm:items/ammo_standard_r762_fmj")).register();
        Dejson.item(NS, "ammo_standard_r762_he").models(Models.generated("hbm:items/ammo_standard_r762_he")).register();
        Dejson.item(NS, "ammo_standard_r762_jhp").models(Models.generated("hbm:items/ammo_standard_r762_jhp")).register();
    }

    private static void part2() {
        Dejson.item(NS, "ammo_standard_r762_sp").models(Models.generated("hbm:items/ammo_standard_r762_sp")).register();
        Dejson.item(NS, "casing_bag").models(Models.generated("hbm:items/casing_bag")).register();
        Dejson.item(NS, "casing_buckshot_advanced").models(Models.generated("hbm:items/casing_buckshot_advanced")).register();
        Dejson.item(NS, "casing_shotshell").models(Models.generated("hbm:items/casing_shotshell")).register();
        Dejson.item(NS, "fluid_counter_valve").shares("hbm:block/fluid_counter_valve_off").register();
        Dejson.item(NS, "fluid_duct_gauge").shares("hbm:block/fluid_duct_gauge").register();
        Dejson.item(NS, "fluid_duct_paintable").shares("hbm:block/fluid_duct_paintable").register();
        Dejson.item(NS, "fluid_duct_paintable_block_exhaust").shares("hbm:block/fluid_duct_paintable_block_exhaust").register();
        Dejson.item(NS, "fluid_switch").shares("hbm:block/fluid_switch_off").register();
        Dejson.item(NS, "fluid_valve").shares("hbm:block/fluid_valve_off").register();
        Dejson.item(NS, "foam_layer").shares("hbm:item/foam_layer_height2").register();
        Dejson.item(NS, "foundry_channel").shares("hbm:block/foundry_channel_cross").register();
        Dejson.item(NS, "foundry_mold").shares("hbm:block/foundry_mold").register();
        Dejson.item(NS, "foundry_tank").models(Model.parent("minecraft:block/block")
                .textures(Slots.of("outer", "hbm:blocks/foundry_tank_side", "inner", "hbm:blocks/foundry_tank_inner", "top", "hbm:blocks/foundry_tank_top").and("bottom", "hbm:blocks/foundry_tank_bottom").and("particle", "hbm:blocks/foundry_tank_side"))
                .elements(Element.box(0, 0, 0, 16, 2, 16)
                        .face(Face.of("up", "#bottom"))
                        .face(Face.of("down", "#bottom")),
                        Element.box(0, 0, 0, 16, 16, 2)
                        .face(Face.of("north", "#outer"))
                        .face(Face.of("south", "#inner"))
                        .face(Face.of("east", "#outer"))
                        .face(Face.of("west", "#outer"))
                        .face(Face.of("up", "#top")),
                        Element.box(0, 0, 14, 16, 16, 16)
                        .face(Face.of("south", "#outer"))
                        .face(Face.of("north", "#inner"))
                        .face(Face.of("east", "#outer"))
                        .face(Face.of("west", "#outer"))
                        .face(Face.of("up", "#top")),
                        Element.box(0, 0, 2, 2, 16, 14)
                        .face(Face.of("west", "#outer"))
                        .face(Face.of("east", "#inner"))
                        .face(Face.of("up", "#top")),
                        Element.box(14, 0, 2, 16, 16, 14)
                        .face(Face.of("east", "#outer"))
                        .face(Face.of("west", "#inner"))
                        .face(Face.of("up", "#top")))).register();
        Dejson.item(NS, "gun_pa_ranged").models(Models.generated("hbm:items/gun_pa_ranged")).register();
        Dejson.item(NS, "key").models(Models.parented("item/handheld", Slots.of("layer0", "hbm:items/key"))).register();
        Dejson.item(NS, "key_fake").models(Models.parented("item/handheld", Slots.of("layer0", "hbm:items/key_gold"))).register();
        Dejson.item(NS, "machine_difurnace_off").shares("hbm:block/machine_difurnace_off").register();
        Dejson.item(NS, "machine_electric_furnace_off").shares("hbm:block/machine_electric_furnace_off").register();
        Dejson.item(NS, "machine_furnace_brick_off").shares("hbm:block/machine_furnace_brick_off").register();
        Dejson.item(NS, "machine_weapon_table").shares("hbm:block/machine_weapon_table").register();
        Dejson.item(NS, "oil_spill").shares("hbm:block/oil_spill_height2").register();
        Dejson.item(NS, "pipe_anchor").shares("hbm:block/pipe_anchor_up").register();
        Dejson.item(NS, "pneumatic_tube").models(Model.parent("minecraft:block/block")
                .textures(Slots.of("particle", "hbm:blocks/pneumatic_tube", "straight", "hbm:blocks/pneumatic_tube_straight", "connector", "hbm:blocks/pneumatic_tube_connector"))
                .elements(Element.box(5, 5, 0, 11, 11, 16)
                        .face(Face.of("down", "#straight").uv(5, 0, 11, 16).rotation(90))
                        .face(Face.of("up", "#straight").uv(5, 0, 11, 16).rotation(90))
                        .face(Face.of("north", "#connector").uv(5, 5, 11, 11))
                        .face(Face.of("south", "#connector").uv(5, 5, 11, 11))
                        .face(Face.of("west", "#straight").uv(0, 5, 16, 11))
                        .face(Face.of("east", "#straight").uv(0, 5, 16, 11)))).register();
        Dejson.item(NS, "red_cable_classic").shares("hbm:block/red_cable_classic_item").register();
        Dejson.item(NS, "red_cable_paintable").shares("hbm:block/red_cable_paintable").register();
        Dejson.item(NS, "sand_boron_layer").shares("hbm:block/sand_boron_layer_height2").register();
    }
}
