package com.hbm.inventory.recipes;

import com.hbm.items.special.CellVariants;
import com.hbm.items.special.ItemHot;
import com.hbm.lib.ComponentIds;
import com.hbm.lib.ItemIds;
import com.hbm.lib.TagIds;
import com.hbm.lib.VanillaIds;

import static dev.golem.core.dejson.Crafting.componentShaped;
import static dev.golem.core.dejson.Crafting.componentSmelt;
import static dev.golem.core.dejson.Crafting.copyShapeless;
import static dev.golem.core.dejson.Crafting.componentShapeless;
import static dev.golem.core.dejson.Crafting.fluid;
import static dev.golem.core.dejson.Crafting.tag;
import static dev.golem.core.dejson.Crafting.item;
import static dev.golem.core.dejson.Crafting.key;
import static dev.golem.core.dejson.Crafting.rows;
import static dev.golem.core.dejson.Crafting.special;
import static dev.golem.core.dejson.Crafting.stack;

/**
 * The hbm recipes matched on data components rather than on ids alone.
 *
 * <p>These are the mod's own recipe types, and what makes them their own is that an ingredient
 * here carries a constraint on the stack's components: the arty shell already at variant zero, the
 * cell that has to be empty, the fluid tank that is spent and left behind as an empty tank. The
 * ingredient vocabulary says those three things directly — {@code with}, {@code without} and
 * {@code remainder}.
 *
 * <p>{@code rbmk_fuel_disassembly} is here too, and is the one recipe with nothing to declare: its
 * matching and its output are decided in {@link RBMKFuelDisassemblyRecipe}, and the file exists
 * only to put that serialiser in front of the recipe manager.
 */
public final class ModComponentRecipes {

    private static final String NS = "hbm";

    /** How many recipe files these declarations stand in for; quoted in the javadoc above. */
    static final int FILES_REPLACED = 95;

    private static boolean declared;

    private ModComponentRecipes() {}

    public static void declare() {
        if (declared) {
            return;
        }
        declared = true;

        shaped();
        smelting();
        copying();
        programs();
    }

    /**
     * The {@code fluid(...)} keys are 1.7's {@code Fluids.X.getDict(1000)}: any full container of that fluid
     * (bucket, tank, canister, lead tank...), which leaves its empty container in the grid. The keys matched on
     * the tank itself ({@code fluid_tank} with a fluid id) are 1.7 recipes that named {@code fluid_tank_full}.
     */
    private static void shaped() {
        componentShaped(NS, "ammo_arty_1", stack("hbm:ammo_arty_classic"),
                rows(" D ", "DSD", " D "),
                key("D", item(ItemIds.ball_dynamite)),
                key("S", item("hbm:ammo_arty")));
        componentShaped(NS, "ammo_arty_2", stack("hbm:ammo_arty_he"),
                rows("TTT", "TST", "TTT"),
                key("T", item(ItemIds.ball_tnt)), key("S", item("hbm:ammo_arty")));
        componentShaped(NS, "ammo_arty_3", stack("hbm:ammo_arty_mini_nuke"),
                rows(" P ", "NSN", " P "),
                key("P", item(ItemIds.nugget_pu239)), key("N", item(ItemIds.neutron_reflector)),
                key("S", item("hbm:ammo_arty")));
        componentShaped(NS, "ams_core_eyeofharmony", stack(ItemIds.ams_core_eyeofharmony), rows("ALA", "LSL", "ALA"),
                key("A", item(ItemIds.plate_dalekanium)),
                key("L", item(ItemIds.fluid_barrel).with(ComponentIds.fluid, "hbm:lava").with(ComponentIds.fluid_amount, 16000).remainder(ItemIds.fluid_barrel)),
                key("S", item(ItemIds.black_hole)));
        // 1.7 CraftingManager:600/342/325 use cell_anti_schrabidium (fluid 34 ASCHRAB), not the antimatter cell
        componentShaped(NS, "ams_core_sing", stack(ItemIds.ams_core_sing), rows("EAE", "ASA", "EAE"),
                key("E", item(ItemIds.plate_euphemium)),
                key("A", item(ItemIds.cell).with(ComponentIds.fluid, "hbm:aschrab").with(ComponentIds.fluid_amount, 1000).remainder(ItemIds.cell)),
                key("S", item(ItemIds.singularity)));
        componentShaped(NS, "ams_core_wormhole", stack(ItemIds.ams_core_wormhole), rows("DPD", "PSP", "DPD"),
                key("D", item(ItemIds.plate_dineutronium)), key("P", item(ItemIds.powder_spark_mix)),
                key("S", item(ItemIds.singularity)));
        componentShaped(NS, "ams_lens", stack(ItemIds.ams_lens), rows("PDP", "GDG", "PDP"),
                key("P", item(ItemIds.plate_dineutronium)), key("G", item(ItemIds.reinforced_glass)),
                key("D", item(VanillaIds.diamond_block)));
        componentShaped(NS, "barbed_wire_acid", stack(ItemIds.barbed_wire_acid, 8), rows("BBB", "BIB", "BBB"),
                key("B", item(ItemIds.barbed_wire)),
                key("I", item(ItemIds.fluid_tank).with(ComponentIds.fluid, "hbm:peroxide").with(ComponentIds.fluid_amount, 1000).remainder(ItemIds.fluid_tank)));
        componentShaped(NS, "bj_plate_jetpack", stack(ItemIds.bj_plate_jetpack), rows("NFN", "TPT", "ICI"),
                key("N", item(ItemIds.plate_armor_lunar)), key("F", item(ItemIds.fins_quad_titanium)),
                key("T", item(ItemIds.fluid_tank).with(ComponentIds.fluid, "hbm:xenon").with(ComponentIds.fluid_amount, 1000).remainder(ItemIds.fluid_tank)),
                key("P", item(ItemIds.bj_plate)), key("I", item(ItemIds.mp_thruster_10_xenon)),
                key("C", item(ItemIds.crystal_phosphorus)));
        componentShaped(NS, "cell_1",
                stack(ItemIds.cell, 8).with(ComponentIds.fluid, "hbm:deuterium").with(ComponentIds.fluid_amount, 1000),
                rows("DDD", "DTD", "DDD"),
                key("D", item(ItemIds.cell).without(ComponentIds.fluid, ComponentIds.fluid_amount)),
                key("T", item(ItemIds.mike_deut)));
        componentShaped(NS, "concrete_colored_ext_stairs_bronze_from_block",
                stack(ItemIds.concrete_colored_ext_stairs_bronze, 8), rows("#  ", "## ", "###"),
                key("#", item("hbm:concrete_ext_bronze")));
        componentShaped(NS, "concrete_colored_ext_stairs_hazard_from_block",
                stack(ItemIds.concrete_colored_ext_stairs_hazard, 8), rows("#  ", "## ", "###"),
                key("#", item("hbm:concrete_ext_hazard")));
        componentShaped(NS, "concrete_colored_ext_stairs_indigo_from_block",
                stack(ItemIds.concrete_colored_ext_stairs_indigo, 8), rows("#  ", "## ", "###"),
                key("#", item("hbm:concrete_ext_indigo")));
        componentShaped(NS, "concrete_colored_ext_stairs_machine_from_block",
                stack(ItemIds.concrete_colored_ext_stairs_machine, 8), rows("#  ", "## ", "###"),
                key("#", item("hbm:concrete_ext_machine")));
        componentShaped(NS, "concrete_colored_ext_stairs_machine_stripe_from_block",
                stack(ItemIds.concrete_colored_ext_stairs_machine_stripe, 8), rows("#  ", "## ", "###"),
                key("#", item("hbm:concrete_ext_machine_stripe")));
        componentShaped(NS, "concrete_colored_ext_stairs_pink_from_block",
                stack(ItemIds.concrete_colored_ext_stairs_pink, 8), rows("#  ", "## ", "###"),
                key("#", item("hbm:concrete_ext_pink")));
        componentShaped(NS, "concrete_colored_ext_stairs_purple_from_block",
                stack(ItemIds.concrete_colored_ext_stairs_purple, 8), rows("#  ", "## ", "###"),
                key("#", item("hbm:concrete_ext_purple")));
        componentShaped(NS, "concrete_colored_ext_stairs_sand_from_block",
                stack(ItemIds.concrete_colored_ext_stairs_sand, 8), rows("#  ", "## ", "###"),
                key("#", item("hbm:concrete_ext_sand")));
        componentShaped(NS, "concrete_colored_stairs_black", stack(ItemIds.concrete_colored_stairs_black, 8),
                rows("#  ", "## ", "###"),
                key("#", item("hbm:concrete_black")));
        componentShaped(NS, "concrete_colored_stairs_black_slab", stack(ItemIds.concrete_black_slab, 6), rows("###"),
                key("#", item("hbm:concrete_black")));
        componentShaped(NS, "concrete_colored_stairs_blue", stack(ItemIds.concrete_colored_stairs_blue, 8),
                rows("#  ", "## ", "###"),
                key("#", item("hbm:concrete_blue")));
        componentShaped(NS, "concrete_colored_stairs_blue_slab", stack(ItemIds.concrete_blue_slab, 6), rows("###"),
                key("#", item("hbm:concrete_blue")));
        componentShaped(NS, "concrete_colored_stairs_brown", stack(ItemIds.concrete_colored_stairs_brown, 8),
                rows("#  ", "## ", "###"),
                key("#", item("hbm:concrete_brown")));
        componentShaped(NS, "concrete_colored_stairs_brown_slab", stack(ItemIds.concrete_brown_slab, 6), rows("###"),
                key("#", item("hbm:concrete_brown")));
        componentShaped(NS, "concrete_colored_stairs_cyan", stack(ItemIds.concrete_colored_stairs_cyan, 8),
                rows("#  ", "## ", "###"),
                key("#", item("hbm:concrete_cyan")));
        componentShaped(NS, "concrete_colored_stairs_cyan_slab", stack(ItemIds.concrete_cyan_slab, 6), rows("###"),
                key("#", item("hbm:concrete_cyan")));
        componentShaped(NS, "concrete_colored_stairs_gray", stack(ItemIds.concrete_colored_stairs_gray, 8),
                rows("#  ", "## ", "###"),
                key("#", item("hbm:concrete_gray")));
        componentShaped(NS, "concrete_colored_stairs_gray_slab", stack(ItemIds.concrete_gray_slab, 6), rows("###"),
                key("#", item("hbm:concrete_gray")));
        componentShaped(NS, "concrete_colored_stairs_green", stack(ItemIds.concrete_colored_stairs_green, 8),
                rows("#  ", "## ", "###"),
                key("#", item("hbm:concrete_green")));
        componentShaped(NS, "concrete_colored_stairs_green_slab", stack(ItemIds.concrete_green_slab, 6), rows("###"),
                key("#", item("hbm:concrete_green")));
        componentShaped(NS, "concrete_colored_stairs_light_blue", stack(ItemIds.concrete_colored_stairs_light_blue, 8),
                rows("#  ", "## ", "###"),
                key("#", item("hbm:concrete_light_blue")));
        componentShaped(NS, "concrete_colored_stairs_light_blue_slab", stack(ItemIds.concrete_light_blue_slab, 6),
                rows("###"),
                key("#", item("hbm:concrete_light_blue")));
        componentShaped(NS, "concrete_colored_stairs_lime", stack(ItemIds.concrete_colored_stairs_lime, 8),
                rows("#  ", "## ", "###"),
                key("#", item("hbm:concrete_lime")));
        componentShaped(NS, "concrete_colored_stairs_lime_slab", stack(ItemIds.concrete_lime_slab, 6), rows("###"),
                key("#", item("hbm:concrete_lime")));
        componentShaped(NS, "concrete_colored_stairs_magenta", stack(ItemIds.concrete_colored_stairs_magenta, 8),
                rows("#  ", "## ", "###"),
                key("#", item("hbm:concrete_magenta")));
        componentShaped(NS, "concrete_colored_stairs_magenta_slab", stack(ItemIds.concrete_magenta_slab, 6), rows("###"),
                key("#", item("hbm:concrete_magenta")));
        componentShaped(NS, "concrete_colored_stairs_orange", stack(ItemIds.concrete_colored_stairs_orange, 8),
                rows("#  ", "## ", "###"),
                key("#", item("hbm:concrete_orange")));
        componentShaped(NS, "concrete_colored_stairs_orange_slab", stack(ItemIds.concrete_orange_slab, 6), rows("###"),
                key("#", item("hbm:concrete_orange")));
        componentShaped(NS, "concrete_colored_stairs_pink", stack(ItemIds.concrete_colored_stairs_pink, 8),
                rows("#  ", "## ", "###"),
                key("#", item("hbm:concrete_pink")));
        componentShaped(NS, "concrete_colored_stairs_pink_slab", stack(ItemIds.concrete_pink_slab, 6), rows("###"),
                key("#", item("hbm:concrete_pink")));
        componentShaped(NS, "concrete_colored_stairs_purple", stack(ItemIds.concrete_colored_stairs_purple, 8),
                rows("#  ", "## ", "###"),
                key("#", item("hbm:concrete_purple")));
        componentShaped(NS, "concrete_colored_stairs_purple_slab", stack(ItemIds.concrete_purple_slab, 6), rows("###"),
                key("#", item("hbm:concrete_purple")));
        componentShaped(NS, "concrete_colored_stairs_red", stack(ItemIds.concrete_colored_stairs_red, 8),
                rows("#  ", "## ", "###"),
                key("#", item("hbm:concrete_red")));
        componentShaped(NS, "concrete_colored_stairs_red_slab", stack(ItemIds.concrete_red_slab, 6), rows("###"),
                key("#", item("hbm:concrete_red")));
        componentShaped(NS, "concrete_colored_stairs_silver", stack(ItemIds.concrete_colored_stairs_silver, 8),
                rows("#  ", "## ", "###"),
                key("#", item("hbm:concrete_silver")));
        componentShaped(NS, "concrete_colored_stairs_silver_slab", stack(ItemIds.concrete_silver_slab, 6), rows("###"),
                key("#", item("hbm:concrete_silver")));
        componentShaped(NS, "concrete_colored_stairs_white", stack(ItemIds.concrete_colored_stairs_white, 8),
                rows("#  ", "## ", "###"),
                key("#", item("hbm:concrete_white")));
        componentShaped(NS, "concrete_colored_stairs_white_slab", stack(ItemIds.concrete_white_slab, 6), rows("###"),
                key("#", item("hbm:concrete_white")));
        componentShaped(NS, "concrete_colored_stairs_yellow", stack(ItemIds.concrete_colored_stairs_yellow, 8),
                rows("#  ", "## ", "###"),
                key("#", item("hbm:concrete_yellow")));
        componentShaped(NS, "concrete_colored_stairs_yellow_slab", stack(ItemIds.concrete_yellow_slab, 6), rows("###"),
                key("#", item("hbm:concrete_yellow")));
        componentShaped(NS, "conveyor_wand_express", stack(ItemIds.conveyor_wand_express, 8), rows("CCC", "CLC", "CCC"),
                key("C", item(ItemIds.conveyor_wand_regular)),
                key("L", fluid("LUBRICANT", 1000)));
        componentShaped(NS, "flame_conspiracy", stack(ItemIds.flame_conspiracy), rows(" S ", "STS", " S "),
                key("S", fluid("KEROSENE", 1000)),
                key("T", item(ItemIds.ingot_steel)));
        componentShaped(NS, "jetpack_tank", stack(ItemIds.jetpack_tank), rows(" S ", "BKB", " S "),
                key("S", item(ItemIds.plate_steel)), key("B", item(ItemIds.bolt_steel)),
                key("K", fluid("KEROSENE", 1000)));
        // 1.7 CraftingManager: shapeless { KEY_ANYGLASS, P_RED.dust(), Fluids.TRITIUM.getDict(1_000), AL/S.dust() }
        componentShapeless(NS, "lamp_tritium_blue_off", stack(ItemIds.lamp_tritium_blue_off),
                tag(TagIds.glass_blocks), tag(TagIds.dusts_red_phosphorus), fluid("TRITIUM", 1000),
                tag(TagIds.dusts_aluminium));
        componentShapeless(NS, "lamp_tritium_green_off", stack(ItemIds.lamp_tritium_green_off),
                tag(TagIds.glass_blocks), tag(TagIds.dusts_red_phosphorus), fluid("TRITIUM", 1000),
                tag(TagIds.dusts_sulfur));
        componentShaped(NS, "laser_crystal_cmb", stack(ItemIds.laser_crystal_cmb), rows("QBQ", "CSC", "QBQ"),
                key("Q", item(ItemIds.glass_quartz)), key("B", item(ItemIds.ingot_combine_steel)),
                key("C", item(ItemIds.ingot_schrabidate)),
                key("S", item(ItemIds.cell).with(ComponentIds.fluid, "hbm:aschrab").with(ComponentIds.fluid_amount, 1000).remainder(ItemIds.cell)));
        componentShaped(NS, "laser_crystal_co2", stack(ItemIds.laser_crystal_co2), rows("QDQ", "NCN", "QDQ"),
                key("Q", item(ItemIds.glass_quartz)), key("D", item(ItemIds.ingot_desh)),
                key("N", item(ItemIds.ingot_niobium)),
                key("C", item(ItemIds.fluid_tank).with(ComponentIds.fluid, "hbm:carbondioxide").with(ComponentIds.fluid_amount, 1000).remainder(ItemIds.fluid_tank)));
        componentShaped(NS, "lightstone_bricks", stack("hbm:lightstone_bricks", 4),
                rows("CC", "CC"),
                key("C", item("hbm:lightstone_tile")));
        componentShaped(NS, "lightstone_bricks_chiseled", stack("hbm:lightstone_bricks_chiseled"),
                rows("#"),
                key("#", item("hbm:lightstone_bricks")));
        componentShaped(NS, "lightstone_bricks_stairs_from_block", stack(ItemIds.lightstone_bricks_stairs, 4),
                rows("#  ", "## ", "###"),
                key("#", item("hbm:lightstone_bricks")));
        // 1.7 CraftingManager:426: shapeless CHISELED from a bare ModBlocks.lightstone, which vanilla's shapeless
        // recipe reads as metadata 0 - the unrefined block, not the tile
        componentShaped(NS, "lightstone_chiseled", stack("hbm:lightstone_chiseled"),
                rows("#"),
                key("#", item("hbm:lightstone_unrefined")));
        componentShaped(NS, "lightstone_tile", stack("hbm:lightstone_tile", 4),
                rows("CC", "CC"),
                key("C", item("hbm:lightstone_unrefined")));
        componentShaped(NS, "lightstone_tile_stairs_from_block", stack(ItemIds.lightstone_tile_stairs, 4),
                rows("#  ", "## ", "###"),
                key("#", item("hbm:lightstone_tile")));
        componentShaped(NS, "part_generic_piston_hydraulic", stack(ItemIds.part_generic_piston_hydraulic, 4),
                rows(" I ", "CPC", " I "),
                key("I", item(ItemIds.ingot_steel)), key("C", item(ItemIds.ingot_titanium)),
                key("P", fluid("LUBRICANT", 1000)));
        componentShaped(NS, "pellet_antimatter", stack(ItemIds.pellet_antimatter), rows("###", "###", "###"),
                key("#", item(ItemIds.cell).with(ComponentIds.fluid, "hbm:amat").with(ComponentIds.fluid_amount, 1000).remainder(ItemIds.cell)));
        componentShaped(NS, "press_preheater", stack(ItemIds.press_preheater), rows("CCC", "SLS", "TST"),
                key("C", item(ItemIds.plate_copper)), key("S", item(VanillaIds.stone)),
                key("L", fluid("LAVA", 1000)),
                key("T", item(ItemIds.ingot_tungsten)));
        componentShaped(NS, "rod_dual_tritium_unload",
                stack(ItemIds.cell, 2).with(ComponentIds.fluid, "hbm:tritium").with(ComponentIds.fluid_amount, 1000),
                rows("RCC"),
                key("R", item(ItemIds.rod_dual_tritium)),
                key("C", item(ItemIds.cell).without(ComponentIds.fluid, ComponentIds.fluid_amount)));
        componentShaped(NS, "rod_quad_tritium_unload",
                stack(ItemIds.cell, 4).with(ComponentIds.fluid, "hbm:tritium").with(ComponentIds.fluid_amount, 1000),
                rows("RCC", "CC "),
                key("R", item(ItemIds.rod_quad_tritium)),
                key("C", item(ItemIds.cell).without(ComponentIds.fluid, ComponentIds.fluid_amount)));
        componentShaped(NS, "rod_tritium_unload",
                stack(ItemIds.cell).with(ComponentIds.fluid, "hbm:tritium").with(ComponentIds.fluid_amount, 1000),
                rows("RC"),
                key("R", item(ItemIds.rod_tritium)),
                key("C", item(ItemIds.cell).without(ComponentIds.fluid, ComponentIds.fluid_amount)));
        componentShaped(NS, "singularity", stack(ItemIds.singularity), rows("ESE", "SBS", "ESE"),
                key("E", item(ItemIds.nugget_euphemium)),
                key("S", item(ItemIds.cell).with(ComponentIds.fluid, "hbm:aschrab").with(ComponentIds.fluid_amount, 1000).remainder(ItemIds.cell)),
                key("B", item(ItemIds.block_schrabidium)));
        componentShaped(NS, "struct_soyuz_core", stack(ItemIds.struct_soyuz_core), rows("CUC", "TST", "TBT"),
                key("C", item(ItemIds.circuit_advanced)), key("U", item(ItemIds.upgrade_power_3)),
                key("T", item(ItemIds.barrel_steel)), key("S", item(ItemIds.steel_scaffold)),
                key("B", item(ItemIds.battery_lithium)));
        componentShaped(NS, "syringe_empty", stack(ItemIds.syringe_empty, 6), rows("P", "C", "B"),
                key("B", item(VanillaIds.iron_bars)),
                key("C", item(ItemIds.cell).without(ComponentIds.fluid, ComponentIds.fluid_amount)),
                key("P", item(ItemIds.plate_iron)));
    }

    private static final String FORGE_COUNT = "hbm:forge_count";

    private static void smelting() {
        componentSmelt(NS, "ingot_chainsteel", item(ItemIds.ingot_chainsteel),
                stack(ItemIds.ingot_chainsteel).with(ComponentIds.heat, ItemHot.LEGACY_HEAT), 0.0);
        // 1.7 metadata = forge count: hbm:forge_count, absent on an unforged ingot (History §19es-itemvariant)
        componentSmelt(NS, "ingot_steel_dusted", item(ItemIds.ingot_steel_dusted).without(FORGE_COUNT),
                stack(ItemIds.ingot_steel_dusted).with(ComponentIds.heat, ItemHot.LEGACY_HEAT), 1.0);
        for (int count = 1; count <= 9; count++) {
            componentSmelt(NS, "ingot_steel_dusted_" + count, item(ItemIds.ingot_steel_dusted).with(FORGE_COUNT, count),
                    stack(ItemIds.ingot_steel_dusted).with(ComponentIds.heat, ItemHot.LEGACY_HEAT).with(FORGE_COUNT, count), 1.0);
        }
    }

    private static void copying() {
        copyShapeless(NS, "rod_zirnox_les_fuel", ItemIds.rod_zirnox_les_fuel, ItemIds.rod_zirnox_empty,
                ItemIds.rod_zirnox_empty, ItemIds.billet_les, ItemIds.billet_les);
        copyShapeless(NS, "rod_zirnox_lithium_fuel", ItemIds.rod_zirnox_lithium_fuel, ItemIds.rod_zirnox_empty,
                ItemIds.rod_zirnox_empty, TagIds.ingots_lithium, TagIds.ingots_lithium);
        copyShapeless(NS, "rod_zirnox_mox_fuel", ItemIds.rod_zirnox_mox_fuel, ItemIds.rod_zirnox_empty,
                ItemIds.rod_zirnox_empty, ItemIds.billet_mox_fuel, ItemIds.billet_mox_fuel);
        copyShapeless(NS, "rod_zirnox_natural_uranium_fuel", ItemIds.rod_zirnox_natural_uranium_fuel,
                ItemIds.rod_zirnox_empty,
                ItemIds.rod_zirnox_empty, ItemIds.billet_uranium, ItemIds.billet_uranium);
        copyShapeless(NS, "rod_zirnox_plutonium_fuel", ItemIds.rod_zirnox_plutonium_fuel, ItemIds.rod_zirnox_empty,
                ItemIds.rod_zirnox_empty, ItemIds.billet_plutonium_fuel, ItemIds.billet_plutonium_fuel);
        copyShapeless(NS, "rod_zirnox_th232_fuel", ItemIds.rod_zirnox_th232_fuel, ItemIds.rod_zirnox_empty,
                ItemIds.rod_zirnox_empty, ItemIds.billet_th232, ItemIds.billet_th232);
        copyShapeless(NS, "rod_zirnox_thorium_fuel", ItemIds.rod_zirnox_thorium_fuel, ItemIds.rod_zirnox_empty,
                ItemIds.rod_zirnox_empty, ItemIds.billet_thorium_fuel, ItemIds.billet_thorium_fuel);
        copyShapeless(NS, "rod_zirnox_u233_fuel", ItemIds.rod_zirnox_u233_fuel, ItemIds.rod_zirnox_empty,
                ItemIds.rod_zirnox_empty, ItemIds.billet_u233, ItemIds.billet_u233);
        copyShapeless(NS, "rod_zirnox_u235_fuel", ItemIds.rod_zirnox_u235_fuel, ItemIds.rod_zirnox_empty,
                ItemIds.rod_zirnox_empty, ItemIds.billet_u235, ItemIds.billet_u235);
        copyShapeless(NS, "rod_zirnox_uranium_fuel", ItemIds.rod_zirnox_uranium_fuel, ItemIds.rod_zirnox_empty,
                ItemIds.rod_zirnox_empty, ItemIds.billet_uranium_fuel, ItemIds.billet_uranium_fuel);
        copyShapeless(NS, "rod_zirnox_zfb_mox_fuel", ItemIds.rod_zirnox_zfb_mox_fuel, ItemIds.rod_zirnox_empty,
                ItemIds.rod_zirnox_empty, ItemIds.billet_mox_fuel, ItemIds.billet_zirconium);
    }

    private static void programs() {
        special(NS, "rbmk_fuel_disassembly", "hbm:rbmk_fuel_disassembly");
    }
}
