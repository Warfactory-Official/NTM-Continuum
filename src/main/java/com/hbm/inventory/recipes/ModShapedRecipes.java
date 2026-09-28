package com.hbm.inventory.recipes;

import com.hbm.lib.ComponentIds;
import com.hbm.lib.ItemIds;
import com.hbm.lib.TagIds;
import com.hbm.lib.VanillaIds;

import static dev.golem.core.dejson.Crafting.rows;
import static dev.golem.core.dejson.Crafting.shaped;
import static dev.golem.core.dejson.Crafting.stack;

/**
 * Every shaped hbm recipe, declared through Golem Core's {@code dejson} vocabulary.
 *
 * <p>A pattern and its symbols are the recipe, and they are all that is here. The type line, the
 * category, the key block and the result wrapper were the same in all {@value #FILES_REPLACED} of
 * the files these stand in for, and a declaration says them once.
 *
 * <p>Recipe ids are carried through unchanged rather than derived from the output. They are visible
 * — the recipe book remembers them, JEI bookmarks them — so renaming one would reset progress
 * for no gain.
 */
public final class ModShapedRecipes {

    private static final String NS = "hbm";

    /** How many recipe files these declarations stand in for; quoted in the javadoc above. */
    static final int FILES_REPLACED = 918;

    private static boolean declared;

    private ModShapedRecipes() {}

    public static void declare() {
        if (declared) {
            return;
        }
        declared = true;

        shaped0();
        shaped1();
        shaped2();
        shaped3();
        shaped4();
        shaped5();
        shaped6();
        shaped7();
        shaped8();
    }

    private static void shaped0() {
        shaped(NS, "crowbar", "hbm:crowbar", rows("II", " I", " I"), "I", TagIds.ingots_steel);
        shaped(NS, "rangefinder", "hbm:rangefinder", rows("GRC", "  S"),
                "G", VanillaIds.glass, "R", VanillaIds.redstone, "C", ItemIds.circuit_basic, "S", TagIds.ingots_steel);
        shaped(NS, "designator_manual", "hbm:designator_manual", rows("  A", "#C#", "#B#"),
                "A", ItemIds.crystal_redstone, "C", ItemIds.circuit_basic, "B", ItemIds.battery_redstone, "#", TagIds.ingots_steel);
        shaped(NS, "ammo_arty", stack(ItemIds.ammo_arty).with(ComponentIds.item_variant, 0), rows("CIC", "CSC", "CCC"),
                "C", ItemIds.cordite, "I", TagIds.storage_blocks_iron, "S", TagIds.shells_copper);
        shaped(NS, "ammo_arty_4", stack(ItemIds.ammo_arty).with(ComponentIds.item_variant, 8), rows(" I ", " S ", "CCC"),
                "C", ItemIds.cordite, "I", ItemIds.sphere_steel, "S", TagIds.shells_copper);
        shaped(NS, "ammo_dgk", ItemIds.ammo_dgk, rows("LLL", "GGG", "CCC"),
                "L", TagIds.plates_lead, "G", ItemIds.ballistite, "C", TagIds.ingots_copper);
        shaped(NS, "ammo_dgk_1", ItemIds.ammo_dgk, rows("LLL", "GGG", "CCC"),
                "L", TagIds.plates_lead, "G", ItemIds.cordite, "C", TagIds.ingots_copper);
        shaped(NS, "ammo_shell", ItemIds.ammo_shell, 4, rows(" T ", "GHG", "CCC"),
                "T", ItemIds.tnt_ntm, "G", VanillaIds.gunpowder, "H", TagIds.shells_steel, "C", TagIds.ingots_copper);
        shaped(NS, "ammo_shell_1", ItemIds.ammo_shell, 4, rows(" T ", "GHG", "CCC"),
                "T", ItemIds.tnt_ntm, "G", ItemIds.ballistite, "H", TagIds.shells_steel, "C", TagIds.ingots_copper);
        shaped(NS, "ammo_shell_2", ItemIds.ammo_shell, 6, rows(" T ", "GHG", "CCC"),
                "T", ItemIds.tnt_ntm, "G", ItemIds.cordite, "H", TagIds.shells_steel, "C", TagIds.ingots_copper);
        shaped(NS, "ammo_shell_apfsds_du", ItemIds.ammo_shell_apfsds_du, 4, rows(" I ", "GIG", "CCC"),
                "I", TagIds.ingots_u238, "G", VanillaIds.gunpowder, "C", TagIds.ingots_copper);
        shaped(NS, "ammo_shell_apfsds_du_1", ItemIds.ammo_shell_apfsds_du, 4, rows(" I ", "GIG", "CCC"),
                "I", TagIds.ingots_u238, "G", ItemIds.ballistite, "C", TagIds.ingots_copper);
        shaped(NS, "ammo_shell_apfsds_du_2", ItemIds.ammo_shell_apfsds_du, 6, rows(" I ", "GIG", "CCC"),
                "I", TagIds.ingots_u238, "G", ItemIds.cordite, "C", TagIds.ingots_copper);
        shaped(NS, "ammo_shell_apfsds_t", ItemIds.ammo_shell_apfsds_t, 4, rows(" I ", "GIG", "CCC"),
                "I", TagIds.ingots_tungsten, "G", VanillaIds.gunpowder, "C", TagIds.ingots_copper);
        shaped(NS, "ammo_shell_apfsds_t_1", ItemIds.ammo_shell_apfsds_t, 4, rows(" I ", "GIG", "CCC"),
                "I", TagIds.ingots_tungsten, "G", ItemIds.ballistite, "C", TagIds.ingots_copper);
        shaped(NS, "ammo_shell_apfsds_t_2", ItemIds.ammo_shell_apfsds_t, 6, rows(" I ", "GIG", "CCC"),
                "I", TagIds.ingots_tungsten, "G", ItemIds.cordite, "C", TagIds.ingots_copper);
        shaped(NS, "ammo_shell_explosive", ItemIds.ammo_shell_explosive, 4, rows(" T ", "GHG", "CCC"),
                "T", TagIds.ingots_any_plasticexplosive, "G", VanillaIds.gunpowder, "H", TagIds.shells_steel,
                "C", TagIds.ingots_copper);
        shaped(NS, "ammo_shell_explosive_1", ItemIds.ammo_shell_explosive, 4, rows(" T ", "GHG", "CCC"),
                "T", TagIds.ingots_any_plasticexplosive, "G", ItemIds.ballistite, "H", TagIds.shells_steel,
                "C", TagIds.ingots_copper);
        shaped(NS, "ammo_shell_explosive_2", ItemIds.ammo_shell_explosive, 6, rows(" T ", "GHG", "CCC"),
                "T", TagIds.ingots_any_plasticexplosive, "G", ItemIds.cordite, "H", TagIds.shells_steel,
                "C", TagIds.ingots_copper);
        shaped(NS, "ammo_shell_w9", ItemIds.ammo_shell_w9, rows(" P ", "NSN", " P "),
                "P", TagIds.nuggets_pu239, "N", ItemIds.neutron_reflector, "S", ItemIds.ammo_shell_explosive);
        shaped(NS, "ammo_standard_stone", ItemIds.ammo_standard_stone, 6, rows("C", "P", "G"),
                "C", VanillaIds.cobblestone, "P", VanillaIds.paper, "G", VanillaIds.gunpowder);
        shaped(NS, "ammo_standard_stone_ap", ItemIds.ammo_standard_stone_ap, 6, rows("C", "P", "G"),
                "C", VanillaIds.flint, "P", VanillaIds.paper, "G", VanillaIds.gunpowder);
        shaped(NS, "ammo_standard_stone_iron", ItemIds.ammo_standard_stone_iron, 6, rows("C", "P", "G"),
                "C", TagIds.ingots_iron, "P", VanillaIds.paper, "G", VanillaIds.gunpowder);
        shaped(NS, "ammo_standard_stone_shot", ItemIds.ammo_standard_stone_shot, 6, rows("C", "P", "G"),
                "C", VanillaIds.gravel, "P", VanillaIds.paper, "G", VanillaIds.gunpowder);
        shaped(NS, "anvil_iron", ItemIds.anvil_iron, rows("III", " B ", "III"),
                "I", TagIds.ingots_iron, "B", TagIds.storage_blocks_iron);
        shaped(NS, "anvil_lead", ItemIds.anvil_lead, rows("III", " B ", "III"),
                "I", TagIds.ingots_lead, "B", TagIds.storage_blocks_lead);
        shaped(NS, "anvil_murky", ItemIds.anvil_murky, rows("UUU", "UAU", "UUU"),
                "U", ItemIds.undefined, "A", ItemIds.anvil_steel);
        shaped(NS, "arc_electrode_desh", ItemIds.arc_electrode_desh, rows("C", "T", "C"),
                "C", TagIds.ingots_desh, "T", TagIds.ingots_titanium);
        shaped(NS, "arc_electrode_desh_1", ItemIds.arc_electrode_desh, rows("C", "T", "C"),
                "C", TagIds.ingots_desh, "T", TagIds.ingots_tungsten);
        shaped(NS, "arc_electrode_desh_titanium", ItemIds.arc_electrode_desh, rows("C", "T", "C"),
                "C", TagIds.ingots_desh, "T", TagIds.ingots_titanium);
        shaped(NS, "arc_electrode_desh_tungsten", ItemIds.arc_electrode_desh, rows("C", "T", "C"),
                "C", TagIds.ingots_desh, "T", TagIds.ingots_tungsten);
        shaped(NS, "arc_electrode_graphite", ItemIds.arc_electrode, rows("C", "T", "C"),
                "C", TagIds.ingots_graphite, "T", TagIds.bolts_steel);
        shaped(NS, "arc_electrode_graphite_coke", ItemIds.arc_electrode, rows("C", "T", "C"),
                "C", ItemIds.coke_petroleum, "T", TagIds.tars);
        shaped(NS, "arc_electrode_lanthanium", ItemIds.arc_electrode_lanthanium, rows("C", "T", "C"),
                "C", TagIds.ingots_lanthanium, "T", VanillaIds.brick);
        shaped(NS, "arc_electrode_saturnite", ItemIds.arc_electrode_saturnite, rows("C", "T", "C"),
                "C", TagIds.ingots_saturnite, "T", TagIds.ingots_niobium);
        shaped(NS, "armor_battery", ItemIds.armor_battery, rows("PWP", "PCP", "PWP"),
                "P", TagIds.plates_steel, "C", ItemIds.capacitor_gold, "W", TagIds.wires_dense_red_copper);
        shaped(NS, "armor_battery_mk2", ItemIds.armor_battery_mk2, rows("PWP", "PCP", "PWP"),
                "P", TagIds.ingots_any_plastic, "C", ItemIds.capacitor_niobium, "W", TagIds.wires_dense_red_copper);
        shaped(NS, "armor_battery_mk3", ItemIds.armor_battery_mk3, rows("PWP", "PCP", "PWP"),
                "P", TagIds.plates_gold, "C", ItemIds.capacitor_tantalum, "W", TagIds.wires_dense_red_copper);
        shaped(NS, "asbestos_boots", ItemIds.asbestos_boots, rows("E E", "E E"), "E", ItemIds.asbestos_cloth);
        shaped(NS, "asbestos_helmet", ItemIds.asbestos_helmet, rows("EEE", "EIE"),
                "E", ItemIds.asbestos_cloth, "I", TagIds.plates_gold);
        shaped(NS, "asbestos_legs", ItemIds.asbestos_legs, rows("EEE", "E E", "E E"), "E", ItemIds.asbestos_cloth);
        shaped(NS, "asbestos_plate", ItemIds.asbestos_plate, rows("E E", "EEE", "EEE"), "E", ItemIds.asbestos_cloth);
        shaped(NS, "asphalt_slab", ItemIds.asphalt_slab, 6, rows("###"), "#", ItemIds.asphalt);
        shaped(NS, "asphalt_stairs", ItemIds.asphalt_stairs, 4, rows("C  ", "CC ", "CCC"), "C", ItemIds.asphalt);
        shaped(NS, "assembly_nuke", ItemIds.assembly_nuke, rows(" WP", "SEP", " WP"),
                "W", TagIds.wires_gold, "P", TagIds.plates_steel, "S", TagIds.shells_steel, "E", ItemIds.ball_tatb);
        shaped(NS, "ball_resin", ItemIds.ball_resin, rows("DD", "DD"), "D", VanillaIds.dandelion);
        shaped(NS, "barbed_wire", ItemIds.barbed_wire, 16, rows("AIA", "I I", "AIA"),
                "A", TagIds.wires_steel, "I", TagIds.ingots_iron);
        shaped(NS, "barbed_wire_fire", ItemIds.barbed_wire_fire, 8, rows("BBB", "BIB", "BBB"),
                "B", ItemIds.barbed_wire, "I", TagIds.dusts_red_phosphorus);
        shaped(NS, "barbed_wire_poison", ItemIds.barbed_wire_poison, 8, rows("BBB", "BIB", "BBB"),
                "B", ItemIds.barbed_wire, "I", ItemIds.powder_poison);
        shaped(NS, "barbed_wire_ultradeath", ItemIds.barbed_wire_ultradeath, 4, rows("BCB", "CIC", "BCB"),
                "B", ItemIds.barbed_wire, "C", ItemIds.powder_yellowcake, "I", ItemIds.nuclear_waste);
        shaped(NS, "barbed_wire_wither", ItemIds.barbed_wire_wither, 8, rows("BBB", "BIB", "BBB"),
                "B", ItemIds.barbed_wire, "I", VanillaIds.wither_skeleton_skull);
        shaped(NS, "barrel_antimatter", ItemIds.barrel_antimatter, rows("IPI", "I I", "IPI"),
                "I", TagIds.plates_bismuth, "P", TagIds.wires_dense_bscco);
        shaped(NS, "barrel_plastic", ItemIds.barrel_plastic, rows("IPI", "I I", "IPI"),
                "I", ItemIds.plate_polymer, "P", TagIds.plates_aluminium);
        shaped(NS, "barrel_steel", ItemIds.barrel_steel, rows("IPI", "I I", "IPI"),
                "I", TagIds.plates_steel, "P", TagIds.ingots_steel);
        shaped(NS, "barrel_tcalloy", ItemIds.barrel_tcalloy, rows("IPI", "I I", "IPI"),
                "I", TagIds.ingots_tcalloy, "P", TagIds.plates_titanium);
        shaped(NS, "basalt_brick", ItemIds.basalt_brick, 4, rows("CC", "CC"), "C", ItemIds.basalt_polished);
        shaped(NS, "basalt_polished", ItemIds.basalt_polished, 4, rows("CC", "CC"), "C", ItemIds.basalt_smooth);
        shaped(NS, "basalt_tiles", ItemIds.basalt_tiles, 4, rows("CC", "CC"), "C", ItemIds.basalt_brick);
        shaped(NS, "battery_redstone", ItemIds.battery_redstone, rows("IRI", "PRP", "IRI"),
                "I", TagIds.plates_iron, "R", TagIds.storage_blocks_redstone, "P", ItemIds.plate_polymer);
        shaped(NS, "battery_spark", ItemIds.battery_spark, rows(" W ", "DSD", "DSD"),
                "W", TagIds.wires_dense_magnetized_tungsten, "D", ItemIds.plate_dineutronium,
                "S", ItemIds.powder_spark_mix);
        shaped(NS, "battery_trixite", ItemIds.battery_trixite, rows(" W ", "DSD", "DTD"),
                "W", TagIds.wires_dense_magnetized_tungsten, "D", TagIds.plates_cast_saturnite,
                "S", ItemIds.powder_power, "T", ItemIds.crystal_trixite);
        shaped(NS, "battery_trixite_1", ItemIds.battery_trixite, rows(" W ", "DTD", "DSD"),
                "W", TagIds.wires_dense_magnetized_tungsten, "D", TagIds.plates_cast_saturnite,
                "S", ItemIds.powder_power, "T", ItemIds.crystal_trixite);
        shaped(NS, "billet_actinium", ItemIds.billet_actinium, rows("###", "###"), "#", ItemIds.nugget_actinium);
        shaped(NS, "billet_actinium_1", ItemIds.billet_actinium, rows("###", "###"), "#", ItemIds.ingot_actinium);
        shaped(NS, "billet_am241", ItemIds.billet_am241, rows("###", "###"), "#", ItemIds.nugget_am241);
        shaped(NS, "billet_am241_1", ItemIds.billet_am241, rows("###", "###"), "#", ItemIds.ingot_am241);
        shaped(NS, "billet_am242", ItemIds.billet_am242, rows("###", "###"), "#", ItemIds.nugget_am242);
        shaped(NS, "billet_am242_1", ItemIds.billet_am242, rows("###", "###"), "#", ItemIds.ingot_am242);
        shaped(NS, "billet_am_mix", ItemIds.billet_am_mix, rows("###", "###"), "#", ItemIds.nugget_am_mix);
        shaped(NS, "billet_am_mix_1", ItemIds.billet_am_mix, rows("###", "###"), "#", ItemIds.ingot_am_mix);
        shaped(NS, "billet_au198", ItemIds.billet_au198, rows("###", "###"), "#", ItemIds.nugget_au198);
        shaped(NS, "billet_au198_1", ItemIds.billet_au198, rows("###", "###"), "#", ItemIds.ingot_au198);
        shaped(NS, "billet_australium", ItemIds.billet_australium, rows("###", "###"), "#", ItemIds.nugget_australium);
        shaped(NS, "billet_australium_1", ItemIds.billet_australium, rows("###", "###"), "#", ItemIds.ingot_australium);
        shaped(NS, "billet_australium_greater", ItemIds.billet_australium_greater, rows("###", "###"),
                "#", ItemIds.nugget_australium_greater);
        shaped(NS, "billet_australium_lesser", ItemIds.billet_australium_lesser, rows("###", "###"),
                "#", ItemIds.nugget_australium_lesser);
        shaped(NS, "billet_beryllium", ItemIds.billet_beryllium, rows("###", "###"), "#", ItemIds.nugget_beryllium);
        shaped(NS, "billet_beryllium_1", ItemIds.billet_beryllium, rows("###", "###"), "#", TagIds.nuggets_beryllium);
        shaped(NS, "billet_beryllium_2", ItemIds.billet_beryllium, rows("###", "###"), "#", ItemIds.ingot_beryllium);
        shaped(NS, "billet_bismuth", ItemIds.billet_bismuth, rows("###", "###"), "#", ItemIds.nugget_bismuth);
        shaped(NS, "billet_bismuth_1", ItemIds.billet_bismuth, rows("###", "###"), "#", ItemIds.ingot_bismuth);
        shaped(NS, "billet_co60", ItemIds.billet_co60, rows("###", "###"), "#", ItemIds.nugget_co60);
        shaped(NS, "billet_co60_1", ItemIds.billet_co60, rows("###", "###"), "#", ItemIds.ingot_co60);
        shaped(NS, "billet_cobalt", ItemIds.billet_cobalt, rows("###", "###"), "#", ItemIds.nugget_cobalt);
        shaped(NS, "billet_cobalt_1", ItemIds.billet_cobalt, rows("###", "###"), "#", ItemIds.ingot_cobalt);
        shaped(NS, "billet_gh336", ItemIds.billet_gh336, rows("###", "###"), "#", ItemIds.nugget_gh336);
        shaped(NS, "billet_gh336_1", ItemIds.billet_gh336, rows("###", "###"), "#", ItemIds.ingot_gh336);
        shaped(NS, "billet_hes", ItemIds.billet_hes, rows("###", "###"), "#", ItemIds.nugget_hes);
        shaped(NS, "billet_hes_1", ItemIds.billet_hes, rows("###", "###"), "#", ItemIds.ingot_hes);
        shaped(NS, "billet_les", ItemIds.billet_les, rows("###", "###"), "#", ItemIds.nugget_les);
        shaped(NS, "billet_les_1", ItemIds.billet_les, rows("###", "###"), "#", ItemIds.ingot_les);
        shaped(NS, "billet_mox_fuel", ItemIds.billet_mox_fuel, rows("###", "###"), "#", ItemIds.nugget_mox_fuel);
        shaped(NS, "billet_mox_fuel_1", ItemIds.billet_mox_fuel, rows("###", "###"), "#", ItemIds.ingot_mox_fuel);
        shaped(NS, "billet_neptunium", ItemIds.billet_neptunium, rows("###", "###"), "#", ItemIds.nugget_neptunium);
        shaped(NS, "billet_neptunium_1", ItemIds.billet_neptunium, rows("###", "###"), "#", ItemIds.ingot_neptunium);
        shaped(NS, "billet_neptunium_fuel", ItemIds.billet_neptunium_fuel, rows("###", "###"),
                "#", ItemIds.nugget_neptunium_fuel);
        shaped(NS, "billet_neptunium_fuel_1", ItemIds.billet_neptunium_fuel, rows("###", "###"),
                "#", ItemIds.ingot_neptunium_fuel);
        shaped(NS, "billet_nuclear_waste", ItemIds.billet_nuclear_waste, rows("###", "###"),
                "#", ItemIds.nuclear_waste_tiny);
        shaped(NS, "billet_nuclear_waste_1", ItemIds.billet_nuclear_waste, rows("###", "###"),
                "#", ItemIds.nuclear_waste);
        shaped(NS, "billet_pb209", ItemIds.billet_pb209, rows("###", "###"), "#", ItemIds.nugget_pb209);
        shaped(NS, "billet_pb209_1", ItemIds.billet_pb209, rows("###", "###"), "#", ItemIds.ingot_pb209);
        shaped(NS, "billet_plutonium", ItemIds.billet_plutonium, rows("###", "###"), "#", ItemIds.nugget_plutonium);
        shaped(NS, "billet_plutonium_1", ItemIds.billet_plutonium, rows("###", "###"), "#", ItemIds.ingot_plutonium);
        shaped(NS, "billet_plutonium_fuel", ItemIds.billet_plutonium_fuel, rows("###", "###"),
                "#", ItemIds.nugget_plutonium_fuel);
        shaped(NS, "billet_plutonium_fuel_1", ItemIds.billet_plutonium_fuel, rows("###", "###"),
                "#", ItemIds.ingot_plutonium_fuel);
        shaped(NS, "billet_polonium", ItemIds.billet_polonium, rows("###", "###"), "#", ItemIds.nugget_polonium);
        shaped(NS, "billet_polonium_1", ItemIds.billet_polonium, rows("###", "###"), "#", ItemIds.ingot_polonium);
        shaped(NS, "billet_pu238", ItemIds.billet_pu238, rows("###", "###"), "#", ItemIds.nugget_pu238);
        shaped(NS, "billet_pu238_1", ItemIds.billet_pu238, rows("###", "###"), "#", ItemIds.ingot_pu238);
        shaped(NS, "billet_pu239", ItemIds.billet_pu239, rows("###", "###"), "#", ItemIds.nugget_pu239);
        shaped(NS, "billet_pu239_1", ItemIds.billet_pu239, rows("###", "###"), "#", ItemIds.ingot_pu239);
        shaped(NS, "billet_pu240", ItemIds.billet_pu240, rows("###", "###"), "#", ItemIds.nugget_pu240);
        shaped(NS, "billet_pu240_1", ItemIds.billet_pu240, rows("###", "###"), "#", ItemIds.ingot_pu240);
    }

    private static void shaped1() {
        shaped(NS, "billet_pu241", ItemIds.billet_pu241, rows("###", "###"), "#", ItemIds.nugget_pu241);
        shaped(NS, "billet_pu241_1", ItemIds.billet_pu241, rows("###", "###"), "#", ItemIds.ingot_pu241);
        shaped(NS, "billet_pu_mix", ItemIds.billet_pu_mix, rows("###", "###"), "#", ItemIds.nugget_pu_mix);
        shaped(NS, "billet_pu_mix_1", ItemIds.billet_pu_mix, rows("###", "###"), "#", ItemIds.ingot_pu_mix);
        shaped(NS, "billet_ra226", ItemIds.billet_ra226, rows("###", "###"), "#", ItemIds.nugget_ra226);
        shaped(NS, "billet_ra226_1", ItemIds.billet_ra226, rows("###", "###"), "#", ItemIds.ingot_ra226);
        shaped(NS, "billet_schrabidium", ItemIds.billet_schrabidium, rows("###", "###"),
                "#", ItemIds.nugget_schrabidium);
        shaped(NS, "billet_schrabidium_1", ItemIds.billet_schrabidium, rows("###", "###"),
                "#", TagIds.nuggets_schrabidium);
        shaped(NS, "billet_schrabidium_2", ItemIds.billet_schrabidium, rows("###", "###"),
                "#", ItemIds.ingot_schrabidium);
        shaped(NS, "billet_schrabidium_fuel", ItemIds.billet_schrabidium_fuel, rows("###", "###"),
                "#", ItemIds.nugget_schrabidium_fuel);
        shaped(NS, "billet_schrabidium_fuel_1", ItemIds.billet_schrabidium_fuel, rows("###", "###"),
                "#", ItemIds.ingot_schrabidium_fuel);
        shaped(NS, "billet_silicon", ItemIds.billet_silicon, rows("###", "###"), "#", ItemIds.nugget_silicon);
        shaped(NS, "billet_silicon_1", ItemIds.billet_silicon, rows("###", "###"), "#", TagIds.nuggets_silicon);
        shaped(NS, "billet_silicon_2", ItemIds.billet_silicon, rows("###", "###"), "#", ItemIds.ingot_silicon);
        shaped(NS, "billet_solinium", ItemIds.billet_solinium, rows("###", "###"), "#", ItemIds.nugget_solinium);
        shaped(NS, "billet_solinium_1", ItemIds.billet_solinium, rows("###", "###"), "#", TagIds.nuggets_solinium);
        shaped(NS, "billet_solinium_2", ItemIds.billet_solinium, rows("###", "###"), "#", ItemIds.ingot_solinium);
        shaped(NS, "billet_sr90", ItemIds.billet_sr90, rows("###", "###"), "#", ItemIds.nugget_sr90);
        shaped(NS, "billet_sr90_1", ItemIds.billet_sr90, rows("###", "###"), "#", ItemIds.ingot_sr90);
        shaped(NS, "billet_technetium", ItemIds.billet_technetium, rows("###", "###"), "#", ItemIds.nugget_technetium);
        shaped(NS, "billet_technetium_1", ItemIds.billet_technetium, rows("###", "###"), "#", ItemIds.ingot_technetium);
        shaped(NS, "billet_th232", ItemIds.billet_th232, rows("###", "###"), "#", ItemIds.nugget_th232);
        shaped(NS, "billet_th232_1", ItemIds.billet_th232, rows("###", "###"), "#", ItemIds.ingot_th232);
        shaped(NS, "billet_thorium_fuel", ItemIds.billet_thorium_fuel, rows("###", "###"),
                "#", ItemIds.nugget_thorium_fuel);
        shaped(NS, "billet_thorium_fuel_1", ItemIds.billet_thorium_fuel, rows("###", "###"),
                "#", ItemIds.ingot_thorium_fuel);
        shaped(NS, "billet_u233", ItemIds.billet_u233, rows("###", "###"), "#", ItemIds.nugget_u233);
        shaped(NS, "billet_u233_1", ItemIds.billet_u233, rows("###", "###"), "#", ItemIds.ingot_u233);
        shaped(NS, "billet_u235", ItemIds.billet_u235, rows("###", "###"), "#", ItemIds.nugget_u235);
        shaped(NS, "billet_u235_1", ItemIds.billet_u235, rows("###", "###"), "#", ItemIds.ingot_u235);
        shaped(NS, "billet_u238", ItemIds.billet_u238, rows("###", "###"), "#", ItemIds.nugget_u238);
        shaped(NS, "billet_u238_1", ItemIds.billet_u238, rows("###", "###"), "#", ItemIds.ingot_u238);
        shaped(NS, "billet_uranium", ItemIds.billet_uranium, rows("###", "###"), "#", ItemIds.nugget_uranium);
        shaped(NS, "billet_uranium_1", ItemIds.billet_uranium, rows("###", "###"), "#", ItemIds.ingot_uranium);
        shaped(NS, "billet_uranium_fuel", ItemIds.billet_uranium_fuel, rows("###", "###"),
                "#", ItemIds.nugget_uranium_fuel);
        shaped(NS, "billet_uranium_fuel_1", ItemIds.billet_uranium_fuel, rows("###", "###"),
                "#", ItemIds.ingot_uranium_fuel);
        shaped(NS, "billet_zirconium", ItemIds.billet_zirconium, rows("###", "###"), "#", ItemIds.nugget_zirconium);
        shaped(NS, "billet_zirconium_1", ItemIds.billet_zirconium, rows("###", "###"), "#", TagIds.nuggets_zirconium);
        shaped(NS, "billet_zirconium_2", ItemIds.billet_zirconium, rows("###", "###"), "#", ItemIds.ingot_zirconium);
        shaped(NS, "bj_boots", ItemIds.bj_boots, rows("N N", "BSB"),
                "N", ItemIds.plate_armor_lunar, "S", ItemIds.starmetal_boots, "B", ItemIds.block_starmetal);
        shaped(NS, "bj_helmet", ItemIds.bj_helmet, rows("SBS", " C ", " I "),
                "S", VanillaIds.string, "B", TagIds.minecraft_wool, "C", ItemIds.circuit_advanced,
                "I", TagIds.ingots_starmetal);
        shaped(NS, "bj_legs", ItemIds.bj_legs, rows("MBM", "NSN", "N N"),
                "N", ItemIds.plate_armor_lunar, "M", ItemIds.motor_desh, "S", ItemIds.starmetal_legs,
                "B", ItemIds.block_starmetal);
        shaped(NS, "bj_plate", ItemIds.bj_plate, rows("N N", "MSM", "NCN"),
                "N", ItemIds.plate_armor_lunar, "M", ItemIds.motor_desh, "S", ItemIds.starmetal_plate,
                "C", ItemIds.circuit_advanced);
        shaped(NS, "black_hole", ItemIds.black_hole, rows("SSS", "SCS", "SSS"),
                "C", ItemIds.singularity, "S", ItemIds.crystal_xen);
        shaped(NS, "blade_titanium", ItemIds.blade_titanium, 2, rows("TP", "TP", "TT"),
                "P", TagIds.plates_titanium, "T", TagIds.ingots_titanium);
        shaped(NS, "blades_desh", ItemIds.blades_desh, rows(" P ", "PBP", " P "),
                "P", ItemIds.plate_desh, "B", ItemIds.blades_titanium);
        shaped(NS, "blades_steel", ItemIds.blades_steel, rows(" P ", "PIP", " P "),
                "P", TagIds.plates_steel, "I", TagIds.ingots_steel);
        shaped(NS, "blades_titanium", ItemIds.blades_titanium, rows(" P ", "PIP", " P "),
                "P", TagIds.plates_titanium, "I", TagIds.ingots_titanium);
        shaped(NS, "block_c4", ItemIds.block_c4, rows("III", "III", "III"), "I", ItemIds.ingot_c4);
        shaped(NS, "block_fiberglass", ItemIds.block_fiberglass, rows("###", "###", "###"),
                "#", ItemIds.ingot_fiberglass);
        shaped(NS, "block_insulator", ItemIds.block_insulator, rows("###", "###", "###"), "#", ItemIds.plate_polymer);
        shaped(NS, "block_meteor_cobble", ItemIds.block_meteor_cobble, rows("##", "##"),
                "#", ItemIds.fragment_meteorite);
        shaped(NS, "block_schrabidium_cluster", ItemIds.block_schrabidium_cluster, rows("#S#", "SXS", "#S#"),
                "#", ItemIds.ingot_schrabidium, "S", ItemIds.ingot_starmetal, "X", ItemIds.ingot_schrabidate);
        shaped(NS, "block_scrap", ItemIds.block_scrap, rows("##", "##"), "#", ItemIds.scrap);
        shaped(NS, "block_semtex", ItemIds.block_semtex, rows("III", "III", "III"), "I", ItemIds.ingot_semtex);
        shaped(NS, "bolt_dura_steel", ItemIds.bolt_dura_steel, 16, rows("#", "#"), "#", TagIds.ingots_dura_steel);
        shaped(NS, "bolt_lead", ItemIds.bolt_lead, 16, rows("#", "#"), "#", TagIds.ingots_lead);
        shaped(NS, "bolt_spike", ItemIds.bolt_spike, 2, rows("BB", "B ", "B "), "B", TagIds.bolts_steel);
        shaped(NS, "bolt_steel", ItemIds.bolt_steel, 16, rows("#", "#"), "#", TagIds.ingots_steel);
        shaped(NS, "bolt_tungsten", ItemIds.bolt_tungsten, 16, rows("#", "#"), "#", TagIds.ingots_tungsten);
        shaped(NS, "boltgun", ItemIds.boltgun, rows("DPS", " RD", " D "),
                "D", TagIds.ingots_dura_steel, "P", ItemIds.part_generic_piston_pneumatic, "R", TagIds.ingots_rubber,
                "S", TagIds.shells_steel);
        shaped(NS, "bomb_multi", ItemIds.bomb_multi, rows("AAD", "CHF", "AAD"),
                "A", TagIds.wires_aluminium, "C", ItemIds.circuit_basic, "H", TagIds.shells_aluminium,
                "F", ItemIds.fins_quad_titanium, "D", TagIds.dyes_white);
        shaped(NS, "bottle2_empty", ItemIds.bottle2_empty, 6, rows(" G ", "G G", "G G"), "G", TagIds.glass_panes);
        shaped(NS, "bottle_empty", ItemIds.bottle_empty, 6, rows(" G ", "G G", "GGG"), "G", TagIds.glass_panes);
        shaped(NS, "bottle_mercury", ItemIds.bottle_mercury, rows("###", "#B#", "###"),
                "#", ItemIds.nugget_mercury, "B", VanillaIds.glass_bottle);
        shaped(NS, "brick_asbestos_2", ItemIds.brick_asbestos, 2, rows(" A ", "ABA", " A "),
                "B", ItemIds.brick_light, "A", TagIds.ingots_asbestos);
        shaped(NS, "brick_asbestos_slab", ItemIds.brick_asbestos_slab, 6, rows("###"), "#", ItemIds.brick_asbestos);
        shaped(NS, "brick_asbestos_stairs", ItemIds.brick_asbestos_stairs, 8, rows("#  ", "## ", "###"),
                "#", ItemIds.brick_asbestos);
        shaped(NS, "brick_asbestos_stairs_1", ItemIds.brick_asbestos_stairs, 4, rows("#  ", "## ", "###"),
                "#", ItemIds.brick_asbestos_slab);
        shaped(NS, "brick_compound_slab", ItemIds.brick_compound_slab, 6, rows("###"), "#", ItemIds.brick_compound);
        shaped(NS, "brick_compound_stairs", ItemIds.brick_compound_stairs, 8, rows("#  ", "## ", "###"),
                "#", ItemIds.brick_compound);
        shaped(NS, "brick_compound_stairs_1", ItemIds.brick_compound_stairs, 4, rows("#  ", "## ", "###"),
                "#", ItemIds.brick_compound_slab);
        shaped(NS, "brick_concrete_2", ItemIds.brick_concrete, 4, rows(" C ", "CBC", " C "),
                "C", ItemIds.concrete_smooth, "B", VanillaIds.clay_ball);
        shaped(NS, "brick_concrete_3", ItemIds.brick_concrete, 4, rows(" C ", "CBC", " C "),
                "C", ItemIds.concrete, "B", VanillaIds.clay_ball);
        shaped(NS, "brick_concrete_broken_2", ItemIds.brick_concrete_broken, 6, rows(" C ", "C C", " C "),
                "C", ItemIds.brick_concrete_cracked);
        shaped(NS, "brick_concrete_broken_slab", ItemIds.brick_concrete_broken_slab, 6, rows("###"),
                "#", ItemIds.brick_concrete_broken);
        shaped(NS, "brick_concrete_broken_stairs", ItemIds.brick_concrete_broken_stairs, 8, rows("#  ", "## ", "###"),
                "#", ItemIds.brick_concrete_broken);
        shaped(NS, "brick_concrete_broken_stairs_1", ItemIds.brick_concrete_broken_stairs, 4, rows("#  ", "## ", "###"),
                "#", ItemIds.brick_concrete_broken_slab);
        shaped(NS, "brick_concrete_cracked_2", ItemIds.brick_concrete_cracked, 6, rows(" C ", "C C", " C "),
                "C", ItemIds.brick_concrete);
        shaped(NS, "brick_concrete_cracked_slab", ItemIds.brick_concrete_cracked_slab, 6, rows("###"),
                "#", ItemIds.brick_concrete_cracked);
        shaped(NS, "brick_concrete_cracked_stairs", ItemIds.brick_concrete_cracked_stairs, 8, rows("#  ", "## ", "###"),
                "#", ItemIds.brick_concrete_cracked);
        shaped(NS, "brick_concrete_cracked_stairs_1", ItemIds.brick_concrete_cracked_stairs, 4,
                rows("#  ", "## ", "###"),
                "#", ItemIds.brick_concrete_cracked_slab);
        shaped(NS, "brick_concrete_mossy_2", ItemIds.brick_concrete_mossy, 8, rows("CCC", "CVC", "CCC"),
                "C", ItemIds.brick_concrete, "V", VanillaIds.vine);
        shaped(NS, "brick_concrete_mossy_slab", ItemIds.brick_concrete_mossy_slab, 6, rows("###"),
                "#", ItemIds.brick_concrete_mossy);
        shaped(NS, "brick_concrete_mossy_stairs", ItemIds.brick_concrete_mossy_stairs, 8, rows("#  ", "## ", "###"),
                "#", ItemIds.brick_concrete_mossy);
        shaped(NS, "brick_concrete_mossy_stairs_1", ItemIds.brick_concrete_mossy_stairs, 4, rows("#  ", "## ", "###"),
                "#", ItemIds.brick_concrete_mossy_slab);
        shaped(NS, "brick_concrete_slab", ItemIds.brick_concrete_slab, 6, rows("###"), "#", ItemIds.brick_concrete);
        shaped(NS, "brick_concrete_stairs", ItemIds.brick_concrete_stairs, 8, rows("#  ", "## ", "###"),
                "#", ItemIds.brick_concrete);
        shaped(NS, "brick_concrete_stairs_1", ItemIds.brick_concrete_stairs, 4, rows("#  ", "## ", "###"),
                "#", ItemIds.brick_concrete_slab);
        shaped(NS, "brick_ducrete", ItemIds.brick_ducrete, 4, rows("CDC", "DLD", "CDC"),
                "D", ItemIds.ducrete_smooth, "C", VanillaIds.clay_ball, "L", TagIds.plates_lead);
        shaped(NS, "brick_ducrete_1", ItemIds.brick_ducrete, 4, rows("CDC", "DLD", "CDC"),
                "D", ItemIds.ducrete, "C", VanillaIds.clay_ball, "L", TagIds.plates_lead);
        shaped(NS, "brick_ducrete_stairs", ItemIds.brick_ducrete_stairs, 4, rows("C  ", "CC ", "CCC"),
                "C", ItemIds.brick_ducrete);
        shaped(NS, "brick_fire_2", ItemIds.brick_fire, rows("BB", "BB"), "B", ItemIds.ingot_firebrick);
        shaped(NS, "brick_fire_slab", ItemIds.brick_fire_slab, 6, rows("###"), "#", ItemIds.brick_fire);
        shaped(NS, "brick_fire_stairs", ItemIds.brick_fire_stairs, 8, rows("#  ", "## ", "###"),
                "#", ItemIds.brick_fire);
        shaped(NS, "brick_fire_stairs_1", ItemIds.brick_fire_stairs, 4, rows("#  ", "## ", "###"),
                "#", ItemIds.brick_fire_slab);
        shaped(NS, "brick_light_slab", ItemIds.brick_light_slab, 6, rows("###"), "#", ItemIds.brick_light);
        shaped(NS, "brick_light_stairs", ItemIds.brick_light_stairs, 8, rows("#  ", "## ", "###"),
                "#", ItemIds.brick_light);
        shaped(NS, "brick_light_stairs_1", ItemIds.brick_light_stairs, 4, rows("#  ", "## ", "###"),
                "#", ItemIds.brick_light_slab);
        shaped(NS, "brick_obsidian_2", ItemIds.brick_obsidian, 4, rows("FBF", "BFB", "FBF"),
                "F", VanillaIds.iron_bars, "B", VanillaIds.obsidian);
        shaped(NS, "brick_obsidian_slab", ItemIds.brick_obsidian_slab, 6, rows("###"), "#", ItemIds.brick_obsidian);
        shaped(NS, "brick_obsidian_stairs", ItemIds.brick_obsidian_stairs, 8, rows("#  ", "## ", "###"),
                "#", ItemIds.brick_obsidian);
        shaped(NS, "brick_obsidian_stairs_1", ItemIds.brick_obsidian_stairs, 4, rows("#  ", "## ", "###"),
                "#", ItemIds.brick_obsidian_slab);
        shaped(NS, "bucket", VanillaIds.bucket, rows("S S", " S "), "S", TagIds.ingots_steel);
        shaped(NS, "c4", ItemIds.c4, rows("DDD", "DSD", "DDD"), "D", ItemIds.stick_c4, "S", ItemIds.safety_fuse);
        shaped(NS, "cable_detector", ItemIds.cable_detector, rows("S", "W"),
                "S", TagIds.dusts_redstone, "W", "hbm:red_wire_coated"); // 1.7: the coated wire
        shaped(NS, "cable_diode", ItemIds.cable_diode, rows(" Q ", "CAC", " Q "),
                "Q", TagIds.nuggets_silicon, "C", ItemIds.red_cable, "A", TagIds.ingots_aluminium);
        shaped(NS, "cable_switch", ItemIds.cable_switch, rows("S", "W"), "S", VanillaIds.lever, "W", "hbm:red_wire_coated"); // 1.7: the coated wire
        shaped(NS, "can_empty", ItemIds.can_empty, rows("P", "P"), "P", TagIds.plates_aluminium);
        shaped(NS, "canister_empty", ItemIds.canister_empty, 2, rows("S ", "AA", "AA"),
                "S", TagIds.plates_steel, "A", TagIds.plates_aluminium);
        shaped(NS, "capacitor_copper", ItemIds.capacitor_copper, rows("IRI", "PRP", "IRI"),
                "I", TagIds.plates_steel, "R", TagIds.storage_blocks_copper, "P", ItemIds.plate_polymer);
    }

    private static void shaped2() {
        shaped(NS, "casing_buckshot", ItemIds.casing_buckshot, 2, rows("P", "C"),
                "P", TagIds.ingots_any_plastic, "C", ItemIds.casing_large);
        shaped(NS, "catalytic_converter", ItemIds.catalytic_converter, rows("PCP", "PBP", "PCP"),
                "P", TagIds.ingots_any_hardplastic, "C", TagIds.dusts_cobalt, "B", TagIds.ingots_any_bismoid);
        shaped(NS, "cell", ItemIds.cell, 6, rows(" S ", "G G", " S "),
                "S", TagIds.plates_steel, "G", TagIds.glass_panes);
        shaped(NS, "charge_miner", ItemIds.charge_miner, rows(" F ", "FCF", " F "),
                "F", VanillaIds.flint, "C", ItemIds.charge_dynamite);
        shaped(NS, "circuit_capacitor", ItemIds.circuit_capacitor, rows("I", "N", "W"),
                "I", ItemIds.plate_polymer, "N", TagIds.nuggets_niobium, "W", TagIds.wires_aluminium);
        shaped(NS, "circuit_capacitor_1", ItemIds.circuit_capacitor, rows("I", "N", "W"),
                "I", ItemIds.plate_polymer, "N", TagIds.nuggets_niobium, "W", TagIds.wires_copper);
        shaped(NS, "circuit_capacitor_2", ItemIds.circuit_capacitor, 2, rows("IAI", "W W"),
                "I", ItemIds.plate_polymer, "A", TagIds.dusts_aluminium, "W", TagIds.wires_aluminium);
        shaped(NS, "circuit_capacitor_3", ItemIds.circuit_capacitor, 2, rows("IAI", "W W"),
                "I", ItemIds.plate_polymer, "A", TagIds.dusts_aluminium, "W", TagIds.wires_copper);
        shaped(NS, "circuit_capacitor_tantalium", ItemIds.circuit_capacitor_tantalium, rows("I", "N", "W"),
                "I", ItemIds.plate_polymer, "N", TagIds.nuggets_tantalium, "W", TagIds.wires_aluminium);
        shaped(NS, "circuit_capacitor_tantalium_1", ItemIds.circuit_capacitor_tantalium, rows("I", "N", "W"),
                "I", ItemIds.plate_polymer, "N", TagIds.nuggets_tantalium, "W", TagIds.wires_copper);
        shaped(NS, "circuit_controller_chassis", ItemIds.circuit_controller_chassis, rows("PPP", "CBB", "PPP"),
                "P", TagIds.ingots_any_plastic, "C", ItemIds.crt_display, "B", ItemIds.circuit_pcb);
        shaped(NS, "circuit_numitron", ItemIds.circuit_numitron, 3, rows("G", "W", "I"),
                "G", TagIds.glass_panes, "W", ItemIds.coil_tungsten, "I", TagIds.plates_copper);
        shaped(NS, "circuit_pcb", ItemIds.circuit_pcb, rows("I", "P"),
                "I", ItemIds.plate_polymer, "P", TagIds.plates_copper);
        shaped(NS, "circuit_pcb_1", ItemIds.circuit_pcb, 4, rows("I", "P"),
                "I", ItemIds.plate_polymer, "P", TagIds.plates_gold);
        shaped(NS, "circuit_vacuum_tube", ItemIds.circuit_vacuum_tube, rows("G", "W", "I"),
                "G", TagIds.glass_panes, "W", TagIds.wires_tungsten, "I", ItemIds.plate_polymer);
        shaped(NS, "circuit_vacuum_tube_1", ItemIds.circuit_vacuum_tube, rows("G", "W", "I"),
                "G", TagIds.glass_panes, "W", TagIds.wires_carbon, "I", ItemIds.plate_polymer);
        shaped(NS, "cladding_desh", ItemIds.cladding_desh, rows("DPD", "PRP", "DPD"),
                "R", ItemIds.cladding_lead, "P", ItemIds.plate_desh, "D", ItemIds.ducttape);
        shaped(NS, "cladding_ghiorsium", ItemIds.cladding_ghiorsium, rows("DPD", "PRP", "DPD"),
                "R", ItemIds.cladding_desh, "P", ItemIds.ingot_gh336, "D", ItemIds.ducttape);
        shaped(NS, "cladding_lead", ItemIds.cladding_lead, rows("DPD", "PRP", "DPD"),
                "R", ItemIds.cladding_rubber, "P", TagIds.plates_lead, "D", ItemIds.ducttape);
        shaped(NS, "cladding_rubber", ItemIds.cladding_rubber, rows("RCR", "CDC", "RCR"),
                "R", TagIds.ingots_any_rubber, "C", TagIds.dusts_coal, "D", ItemIds.ducttape);
        shaped(NS, "cmb_brick_reinforced_slab", ItemIds.cmb_brick_reinforced_slab, 6, rows("###"),
                "#", ItemIds.cmb_brick_reinforced);
        shaped(NS, "cmb_brick_reinforced_stairs", ItemIds.cmb_brick_reinforced_stairs, 8, rows("#  ", "## ", "###"),
                "#", ItemIds.cmb_brick_reinforced);
        shaped(NS, "cmb_brick_reinforced_stairs_1", ItemIds.cmb_brick_reinforced_stairs, 4, rows("#  ", "## ", "###"),
                "#", ItemIds.cmb_brick_reinforced_slab);
        shaped(NS, "cobalt_boots", ItemIds.cobalt_boots, rows("ECE"),
                "E", TagIds.billets_cobalt, "C", ItemIds.steel_boots);
        shaped(NS, "cobalt_helmet", ItemIds.cobalt_helmet, rows("ECE"),
                "E", TagIds.billets_cobalt, "C", ItemIds.steel_helmet);
        shaped(NS, "cobalt_legs", ItemIds.cobalt_legs, rows("ECE", "E E"),
                "E", TagIds.billets_cobalt, "C", ItemIds.steel_legs);
        shaped(NS, "cobalt_plate", ItemIds.cobalt_plate, rows(" E ", "ECE", " E "),
                "E", TagIds.billets_cobalt, "C", ItemIds.steel_plate);
        shaped(NS, "coil_copper", ItemIds.coil_copper, rows("WWW", "WIW", "WWW"),
                "W", TagIds.wires_red_copper, "I", TagIds.ingots_iron);
        shaped(NS, "coil_copper_1", ItemIds.coil_copper, rows("WWW", "WIW", "WWW"),
                "W", TagIds.wires_red_copper, "I", TagIds.ingots_steel);
        shaped(NS, "coil_copper_torus", ItemIds.coil_copper_torus, 2, rows(" C ", "CPC", " C "),
                "P", TagIds.plates_iron, "C", ItemIds.coil_copper);
        shaped(NS, "coil_copper_torus_1", ItemIds.coil_copper_torus, 2, rows(" C ", "CPC", " C "),
                "P", TagIds.plates_steel, "C", ItemIds.coil_copper);
        shaped(NS, "coil_gold", ItemIds.coil_gold, rows("WWW", "WIW", "WWW"),
                "W", TagIds.wires_gold, "I", TagIds.ingots_iron);
        shaped(NS, "coil_gold_1", ItemIds.coil_gold, rows("WWW", "WIW", "WWW"),
                "W", TagIds.wires_gold, "I", TagIds.ingots_steel);
        shaped(NS, "coil_gold_torus", ItemIds.coil_gold_torus, 2, rows(" C ", "CPC", " C "),
                "P", TagIds.plates_iron, "C", ItemIds.coil_gold);
        shaped(NS, "coil_gold_torus_1", ItemIds.coil_gold_torus, 2, rows(" C ", "CPC", " C "),
                "P", TagIds.plates_steel, "C", ItemIds.coil_gold);
        shaped(NS, "coil_magnetized_tungsten", ItemIds.coil_magnetized_tungsten, rows("WWW", "WIW", "WWW"),
                "W", TagIds.wires_magnetized_tungsten, "I", TagIds.ingots_iron);
        shaped(NS, "coil_magnetized_tungsten_1", ItemIds.coil_magnetized_tungsten, rows("WWW", "WIW", "WWW"),
                "W", TagIds.wires_magnetized_tungsten, "I", TagIds.ingots_steel);
        shaped(NS, "coil_tungsten", ItemIds.coil_tungsten, rows("WWW", "WIW", "WWW"),
                "W", TagIds.wires_tungsten, "I", TagIds.ingots_iron);
        shaped(NS, "coil_tungsten_1", ItemIds.coil_tungsten, rows("WWW", "WIW", "WWW"),
                "W", TagIds.wires_tungsten, "I", TagIds.ingots_steel);
        shaped(NS, "concrete_2", ItemIds.concrete, 4, rows("CC", "CC"), "C", ItemIds.concrete_smooth);
        shaped(NS, "concrete_asbestos_slab", ItemIds.concrete_asbestos_slab, 6, rows("###"),
                "#", ItemIds.concrete_asbestos);
        shaped(NS, "concrete_asbestos_stairs", ItemIds.concrete_asbestos_stairs, 8, rows("#  ", "## ", "###"),
                "#", ItemIds.concrete_asbestos);
        shaped(NS, "concrete_asbestos_stairs_1", ItemIds.concrete_asbestos_stairs, 4, rows("#  ", "## ", "###"),
                "#", ItemIds.concrete_asbestos_slab);
        shaped(NS, "concrete_colored_black", stack(ItemIds.concrete_colored, 8).with(ComponentIds.item_variant, 15),
                rows("CCC", "CDC", "CCC"),
                "C", ItemIds.concrete_smooth, "D", VanillaIds.black_dye);
        shaped(NS, "concrete_colored_blue", stack(ItemIds.concrete_colored, 8).with(ComponentIds.item_variant, 11),
                rows("CCC", "CDC", "CCC"),
                "C", ItemIds.concrete_smooth, "D", VanillaIds.blue_dye);
        shaped(NS, "concrete_colored_brown", stack(ItemIds.concrete_colored, 8).with(ComponentIds.item_variant, 12),
                rows("CCC", "CDC", "CCC"),
                "C", ItemIds.concrete_smooth, "D", VanillaIds.brown_dye);
        shaped(NS, "concrete_colored_cyan", stack(ItemIds.concrete_colored, 8).with(ComponentIds.item_variant, 9),
                rows("CCC", "CDC", "CCC"),
                "C", ItemIds.concrete_smooth, "D", VanillaIds.cyan_dye);
        shaped(NS, "concrete_colored_ext_bronze",
                stack(ItemIds.concrete_colored_ext, 6).with(ComponentIds.item_variant, 7), rows("CCC", "1 2", "CCC"),
                "C", ItemIds.concrete_smooth, "1", VanillaIds.orange_dye, "2", VanillaIds.brown_dye);
        shaped(NS, "concrete_colored_ext_hazard",
                stack(ItemIds.concrete_colored_ext, 6).with(ComponentIds.item_variant, 5), rows("CCC", "1 2", "CCC"),
                "C", ItemIds.concrete_smooth, "1", VanillaIds.yellow_dye, "2", VanillaIds.black_dye);
        shaped(NS, "concrete_colored_ext_indigo",
                stack(ItemIds.concrete_colored_ext, 6).with(ComponentIds.item_variant, 2), rows("CCC", "1 2", "CCC"),
                "C", ItemIds.concrete_smooth, "1", VanillaIds.blue_dye, "2", VanillaIds.purple_dye);
        shaped(NS, "concrete_colored_ext_machine",
                stack(ItemIds.concrete_colored_ext, 6).with(ComponentIds.item_variant, 0), rows("CCC", "1 2", "CCC"),
                "C", ItemIds.concrete_smooth, "1", VanillaIds.brown_dye, "2", VanillaIds.gray_dye);
        shaped(NS, "concrete_colored_ext_machine_stripe",
                stack(ItemIds.concrete_colored_ext, 6).with(ComponentIds.item_variant, 1), rows("CCC", "1 2", "CCC"),
                "C", ItemIds.concrete_smooth, "1", VanillaIds.brown_dye, "2", VanillaIds.black_dye);
        shaped(NS, "concrete_colored_ext_pink",
                stack(ItemIds.concrete_colored_ext, 6).with(ComponentIds.item_variant, 4), rows("CCC", "1 2", "CCC"),
                "C", ItemIds.concrete_smooth, "1", VanillaIds.pink_dye, "2", VanillaIds.red_dye);
        shaped(NS, "concrete_colored_ext_purple",
                stack(ItemIds.concrete_colored_ext, 6).with(ComponentIds.item_variant, 3), rows("CCC", "1 2", "CCC"),
                "C", ItemIds.concrete_smooth, "1", VanillaIds.purple_dye, "2", VanillaIds.purple_dye);
        shaped(NS, "concrete_colored_ext_sand",
                stack(ItemIds.concrete_colored_ext, 6).with(ComponentIds.item_variant, 6), rows("CCC", "1 2", "CCC"),
                "C", ItemIds.concrete_smooth, "1", VanillaIds.yellow_dye, "2", VanillaIds.gray_dye);
        shaped(NS, "concrete_colored_gray", stack(ItemIds.concrete_colored, 8).with(ComponentIds.item_variant, 7),
                rows("CCC", "CDC", "CCC"),
                "C", ItemIds.concrete_smooth, "D", VanillaIds.gray_dye);
        shaped(NS, "concrete_colored_green", stack(ItemIds.concrete_colored, 8).with(ComponentIds.item_variant, 13),
                rows("CCC", "CDC", "CCC"),
                "C", ItemIds.concrete_smooth, "D", VanillaIds.green_dye);
        shaped(NS, "concrete_colored_light_blue", stack(ItemIds.concrete_colored, 8).with(ComponentIds.item_variant, 3),
                rows("CCC", "CDC", "CCC"),
                "C", ItemIds.concrete_smooth, "D", VanillaIds.light_blue_dye);
        shaped(NS, "concrete_colored_lime", stack(ItemIds.concrete_colored, 8).with(ComponentIds.item_variant, 5),
                rows("CCC", "CDC", "CCC"),
                "C", ItemIds.concrete_smooth, "D", VanillaIds.lime_dye);
        shaped(NS, "concrete_colored_magenta", stack(ItemIds.concrete_colored, 8).with(ComponentIds.item_variant, 2),
                rows("CCC", "CDC", "CCC"),
                "C", ItemIds.concrete_smooth, "D", VanillaIds.magenta_dye);
        shaped(NS, "concrete_colored_orange", stack(ItemIds.concrete_colored, 8).with(ComponentIds.item_variant, 1),
                rows("CCC", "CDC", "CCC"),
                "C", ItemIds.concrete_smooth, "D", VanillaIds.orange_dye);
        shaped(NS, "concrete_colored_pink", stack(ItemIds.concrete_colored, 8).with(ComponentIds.item_variant, 6),
                rows("CCC", "CDC", "CCC"),
                "C", ItemIds.concrete_smooth, "D", VanillaIds.pink_dye);
        shaped(NS, "concrete_colored_purple", stack(ItemIds.concrete_colored, 8).with(ComponentIds.item_variant, 10),
                rows("CCC", "CDC", "CCC"),
                "C", ItemIds.concrete_smooth, "D", VanillaIds.purple_dye);
        shaped(NS, "concrete_colored_red", stack(ItemIds.concrete_colored, 8).with(ComponentIds.item_variant, 14),
                rows("CCC", "CDC", "CCC"),
                "C", ItemIds.concrete_smooth, "D", VanillaIds.red_dye);
        shaped(NS, "concrete_colored_silver", stack(ItemIds.concrete_colored, 8).with(ComponentIds.item_variant, 8),
                rows("CCC", "CDC", "CCC"),
                "C", ItemIds.concrete_smooth, "D", VanillaIds.light_gray_dye);
        shaped(NS, "concrete_colored_white", stack(ItemIds.concrete_colored, 8).with(ComponentIds.item_variant, 0),
                rows("CCC", "CDC", "CCC"),
                "C", ItemIds.concrete_smooth, "D", VanillaIds.white_dye);
        shaped(NS, "concrete_colored_yellow", stack(ItemIds.concrete_colored, 8).with(ComponentIds.item_variant, 4),
                rows("CCC", "CDC", "CCC"),
                "C", ItemIds.concrete_smooth, "D", VanillaIds.yellow_dye);
        shaped(NS, "concrete_pillar", ItemIds.concrete_pillar, 6, rows("CBC", "CBC", "CBC"),
                "C", ItemIds.concrete_smooth, "B", VanillaIds.iron_bars);
        shaped(NS, "concrete_slab", ItemIds.concrete_slab, 6, rows("###"), "#", ItemIds.concrete);
        shaped(NS, "concrete_smooth_slab", ItemIds.concrete_smooth_slab, 6, rows("###"), "#", ItemIds.concrete_smooth);
        shaped(NS, "concrete_smooth_stairs", ItemIds.concrete_smooth_stairs, 8, rows("#  ", "## ", "###"),
                "#", ItemIds.concrete_smooth);
        shaped(NS, "concrete_smooth_stairs_1", ItemIds.concrete_smooth_stairs, 4, rows("#  ", "## ", "###"),
                "#", ItemIds.concrete_smooth_slab);
        shaped(NS, "concrete_stairs", ItemIds.concrete_stairs, 8, rows("#  ", "## ", "###"), "#", ItemIds.concrete);
        shaped(NS, "concrete_stairs_1", ItemIds.concrete_stairs, 4, rows("#  ", "## ", "###"),
                "#", ItemIds.concrete_slab);
        shaped(NS, "conveyor_wand_double", ItemIds.conveyor_wand_double, rows("CPC"),
                "C", ItemIds.conveyor_wand_regular, "P", TagIds.plates_iron);
        shaped(NS, "conveyor_wand_regular", ItemIds.conveyor_wand_regular, 16, rows("LLL", "I I", "LLL"),
                "L", VanillaIds.leather, "I", TagIds.ingots_iron);
        shaped(NS, "conveyor_wand_regular_rubber", ItemIds.conveyor_wand_regular, 64, rows("LLL", "I I", "LLL"),
                "L", TagIds.ingots_any_rubber, "I", TagIds.ingots_iron);
        shaped(NS, "conveyor_wand_triple", ItemIds.conveyor_wand_triple, rows("DPC"),
                "C", ItemIds.conveyor_wand_regular, "D", ItemIds.conveyor_wand_double, "P", TagIds.plates_steel);
        shaped(NS, "crane_boxer", ItemIds.crane_boxer, rows("WWW", "WPW", "CCC"),
                "W", TagIds.minecraft_planks, "P", ItemIds.part_generic_piston_pneumatic,
                "C", ItemIds.conveyor_wand_regular);
        shaped(NS, "crane_extractor_iron", ItemIds.crane_extractor, 2, rows("CCC", "CPC", "CBC"),
                "C", TagIds.ingots_iron, "B", ItemIds.conveyor_wand_regular, "P", ItemIds.part_generic_piston_pneumatic);
        shaped(NS, "crane_extractor_iron_block", ItemIds.crane_extractor, 2, rows("CCC", "CPC", "CBC"),
                "C", TagIds.ingots_iron, "B", ItemIds.conveyor, "P", ItemIds.part_generic_piston_pneumatic);
        shaped(NS, "crane_extractor_steel", ItemIds.crane_extractor, 4, rows("CCC", "CPC", "CBC"),
                "C", TagIds.ingots_steel, "B", ItemIds.conveyor_wand_regular, "P", ItemIds.part_generic_piston_pneumatic);
        shaped(NS, "crane_extractor_steel_block", ItemIds.crane_extractor, 4, rows("CCC", "CPC", "CBC"),
                "C", TagIds.ingots_steel, "B", ItemIds.conveyor, "P", ItemIds.part_generic_piston_pneumatic);
        shaped(NS, "crane_extractor_stonebrick", ItemIds.crane_extractor, rows("CCC", "CPC", "CBC"),
                "C", VanillaIds.stone_bricks, "B", ItemIds.conveyor_wand_regular,
                "P", ItemIds.part_generic_piston_pneumatic);
        shaped(NS, "crane_extractor_stonebrick_block", ItemIds.crane_extractor, rows("CCC", "CPC", "CBC"),
                "C", VanillaIds.stone_bricks, "B", ItemIds.conveyor, "P", ItemIds.part_generic_piston_pneumatic);
        shaped(NS, "crane_grabber_iron", ItemIds.crane_grabber, 2, rows("C C", "P P", "CBC"),
                "C", TagIds.ingots_iron, "B", ItemIds.conveyor_wand_regular, "P", ItemIds.part_generic_piston_pneumatic);
        shaped(NS, "crane_grabber_iron_block", ItemIds.crane_grabber, 2, rows("C C", "P P", "CBC"),
                "C", TagIds.ingots_iron, "B", ItemIds.conveyor, "P", ItemIds.part_generic_piston_pneumatic);
        shaped(NS, "crane_grabber_steel", ItemIds.crane_grabber, 4, rows("C C", "P P", "CBC"),
                "C", TagIds.ingots_steel, "B", ItemIds.conveyor_wand_regular, "P", ItemIds.part_generic_piston_pneumatic);
        shaped(NS, "crane_grabber_steel_block", ItemIds.crane_grabber, 4, rows("C C", "P P", "CBC"),
                "C", TagIds.ingots_steel, "B", ItemIds.conveyor, "P", ItemIds.part_generic_piston_pneumatic);
        shaped(NS, "crane_grabber_stonebrick", ItemIds.crane_grabber, rows("C C", "P P", "CBC"),
                "C", VanillaIds.stone_bricks, "B", ItemIds.conveyor_wand_regular,
                "P", ItemIds.part_generic_piston_pneumatic);
        shaped(NS, "crane_grabber_stonebrick_block", ItemIds.crane_grabber, rows("C C", "P P", "CBC"),
                "C", VanillaIds.stone_bricks, "B", ItemIds.conveyor, "P", ItemIds.part_generic_piston_pneumatic);
        shaped(NS, "crane_inserter_iron", ItemIds.crane_inserter, 2, rows("CCC", "C C", "CBC"),
                "C", TagIds.ingots_iron, "B", ItemIds.conveyor_wand_regular);
        shaped(NS, "crane_inserter_iron_block", ItemIds.crane_inserter, 2, rows("CCC", "C C", "CBC"),
                "C", TagIds.ingots_iron, "B", ItemIds.conveyor);
        shaped(NS, "crane_inserter_steel", ItemIds.crane_inserter, 4, rows("CCC", "C C", "CBC"),
                "C", TagIds.ingots_steel, "B", ItemIds.conveyor_wand_regular);
        shaped(NS, "crane_inserter_steel_block", ItemIds.crane_inserter, 4, rows("CCC", "C C", "CBC"),
                "C", TagIds.ingots_steel, "B", ItemIds.conveyor);
        shaped(NS, "crane_inserter_stonebrick", ItemIds.crane_inserter, rows("CCC", "C C", "CBC"),
                "C", VanillaIds.stone_bricks, "B", ItemIds.conveyor_wand_regular);
        shaped(NS, "crane_inserter_stonebrick_block", ItemIds.crane_inserter, rows("CCC", "C C", "CBC"),
                "C", VanillaIds.stone_bricks, "B", ItemIds.conveyor);
        shaped(NS, "crane_partitioner", ItemIds.crane_partitioner, rows(" M ", "BCB"),
                "M", ItemIds.circuit_chip, "B", ItemIds.conveyor_wand_regular, "C", ItemIds.crate_steel);
        shaped(NS, "crane_router", ItemIds.crane_router, rows("PIP", "ICI", "PIP"),
                "P", ItemIds.part_generic_piston_pneumatic, "I", ItemIds.plate_polymer, "C", ItemIds.circuit_basic);
        shaped(NS, "crane_splitter", ItemIds.crane_splitter, rows("III", "PCP", "III"),
                "P", ItemIds.part_generic_piston_pneumatic, "I", TagIds.ingots_steel, "C", ItemIds.circuit_vacuum_tube);
        shaped(NS, "crane_unboxer", ItemIds.crane_unboxer, rows("WWW", "WPW", "CCC"),
                "W", VanillaIds.stick, "P", VanillaIds.shears, "C", ItemIds.conveyor_wand_regular);
        shaped(NS, "crate_iron", ItemIds.crate_iron, rows("PPP", "I I", "III"),
                "P", TagIds.plates_iron, "I", TagIds.ingots_iron);
        shaped(NS, "crate_steel", ItemIds.crate_steel, rows("PPP", "I I", "III"),
                "P", TagIds.plates_steel, "I", TagIds.ingots_steel);
        shaped(NS, "crt_display", ItemIds.crt_display, 4, rows(" A ", "SGS", " T "),
                "A", TagIds.dusts_aluminium, "S", TagIds.plates_steel, "G", TagIds.glass_panes,
                "T", ItemIds.circuit_vacuum_tube);
        shaped(NS, "crystal_xen", ItemIds.crystal_xen, rows("EEE", "EIE", "EEE"),
                "E", ItemIds.powder_power, "I", TagIds.ingots_euphemium);
        shaped(NS, "custom_dirty", ItemIds.custom_dirty, rows(" C ", "WLW", "WLW"),
                "C", TagIds.plates_copper, "L", TagIds.plates_lead, "W", ItemIds.nuclear_waste);
        shaped(NS, "custom_euph", ItemIds.custom_euph, rows(" C ", "LUL", "LUL"),
                "C", TagIds.plates_copper, "L", TagIds.plates_lead, "U", TagIds.ingots_euphemium);
        shaped(NS, "custom_fall", ItemIds.custom_fall, rows("IIP", "CHW", "IIP"),
                "I", TagIds.ingots_any_rubber, "P", TagIds.plates_saturnite, "C", ItemIds.circuit_advanced,
                "H", TagIds.shells_steel, "W", ItemIds.coil_copper);
        shaped(NS, "custom_nuke", ItemIds.custom_nuke, rows(" C ", "LUL", "LUL"),
                "C", TagIds.plates_copper, "L", TagIds.plates_lead, "U", TagIds.ingots_u235);
        shaped(NS, "custom_schrab", ItemIds.custom_schrab, rows(" C ", "LUL", "LUL"),
                "C", TagIds.plates_copper, "L", TagIds.plates_lead, "U", TagIds.ingots_schrabidium);
    }

    private static void shaped3() {
        shaped(NS, "custom_sol", ItemIds.custom_sol, rows(" C ", "LUL", "LUL"),
                "C", TagIds.plates_copper, "L", TagIds.plates_lead, "U", TagIds.ingots_solinium);
        shaped(NS, "custom_tnt", ItemIds.custom_tnt, rows(" C ", "TIT", "TIT"),
                "C", TagIds.plates_copper, "I", TagIds.plates_iron, "T", TagIds.ingots_any_highexplosive);
        shaped(NS, "deco_pipe", ItemIds.deco_pipe, 6, rows("PP"), "P", TagIds.pipes_steel);
        shaped(NS, "deco_pipe_framed", ItemIds.deco_pipe_framed, 8, rows("PPP", "PCP", "PPP"),
                "P", ItemIds.deco_pipe, "C", VanillaIds.iron_bars);
        shaped(NS, "deco_pipe_framed_1", ItemIds.deco_pipe_framed, 8, rows("PPP", "PCP", "PPP"),
                "P", ItemIds.deco_pipe_rim, "C", VanillaIds.iron_bars);
        shaped(NS, "deco_pipe_framed_green", ItemIds.deco_pipe_framed_green, 8, rows("PPP", "PCP", "PPP"),
                "P", ItemIds.deco_pipe_framed, "C", VanillaIds.green_dye);
        shaped(NS, "deco_pipe_framed_green_rusted", ItemIds.deco_pipe_framed_green_rusted, 8, rows("PPP", "PCP", "PPP"),
                "P", ItemIds.deco_pipe_framed_green, "C", TagIds.dusts_iron);
        shaped(NS, "deco_pipe_framed_marked", ItemIds.deco_pipe_framed_marked, 8, rows("PPP", "PCP", "PPP"),
                "P", ItemIds.deco_pipe_framed_green, "C", VanillaIds.green_dye);
        shaped(NS, "deco_pipe_framed_red", ItemIds.deco_pipe_framed_red, 8, rows("PPP", "PCP", "PPP"),
                "P", ItemIds.deco_pipe_framed, "C", VanillaIds.red_dye);
        shaped(NS, "deco_pipe_framed_rusted", ItemIds.deco_pipe_framed_rusted, 8, rows("PPP", "PCP", "PPP"),
                "P", ItemIds.deco_pipe_framed, "C", TagIds.dusts_iron);
        shaped(NS, "deco_pipe_green", ItemIds.deco_pipe_green, 8, rows("PPP", "PCP", "PPP"),
                "P", ItemIds.deco_pipe, "C", VanillaIds.green_dye);
        shaped(NS, "deco_pipe_green_rusted", ItemIds.deco_pipe_green_rusted, 8, rows("PPP", "PCP", "PPP"),
                "P", ItemIds.deco_pipe_green, "C", TagIds.dusts_iron);
        shaped(NS, "deco_pipe_marked", ItemIds.deco_pipe_marked, 8, rows("PPP", "PCP", "PPP"),
                "P", ItemIds.deco_pipe_green, "C", VanillaIds.green_dye);
        shaped(NS, "deco_pipe_quad", ItemIds.deco_pipe_quad, 4, rows("PP", "PP"), "P", ItemIds.deco_pipe);
        shaped(NS, "deco_pipe_quad_green", ItemIds.deco_pipe_quad_green, 8, rows("PPP", "PCP", "PPP"),
                "P", ItemIds.deco_pipe_quad, "C", VanillaIds.green_dye);
        shaped(NS, "deco_pipe_quad_green_rusted", ItemIds.deco_pipe_quad_green_rusted, 8, rows("PPP", "PCP", "PPP"),
                "P", ItemIds.deco_pipe_quad_green, "C", TagIds.dusts_iron);
        shaped(NS, "deco_pipe_quad_marked", ItemIds.deco_pipe_quad_marked, 8, rows("PPP", "PCP", "PPP"),
                "P", ItemIds.deco_pipe_quad_green, "C", VanillaIds.green_dye);
        shaped(NS, "deco_pipe_quad_red", ItemIds.deco_pipe_quad_red, 8, rows("PPP", "PCP", "PPP"),
                "P", ItemIds.deco_pipe_quad, "C", VanillaIds.red_dye);
        shaped(NS, "deco_pipe_quad_rusted", ItemIds.deco_pipe_quad_rusted, 8, rows("PPP", "PCP", "PPP"),
                "P", ItemIds.deco_pipe_quad, "C", TagIds.dusts_iron);
        shaped(NS, "deco_pipe_red", ItemIds.deco_pipe_red, 8, rows("PPP", "PCP", "PPP"),
                "P", ItemIds.deco_pipe, "C", VanillaIds.red_dye);
        shaped(NS, "deco_pipe_rim", ItemIds.deco_pipe_rim, 8, rows("PPP", "PCP", "PPP"),
                "P", ItemIds.deco_pipe, "C", TagIds.plates_steel);
        shaped(NS, "deco_pipe_rim_green", ItemIds.deco_pipe_rim_green, 8, rows("PPP", "PCP", "PPP"),
                "P", ItemIds.deco_pipe_rim, "C", VanillaIds.green_dye);
        shaped(NS, "deco_pipe_rim_green_rusted", ItemIds.deco_pipe_rim_green_rusted, 8, rows("PPP", "PCP", "PPP"),
                "P", ItemIds.deco_pipe_rim_green, "C", TagIds.dusts_iron);
        shaped(NS, "deco_pipe_rim_marked", ItemIds.deco_pipe_rim_marked, 8, rows("PPP", "PCP", "PPP"),
                "P", ItemIds.deco_pipe_rim_green, "C", VanillaIds.green_dye);
        shaped(NS, "deco_pipe_rim_red", ItemIds.deco_pipe_rim_red, 8, rows("PPP", "PCP", "PPP"),
                "P", ItemIds.deco_pipe_rim, "C", VanillaIds.red_dye);
        shaped(NS, "deco_pipe_rim_rusted", ItemIds.deco_pipe_rim_rusted, 8, rows("PPP", "PCP", "PPP"),
                "P", ItemIds.deco_pipe_rim, "C", TagIds.dusts_iron);
        shaped(NS, "deco_pipe_rusted", ItemIds.deco_pipe_rusted, 8, rows("PPP", "PCP", "PPP"),
                "P", ItemIds.deco_pipe, "C", TagIds.dusts_iron);
        shaped(NS, "deco_rbmk", ItemIds.deco_rbmk, 8, rows("R"), "R", ItemIds.rbmk_blank);
        shaped(NS, "deco_rbmk_smooth", ItemIds.deco_rbmk_smooth, rows("R"), "R", ItemIds.deco_rbmk);
        shaped(NS, "defuser", ItemIds.defuser, rows(" PS", "P P", " P "),
                "P", TagIds.ingots_any_plastic, "S", TagIds.plates_steel);
        shaped(NS, "defuser_gold", ItemIds.defuser_gold, rows("GPG", "PRP", "GPG"),
                "G", VanillaIds.gunpowder, "P", TagIds.plates_gold, "R", TagIds.hbm_music_discs);
        shaped(NS, "depth_brick", ItemIds.depth_brick, 4, rows("CC", "CC"), "C", ItemIds.stone_depth);
        shaped(NS, "depth_nether_brick", ItemIds.depth_nether_brick, 4, rows("CC", "CC"),
                "C", ItemIds.stone_depth_nether);
        shaped(NS, "depth_nether_tiles", ItemIds.depth_nether_tiles, 4, rows("CC", "CC"),
                "C", ItemIds.depth_nether_brick);
        shaped(NS, "depth_tiles", ItemIds.depth_tiles, 4, rows("CC", "CC"), "C", ItemIds.depth_brick);
        shaped(NS, "designator", ItemIds.designator, rows("  A", "#B#", "#B#"),
                "#", TagIds.ingots_any_plastic, "A", TagIds.plates_steel, "B", ItemIds.circuit_basic);
        shaped(NS, "det_charge", ItemIds.det_charge, rows("PDP", "DTD", "PDP"),
                "P", TagIds.plates_steel, "D", ItemIds.det_cord, "T", TagIds.ingots_any_plasticexplosive);
        shaped(NS, "det_cord", ItemIds.det_cord, 4, rows(" P ", "PGP", " P "),
                "P", VanillaIds.paper, "G", VanillaIds.gunpowder);
        shaped(NS, "det_miner", ItemIds.det_miner, 4, rows("FFF", "ITI", "ITI"),
                "F", VanillaIds.flint, "I", TagIds.plates_iron, "T", ItemIds.ball_dynamite);
        shaped(NS, "det_miner_1", ItemIds.det_miner, 12, rows("FFF", "ITI", "ITI"),
                "F", VanillaIds.flint, "I", TagIds.plates_steel, "T", TagIds.ingots_any_plasticexplosive);
        shaped(NS, "det_nuke", ItemIds.det_nuke, rows("PFP", "DCD", "PFP"),
                "P", ItemIds.neutron_reflector, "D", ItemIds.det_charge, "C", ItemIds.man_core,
                "F", ItemIds.circuit_controller);
        shaped(NS, "detonator", ItemIds.detonator, rows("C", "S"),
                "S", TagIds.plates_steel, "C", ItemIds.circuit_basic);
        shaped(NS, "detonator_de", ItemIds.detonator_de, rows("T", "D", "T"),
                "T", VanillaIds.tnt, "D", ItemIds.detonator_deadman);
        shaped(NS, "deuterium_filter", ItemIds.deuterium_filter, rows("TST", "SCS", "TST"),
                "T", TagIds.ingots_any_resistant_alloy, "S", TagIds.dusts_sulfur, "C", ItemIds.catalyst_clay);
        shaped(NS, "disperser_canister_empty", ItemIds.disperser_canister_empty, 4, rows(" P ", "PGP", " P "),
                "P", TagIds.ingots_any_hardplastic, "G", ItemIds.glass_boron);
        shaped(NS, "dns_boots", ItemIds.dns_boots, rows("PCP", "PBP", "PSP"),
                "P", ItemIds.plate_armor_dnt, "S", ItemIds.ingot_chainsteel, "B", ItemIds.bj_boots,
                "C", ItemIds.demon_core_closed);
        shaped(NS, "dns_helmet", ItemIds.dns_helmet, rows("PCP", "PBP", "PSP"),
                "P", ItemIds.plate_armor_dnt, "S", ItemIds.ingot_chainsteel, "B", ItemIds.bj_helmet,
                "C", ItemIds.circuit_quantum);
        shaped(NS, "dns_legs", ItemIds.dns_legs, rows("PCP", "PBP", "PSP"),
                "P", ItemIds.plate_armor_dnt, "S", ItemIds.ingot_chainsteel, "B", ItemIds.bj_legs,
                "C", ItemIds.coin_worm);
        shaped(NS, "dns_plate", ItemIds.dns_plate, rows("PCP", "PBP", "PSP"),
                "P", ItemIds.plate_armor_dnt, "S", ItemIds.ingot_chainsteel, "B", ItemIds.bj_plate_jetpack,
                "C", ItemIds.singularity_spark);
        shaped(NS, "door_bunker", ItemIds.door_bunker, rows("II", "SS", "II"),
                "I", TagIds.plates_steel, "S", TagIds.plates_lead);
        shaped(NS, "door_metal", ItemIds.door_metal, rows("II", "SS", "II"),
                "I", TagIds.plates_iron, "S", TagIds.plates_steel);
        shaped(NS, "door_office", ItemIds.door_office, rows("II", "SS", "II"),
                "I", TagIds.minecraft_planks, "S", TagIds.plates_iron);
        shaped(NS, "dosimeter", ItemIds.dosimeter, rows("WGW", "WCW", "WBW"),
                "W", TagIds.minecraft_planks, "G", TagIds.glass_panes, "C", ItemIds.circuit_vacuum_tube,
                "B", TagIds.ingots_beryllium);
        shaped(NS, "drone_crate", ItemIds.drone_crate, rows("T", "C"),
                "T", ItemIds.drone_waypoint, "C", ItemIds.crate_steel);
        shaped(NS, "drone_crate_provider", ItemIds.drone_crate_provider, rows("T", "C", "B"),
                "T", ItemIds.drone_waypoint_request, "C", ItemIds.crate_steel, "B", VanillaIds.orange_dye);
        shaped(NS, "drone_crate_requester", ItemIds.drone_crate_requester, rows("T", "C", "B"),
                "T", ItemIds.drone_waypoint_request, "C", ItemIds.crate_steel, "B", VanillaIds.yellow_dye);
        shaped(NS, "drone_dock", ItemIds.drone_dock, rows("T", "C", "B"),
                "T", ItemIds.drone_waypoint_request, "C", ItemIds.crate_steel, "B", ItemIds.circuit_advanced);
        shaped(NS, "drone_linker", ItemIds.drone_linker, rows("T", "C"),
                "T", ItemIds.drone_waypoint, "C", ItemIds.circuit_basic);
        shaped(NS, "drone_waypoint", ItemIds.drone_waypoint, 4, rows("G", "T", "C"),
                "G", VanillaIds.green_dye, "T", VanillaIds.redstone_torch, "C", ItemIds.circuit_basic);
        shaped(NS, "drone_waypoint_request", ItemIds.drone_waypoint_request, 4, rows("G", "T", "C"),
                "G", VanillaIds.blue_dye, "T", VanillaIds.redstone_torch, "C", ItemIds.circuit_basic);
        shaped(NS, "ducrete_2", ItemIds.ducrete, 4, rows("DD", "DD"), "D", ItemIds.ducrete_smooth);
        shaped(NS, "ducrete_slab", ItemIds.ducrete_slab, 6, rows("###"), "#", ItemIds.ducrete);
        shaped(NS, "ducrete_smooth_slab", ItemIds.ducrete_smooth_slab, 6, rows("###"), "#", ItemIds.ducrete_smooth);
        shaped(NS, "ducrete_smooth_stairs", ItemIds.ducrete_smooth_stairs, 8, rows("#  ", "## ", "###"),
                "#", ItemIds.ducrete_smooth);
        shaped(NS, "ducrete_smooth_stairs_1", ItemIds.ducrete_smooth_stairs, 4, rows("#  ", "## ", "###"),
                "#", ItemIds.ducrete_smooth_slab);
        shaped(NS, "ducrete_stairs", ItemIds.ducrete_stairs, 8, rows("#  ", "## ", "###"), "#", ItemIds.ducrete);
        shaped(NS, "ducrete_stairs_1", ItemIds.ducrete_stairs, 4, rows("#  ", "## ", "###"), "#", ItemIds.ducrete_slab);
        shaped(NS, "ducttape", ItemIds.ducttape, 4, rows("F", "P", "S"),
                "F", VanillaIds.string, "S", VanillaIds.slime_ball, "P", VanillaIds.paper);
        shaped(NS, "dungeon_chain", ItemIds.dungeon_chain, 8, rows("S", "S", "S"), "S", ItemIds.steel_beam);
        shaped(NS, "egg_balefire_shard", ItemIds.egg_balefire_shard, 9, rows("#"), "#", ItemIds.egg_balefire);
        shaped(NS, "egg_balefire_shard_1", ItemIds.egg_balefire_shard, rows("##", "##"), "#", ItemIds.powder_balefire);
        shaped(NS, "emp_bomb", ItemIds.emp_bomb, rows("LML", "LCL", "LML"),
                "L", TagIds.plates_lead, "M", ItemIds.magnetron, "C", ItemIds.circuit_advanced);
        shaped(NS, "fallout", ItemIds.fallout, 2, rows("##"), "#", ItemIds.fallout);
        shaped(NS, "fan", ItemIds.fan, rows("BPB", "PRP", "BPB"),
                "B", TagIds.bolts_steel, "P", TagIds.plates_iron, "R", TagIds.dusts_redstone);
        shaped(NS, "fence_metal", ItemIds.fence_metal, 6, rows("BIB", "BIB"),
                "B", VanillaIds.iron_bars, "I", VanillaIds.iron_ingot);
        shaped(NS, "fins_big_steel", ItemIds.fins_big_steel, rows(" PI", "III", " PI"),
                "P", TagIds.plates_steel, "I", TagIds.ingots_steel);
        shaped(NS, "fins_flat", ItemIds.fins_flat, rows("IP", "PP", "IP"),
                "P", TagIds.plates_steel, "I", TagIds.ingots_steel);
        shaped(NS, "fins_quad_titanium", ItemIds.fins_quad_titanium, rows(" PP", "III", " PP"),
                "P", TagIds.plates_titanium, "I", TagIds.ingots_titanium);
        shaped(NS, "fins_small_steel", ItemIds.fins_small_steel, rows(" PP", "PII", " PP"),
                "P", TagIds.plates_steel, "I", TagIds.ingots_steel);
        shaped(NS, "fins_tri_steel", ItemIds.fins_tri_steel, rows(" PI", "IIB", " PI"),
                "P", TagIds.plates_steel, "I", TagIds.ingots_steel, "B", TagIds.storage_blocks_steel);
        shaped(NS, "fissure_bomb", ItemIds.fissure_bomb, rows("SUS", "RPR", "SUS"),
                "S", ItemIds.semtex, "U", TagIds.storage_blocks_u238, "R", TagIds.ingots_tantalium,
                "P", TagIds.billets_pu239);
        shaped(NS, "flame_opinion", ItemIds.flame_opinion, rows(" R ", "RPR", " R "),
                "P", VanillaIds.paper, "R", VanillaIds.red_dye);
        shaped(NS, "flame_politics", ItemIds.flame_politics, rows(" I ", "IPI", " I "),
                "P", VanillaIds.paper, "I", VanillaIds.black_dye);
        shaped(NS, "flame_pony", ItemIds.flame_pony, rows(" O ", "DPD", " O "),
                "D", VanillaIds.pink_dye, "O", VanillaIds.yellow_dye, "P", VanillaIds.paper);
        shaped(NS, "flame_war", ItemIds.flame_war, rows("WHW", "CTP", "WOW"),
                "W", TagIds.minecraft_planks, "T", VanillaIds.tnt, "H", ItemIds.flame_pony,
                "C", ItemIds.flame_conspiracy, "P", ItemIds.flame_politics, "O", ItemIds.flame_opinion);
        shaped(NS, "floodlight", ItemIds.floodlight, 2, rows("CSC", "TST", "G G"),
                "C", ItemIds.circuit_capacitor, "S", TagIds.plates_steel, "T", ItemIds.coil_tungsten,
                "G", TagIds.glass_panes);
        shaped(NS, "fluid_counter_valve", ItemIds.fluid_counter_valve, rows("S", "W"),
                "S", ItemIds.circuit_chip, "W", ItemIds.fluid_switch);
        shaped(NS, "fluid_duct_mk2", ItemIds.fluid_duct_mk2, 8, rows("SAS", "   ", "SAS"),
                "S", TagIds.plates_steel, "A", TagIds.plates_aluminium);
        shaped(NS, "fluid_duct_neo_colored", ItemIds.fluid_duct_neo_colored, 8, rows("ASA", "   ", "ASA"),
                "S", TagIds.plates_steel, "A", TagIds.plates_aluminium);
        shaped(NS, "fluid_duct_neo_silver", ItemIds.fluid_duct_neo_silver, 8, rows("IAI", "   ", "IAI"),
                "I", TagIds.plates_iron, "A", TagIds.plates_aluminium);
        shaped(NS, "fluid_duct_paintable", ItemIds.fluid_duct_paintable, 8, rows("SAS", "A A", "SAS"),
                "S", TagIds.ingots_steel, "A", TagIds.plates_aluminium);
        shaped(NS, "fluid_duct_paintable_block_exhaust", ItemIds.fluid_duct_paintable_block_exhaust, 8,
                rows("SAS", "A A", "SAS"),
                "S", TagIds.ingots_iron, "A", ItemIds.plate_polymer);
        shaped(NS, "fluid_pump", ItemIds.fluid_pump, rows(" S ", "PGP", "IMI"),
                "S", TagIds.shells_steel, "P", TagIds.pipes_steel, "G", TagIds.ingots_graphite, "I", TagIds.ingots_steel,
                "M", ItemIds.motor);
        shaped(NS, "fluid_switch", ItemIds.fluid_switch, rows("S", "W"),
                "S", VanillaIds.redstone, "W", ItemIds.fluid_duct_paintable);
        shaped(NS, "fluorite_1", ItemIds.fluorite, 9, rows("#"), "#", ItemIds.block_fluorite);
        shaped(NS, "flywheel_beryllium", ItemIds.flywheel_beryllium, rows("IBI", "BTB", "IBI"),
                "B", TagIds.storage_blocks_beryllium, "I", TagIds.plates_cast_iron, "T", TagIds.pipes_dura_steel);
        shaped(NS, "foundry_basin", ItemIds.foundry_basin, rows("B B", "B B", "BSB"),
                "B", ItemIds.ingot_firebrick, "S", VanillaIds.stone_slab);
        shaped(NS, "foundry_channel", ItemIds.foundry_channel, 4, rows("B B", " S "),
                "B", ItemIds.ingot_firebrick, "S", VanillaIds.stone_slab);
        shaped(NS, "foundry_mold", ItemIds.foundry_mold, rows("B B", "BSB"),
                "B", ItemIds.ingot_firebrick, "S", VanillaIds.stone_slab);
        shaped(NS, "foundry_tank", ItemIds.foundry_tank, rows("B B", "I I", "BSB"),
                "B", ItemIds.ingot_firebrick, "I", TagIds.ingots_steel, "S", VanillaIds.smooth_stone_slab);
        shaped(NS, "fraction_spacer", ItemIds.fraction_spacer, rows("BHB"),
                "H", TagIds.shells_steel, "B", VanillaIds.iron_bars);
        shaped(NS, "furnace_iron", ItemIds.furnace_iron, rows("III", "IFI", "BBB"),
                "I", TagIds.ingots_iron, "F", VanillaIds.furnace, "B", VanillaIds.stone_bricks);
        shaped(NS, "gas_empty", ItemIds.gas_empty, 2, rows("S ", "AA", "AA"),
                "A", TagIds.plates_steel, "S", TagIds.plates_copper);
        shaped(NS, "gas_mask", ItemIds.gas_mask, rows("PPP", "GPG", " F "),
                "G", TagIds.glass_panes, "P", TagIds.plates_steel, "F", TagIds.plates_iron);
        shaped(NS, "gas_mask_filter", ItemIds.gas_mask_filter, rows("I", "F"),
                "F", ItemIds.filter_coal, "I", TagIds.plates_iron);
        shaped(NS, "gas_mask_filter_combo", ItemIds.gas_mask_filter_combo, rows("ZCZ", "CFC", "ZCZ"),
                "Z", TagIds.ingots_zirconium, "C", ItemIds.catalyst_clay, "F", ItemIds.gas_mask_filter);
        shaped(NS, "gas_mask_filter_mono", ItemIds.gas_mask_filter_mono, rows("ZZZ", "ZCZ", "ZZZ"),
                "Z", TagIds.nuggets_zirconium, "C", ItemIds.catalyst_clay);
        shaped(NS, "gas_mask_filter_piss", ItemIds.gas_mask_filter_piss, rows("I", "F"),
                "F", ItemIds.rag_piss, "I", TagIds.ingots_iron);
        shaped(NS, "gas_mask_filter_rag", ItemIds.gas_mask_filter_rag, rows("I", "F"),
                "F", ItemIds.rag_damp, "I", TagIds.ingots_iron);
        shaped(NS, "gas_mask_m65", ItemIds.gas_mask_m65, rows("PPP", "GPG", " F "),
                "G", TagIds.glass_panes, "P", TagIds.ingots_any_rubber, "F", TagIds.plates_iron);
    }

    private static void shaped4() {
        shaped(NS, "gas_mask_mono", ItemIds.gas_mask_mono, rows(" P ", "PPP", " F "),
                "P", TagIds.ingots_any_rubber, "F", TagIds.plates_iron);
        shaped(NS, "gas_mask_olde", ItemIds.gas_mask_olde, rows("PPP", "GPG", " F "),
                "G", TagIds.glass_panes, "P", VanillaIds.leather, "F", TagIds.ingots_iron);
        shaped(NS, "gear_large", stack(ItemIds.gear_large).with(ComponentIds.item_variant, 0), rows("III", "ICI", "III"),
                "I", TagIds.plates_iron, "C", TagIds.ingots_copper);
        shaped(NS, "gear_large_1", stack(ItemIds.gear_large).with(ComponentIds.item_variant, 1),
                rows("III", "ICI", "III"),
                "I", TagIds.plates_steel, "C", TagIds.ingots_titanium);
        shaped(NS, "glass_polarized", ItemIds.glass_polarized, 4, rows("##", "##"),
                "#", ItemIds.part_generic_glass_polarized);
        shaped(NS, "gneiss_brick", ItemIds.gneiss_brick, 4, rows("CC", "CC"), "C", ItemIds.gneiss_tile);
        shaped(NS, "gneiss_tile", ItemIds.gneiss_tile, 4, rows("CC", "CC"), "C", ItemIds.stone_gneiss);
        shaped(NS, "gun_am180", ItemIds.gun_am180, rows("BBR", "GMS"),
                "B", TagIds.barrels_light_dura_steel, "R", TagIds.receivers_light_dura_steel,
                "M", TagIds.mechanisms_gunmetal, "G", TagIds.grips_wood, "S", TagIds.stocks_wood);
        shaped(NS, "gun_b92", ItemIds.gun_b92, rows("DDD", "SSC", "  R"),
                "D", ItemIds.plate_dineutronium, "S", ItemIds.ingot_starmetal, "C", ItemIds.circuit_bismoid,
                "R", ItemIds.gun_lasrifle);
        shaped(NS, "gun_b92_ammo", ItemIds.gun_b92_ammo, rows("PSP", "ESE", "PSP"),
                "P", ItemIds.plate_steel, "S", ItemIds.ingot_starmetal, "E", ItemIds.powder_spark_mix);
        shaped(NS, "gun_g3", ItemIds.gun_g3, rows("BRM", "WGS"),
                "B", TagIds.barrels_light_weaponsteel, "R", TagIds.receivers_light_weaponsteel,
                "M", TagIds.mechanisms_weaponsteel, "W", TagIds.grips_wood, "G", TagIds.grips_rubber,
                "S", TagIds.stocks_wood);
        shaped(NS, "gun_greasegun", ItemIds.gun_greasegun, rows("BRS", "SMG"),
                "B", TagIds.barrels_light_steel, "R", TagIds.receivers_light_steel, "S", TagIds.bolts_steel,
                "M", TagIds.mechanisms_gunmetal, "G", TagIds.grips_steel);
        shaped(NS, "gun_heavy_revolver", ItemIds.gun_heavy_revolver, rows("BRM", "  G"),
                "B", TagIds.barrels_light_desh, "R", TagIds.receivers_light_desh, "M", TagIds.mechanisms_gunmetal,
                "G", TagIds.grips_wood);
        shaped(NS, "gun_henry", ItemIds.gun_henry, rows("BRP", "BMS"),
                "B", TagIds.barrels_light_steel, "R", TagIds.receivers_light_gunmetal, "M", TagIds.mechanisms_gunmetal,
                "S", TagIds.stocks_wood, "P", TagIds.plates_gunmetal);
        shaped(NS, "gun_henry_lincoln", ItemIds.gun_henry_lincoln, rows(" M ", "PGP", " M "),
                "M", TagIds.mechanisms_weaponsteel, "P", TagIds.plates_cast_gold, "G", ItemIds.gun_henry);
        shaped(NS, "gun_lag", ItemIds.gun_lag, rows("BRM", "  G"),
                "B", TagIds.barrels_light_any_resistant_alloy, "R", TagIds.receivers_light_any_resistant_alloy,
                "M", TagIds.mechanisms_weaponsteel, "G", TagIds.grips_any_plastic);
        shaped(NS, "gun_light_revolver", ItemIds.gun_light_revolver, rows("BRM", "  G"),
                "B", TagIds.barrels_light_steel, "R", TagIds.receivers_light_steel, "M", TagIds.mechanisms_gunmetal,
                "G", TagIds.grips_wood);
        shaped(NS, "gun_light_revolver_atlas", ItemIds.gun_light_revolver_atlas, rows(" M ", "MAM", " M "),
                "M", TagIds.mechanisms_weaponsteel, "A", ItemIds.gun_light_revolver);
        shaped(NS, "gun_pepperbox", ItemIds.gun_pepperbox, rows("IIW", "  C"),
                "I", TagIds.ingots_iron, "W", TagIds.minecraft_planks, "C", TagIds.ingots_copper);
        shaped(NS, "gun_star_f", ItemIds.gun_star_f, rows("BRM", "  G"),
                "B", TagIds.barrels_light_weaponsteel, "R", TagIds.receivers_light_weaponsteel,
                "M", TagIds.mechanisms_weaponsteel, "G", TagIds.grips_any_plastic);
        shaped(NS, "gun_star_f_akimbo", ItemIds.gun_star_f_akimbo, rows("UMU"),
                "U", ItemIds.gun_star_f, "M", TagIds.mechanisms_saturnite);
        shaped(NS, "gun_uzi", ItemIds.gun_uzi, rows("BRS", " GM"),
                "B", TagIds.barrels_light_desh, "R", TagIds.receivers_light_desh, "S", TagIds.stocks_any_plastic,
                "G", TagIds.grips_any_plastic, "M", TagIds.mechanisms_gunmetal);
        shaped(NS, "gun_uzi_akimbo", ItemIds.gun_uzi_akimbo, rows("UMU"),
                "U", ItemIds.gun_uzi, "M", TagIds.mechanisms_weaponsteel);
        shaped(NS, "hand_drill", ItemIds.hand_drill, rows(" D", "S ", " S"),
                "D", TagIds.ingots_dura_steel, "S", VanillaIds.stick);
        shaped(NS, "hazmat_boots", ItemIds.hazmat_boots, rows("E E", "E E"), "E", ItemIds.hazmat_cloth);
        shaped(NS, "hazmat_boots_grey", ItemIds.hazmat_boots_grey, rows("E E", "E E"), "E", ItemIds.hazmat_cloth_grey);
        shaped(NS, "hazmat_boots_red", ItemIds.hazmat_boots_red, rows("E E", "E E"), "E", ItemIds.hazmat_cloth_red);
        shaped(NS, "hazmat_cloth_grey", ItemIds.hazmat_cloth_grey, rows(" P ", "ICI", " L "),
                "C", ItemIds.hazmat_cloth_red, "P", TagIds.plates_iron, "L", TagIds.plates_lead,
                "I", TagIds.ingots_any_rubber);
        shaped(NS, "hazmat_cloth_red", ItemIds.hazmat_cloth_red, rows("C", "R", "C"),
                "C", ItemIds.hazmat_cloth, "R", TagIds.dusts_redstone);
        shaped(NS, "hazmat_helmet", ItemIds.hazmat_helmet, rows("EEE", "EIE", " P "),
                "E", ItemIds.hazmat_cloth, "I", TagIds.glass_panes, "P", TagIds.plates_iron);
        shaped(NS, "hazmat_helmet_grey", ItemIds.hazmat_helmet_grey, rows("EEE", "IEI", "EFE"),
                "E", ItemIds.hazmat_cloth_grey, "I", TagIds.glass_panes, "F", TagIds.plates_iron);
        shaped(NS, "hazmat_helmet_red", ItemIds.hazmat_helmet_red, rows("EEE", "IEI", "EFE"),
                "E", ItemIds.hazmat_cloth_red, "I", TagIds.glass_panes, "F", TagIds.plates_iron);
        shaped(NS, "hazmat_legs", ItemIds.hazmat_legs, rows("EEE", "E E", "E E"), "E", ItemIds.hazmat_cloth);
        shaped(NS, "hazmat_legs_grey", ItemIds.hazmat_legs_grey, rows("EEE", "E E", "E E"),
                "E", ItemIds.hazmat_cloth_grey);
        shaped(NS, "hazmat_legs_red", ItemIds.hazmat_legs_red, rows("EEE", "E E", "E E"),
                "E", ItemIds.hazmat_cloth_red);
        shaped(NS, "hazmat_plate", ItemIds.hazmat_plate, rows("E E", "EEE", "EEE"), "E", ItemIds.hazmat_cloth);
        shaped(NS, "hazmat_plate_grey", ItemIds.hazmat_plate_grey, rows("E E", "EEE", "EEE"),
                "E", ItemIds.hazmat_cloth_grey);
        shaped(NS, "hazmat_plate_red", ItemIds.hazmat_plate_red, rows("E E", "EEE", "EEE"),
                "E", ItemIds.hazmat_cloth_red);
        shaped(NS, "hopper", VanillaIds.hopper, rows("S S", "S S", " S "), "S", TagIds.ingots_steel);
        shaped(NS, "icf_pellet_empty", ItemIds.icf_pellet_empty, rows("ZLZ", "L L", "ZLZ"),
                "Z", TagIds.wires_zirconium, "L", TagIds.wires_lead);
        shaped(NS, "igniter", ItemIds.igniter, rows(" W", "SC", "CE"),
                "S", TagIds.plates_steel, "W", TagIds.wires_schrabidium, "C", ItemIds.circuit_advanced,
                "E", TagIds.ingots_euphemium);
        shaped(NS, "ingot_asbestos_1", ItemIds.ingot_asbestos, 9, rows("#"), "#", ItemIds.block_asbestos);
        shaped(NS, "ingot_beryllium_4", ItemIds.ingot_beryllium, 9, rows("#"), "#", ItemIds.block_beryllium);
        shaped(NS, "ingot_cobalt_5", ItemIds.ingot_cobalt, 9, rows("#"), "#", ItemIds.block_cobalt);
        shaped(NS, "ingot_combine_steel_2", ItemIds.ingot_combine_steel, 9, rows("#"),
                "#", ItemIds.block_combine_steel);
        shaped(NS, "ingot_copper_5", ItemIds.ingot_copper, 9, rows("#"), "#", ItemIds.block_copper);
        shaped(NS, "ingot_desh_2", ItemIds.ingot_desh, 9, rows("#"), "#", ItemIds.block_desh);
        shaped(NS, "ingot_dura_steel_1", ItemIds.ingot_dura_steel, 9, rows("#"), "#", ItemIds.block_dura_steel);
        shaped(NS, "ingot_euphemium_1", ItemIds.ingot_euphemium, 9, rows("#"), "#", ItemIds.block_euphemium);
        shaped(NS, "ingot_fiberglass_from_block", ItemIds.ingot_fiberglass, 9, rows("#"),
                "#", ItemIds.block_fiberglass);
        shaped(NS, "ingot_lead_3", ItemIds.ingot_lead, 9, rows("#"), "#", ItemIds.block_lead);
        shaped(NS, "ingot_magnetized_tungsten_2", ItemIds.ingot_magnetized_tungsten, 9, rows("#"),
                "#", ItemIds.block_magnetized_tungsten);
        shaped(NS, "ingot_mox_fuel_1", ItemIds.ingot_mox_fuel, 9, rows("#"), "#", ItemIds.block_mox_fuel);
        shaped(NS, "ingot_neptunium_2", ItemIds.ingot_neptunium, 9, rows("#"), "#", ItemIds.block_neptunium);
        shaped(NS, "ingot_phosphorus", ItemIds.ingot_phosphorus, 9, rows("#"), "#", ItemIds.block_white_phosphorus);
        shaped(NS, "ingot_plutonium_5", ItemIds.ingot_plutonium, 9, rows("#"), "#", ItemIds.block_plutonium);
        shaped(NS, "ingot_plutonium_fuel_1", ItemIds.ingot_plutonium_fuel, 9, rows("#"),
                "#", ItemIds.block_plutonium_fuel);
        shaped(NS, "ingot_polonium_2", ItemIds.ingot_polonium, 9, rows("#"), "#", ItemIds.block_polonium);
        shaped(NS, "ingot_pu238_1", ItemIds.ingot_pu238, 9, rows("#"), "#", ItemIds.block_pu238);
        shaped(NS, "ingot_pu239_1", ItemIds.ingot_pu239, 9, rows("#"), "#", ItemIds.block_pu239);
        shaped(NS, "ingot_pu240_1", ItemIds.ingot_pu240, 9, rows("#"), "#", ItemIds.block_pu240);
        shaped(NS, "ingot_red_copper_2", ItemIds.ingot_red_copper, 9, rows("#"), "#", ItemIds.block_red_copper);
        shaped(NS, "ingot_schrabidium_7", ItemIds.ingot_schrabidium, 9, rows("#"), "#", ItemIds.block_schrabidium);
        shaped(NS, "ingot_schrabidium_fuel_1", ItemIds.ingot_schrabidium_fuel, 9, rows("#"),
                "#", ItemIds.block_schrabidium_fuel);
        shaped(NS, "ingot_solinium_1", ItemIds.ingot_solinium, 9, rows("#"), "#", ItemIds.block_solinium);
        shaped(NS, "ingot_starmetal_1", ItemIds.ingot_starmetal, 9, rows("#"), "#", ItemIds.block_starmetal);
        shaped(NS, "ingot_steel_1", ItemIds.ingot_steel, 9, rows("#"), "#", ItemIds.block_steel);
        shaped(NS, "ingot_th232_4", ItemIds.ingot_th232, 9, rows("#"), "#", ItemIds.block_thorium);
        shaped(NS, "ingot_thorium_fuel_1", ItemIds.ingot_thorium_fuel, 9, rows("#"), "#", ItemIds.block_thorium_fuel);
        shaped(NS, "ingot_titanium_3", ItemIds.ingot_titanium, 9, rows("#"), "#", ItemIds.block_titanium);
        shaped(NS, "ingot_tungsten_5", ItemIds.ingot_tungsten, 9, rows("#"), "#", ItemIds.block_tungsten);
        shaped(NS, "ingot_u233_1", ItemIds.ingot_u233, 9, rows("#"), "#", ItemIds.block_u233);
        shaped(NS, "ingot_u235_1", ItemIds.ingot_u235, 9, rows("#"), "#", ItemIds.block_u235);
        shaped(NS, "ingot_u238_1", ItemIds.ingot_u238, 9, rows("#"), "#", ItemIds.block_u238);
        shaped(NS, "ingot_uranium_9", ItemIds.ingot_uranium, 9, rows("#"), "#", ItemIds.block_uranium);
        shaped(NS, "ingot_uranium_fuel_1", ItemIds.ingot_uranium_fuel, 9, rows("#"), "#", ItemIds.block_uranium_fuel);
        shaped(NS, "insert_du", ItemIds.insert_du, rows("DPD", "PSP", "DPD"),
                "D", ItemIds.ducttape, "P", TagIds.plates_iron, "S", TagIds.storage_blocks_u238);
        shaped(NS, "insert_esapi", ItemIds.insert_esapi, rows("PKP", "DSD", "PKP"),
                "P", TagIds.ingots_any_plastic, "K", ItemIds.insert_sapi, "D", ItemIds.ducttape,
                "S", TagIds.plates_weaponsteel);
        shaped(NS, "insert_ghiorsium", ItemIds.insert_ghiorsium, rows("DPD", "PSP", "DPD"),
                "D", ItemIds.ducttape, "P", TagIds.ingots_gh336, "S", TagIds.ingots_u238);
        shaped(NS, "insert_kevlar", ItemIds.insert_kevlar, rows("KIK", "IDI", "KIK"),
                "K", ItemIds.plate_kevlar, "I", TagIds.ingots_any_rubber, "D", ItemIds.ducttape);
        shaped(NS, "insert_polonium", ItemIds.insert_polonium, rows("DPD", "PSP", "DPD"),
                "D", ItemIds.ducttape, "P", TagIds.plates_iron, "S", TagIds.storage_blocks_polonium);
        shaped(NS, "insert_sapi", ItemIds.insert_sapi, rows("PKP", "DPD", "PKP"),
                "P", TagIds.ingots_any_plastic, "K", ItemIds.insert_kevlar, "D", ItemIds.ducttape);
        shaped(NS, "insert_steel", ItemIds.insert_steel, rows("DPD", "PSP", "DPD"),
                "D", ItemIds.ducttape, "P", TagIds.plates_iron, "S", TagIds.storage_blocks_steel);
        shaped(NS, "insert_xsapi", ItemIds.insert_xsapi, rows("PKP", "DSD", "PKP"),
                "P", TagIds.ingots_asbestos, "K", ItemIds.insert_esapi, "D", ItemIds.ducttape,
                "S", TagIds.plates_saturnite);
        shaped(NS, "insert_yharonite", ItemIds.insert_yharonite, rows("YIY", "IYI", "YIY"),
                "Y", ItemIds.billet_yharonite, "I", ItemIds.insert_du);
        shaped(NS, "iv_empty", ItemIds.iv_empty, 4, rows("S", "I", "S"),
                "S", TagIds.ingots_any_rubber, "I", TagIds.plates_iron);
        shaped(NS, "jetpack_boost", ItemIds.jetpack_boost, rows("PCP", "DJD", "PAP"),
                "C", ItemIds.circuit_advanced, "P", TagIds.plates_saturnite, "D", TagIds.ingots_desh,
                "J", ItemIds.jetpack_vector, "A", TagIds.plates_cast_copper);
        shaped(NS, "jetpack_break", ItemIds.jetpack_break, rows("ICI", "TJT", "I I"),
                "C", ItemIds.circuit_basic, "T", ItemIds.ingot_dura_steel, "J", ItemIds.jetpack_fly,
                "I", ItemIds.plate_polymer);
        shaped(NS, "jetpack_fly", ItemIds.jetpack_fly, rows("ACA", "TLT", "D D"),
                "A", TagIds.plates_aluminium, "C", ItemIds.circuit_basic, "T", ItemIds.tank_steel,
                "L", VanillaIds.leather, "D", ItemIds.thruster_small);
        shaped(NS, "jetpack_glider", ItemIds.jetpack_glider, rows("CSC", "DJD", "T T"),
                "J", ItemIds.jetpack_boost, "C", ItemIds.circuit_advanced, "D", ItemIds.plate_desh,
                "T", ItemIds.thruster_nuclear, "S", ItemIds.motor);
        shaped(NS, "jetpack_vector", ItemIds.jetpack_vector, rows("TCT", "MJM", "B B"),
                "C", ItemIds.circuit_advanced, "T", ItemIds.tank_steel, "J", ItemIds.jetpack_break, "M", ItemIds.motor,
                "B", TagIds.bolts_dura_steel);
        shaped(NS, "key", ItemIds.key, rows("  B", " B ", "P  "), "P", TagIds.plates_steel, "B", TagIds.bolts_steel);
        shaped(NS, "key_kit", ItemIds.key_kit, rows("PKP", "DTD", "PKP"),
                "P", TagIds.plates_gold, "K", ItemIds.key, "D", TagIds.dusts_desh, "T", ItemIds.screwdriver);
        shaped(NS, "ladder_copper", ItemIds.ladder_copper, 8, rows("LLL", "L#L", "LLL"),
                "L", VanillaIds.ladder, "#", TagIds.ingots_copper);
        shaped(NS, "ladder_gold", ItemIds.ladder_gold, 8, rows("LLL", "L#L", "LLL"),
                "L", VanillaIds.ladder, "#", TagIds.ingots_gold);
        shaped(NS, "ladder_steel", ItemIds.ladder_steel, 8, rows("LLL", "L#L", "LLL"),
                "L", VanillaIds.ladder, "#", TagIds.ingots_steel);
        shaped(NS, "ladder_sturdy", ItemIds.ladder_sturdy, 8, rows("LLL", "L#L", "LLL"),
                "L", VanillaIds.ladder, "#", TagIds.minecraft_planks);
        shaped(NS, "ladder_titanium", ItemIds.ladder_titanium, 8, rows("LLL", "L#L", "LLL"),
                "L", VanillaIds.ladder, "#", TagIds.ingots_titanium);
        shaped(NS, "lamp_demon", ItemIds.lamp_demon, rows(" D ", "S S"),
                "D", ItemIds.demon_core_closed, "S", TagIds.ingots_steel);
        shaped(NS, "laser_crystal_bale", ItemIds.laser_crystal_bale, rows("QDQ", "SBS", "QDQ"),
                "Q", ItemIds.glass_quartz, "D", TagIds.ingots_dineutronium, "B", ItemIds.egg_balefire,
                "S", ItemIds.powder_spark_mix);
        shaped(NS, "laser_crystal_bismuth", ItemIds.laser_crystal_bismuth, rows("QUQ", "BCB", "QTQ"),
                "Q", ItemIds.glass_quartz, "U", TagIds.ingots_uranium, "T", TagIds.ingots_thorium,
                "B", ItemIds.nugget_bismuth, "C", ItemIds.crystal_rare);
        shaped(NS, "lemon", ItemIds.lemon, rows(" D ", "DSD", " D "),
                "D", VanillaIds.yellow_dye, "S", VanillaIds.stone);
        shaped(NS, "lightstone_bricks_stairs", ItemIds.lightstone_bricks_stairs, 4, rows("#  ", "## ", "###"),
                "#", ItemIds.lightstone_bricks_slab);
        shaped(NS, "lightstone_tile_stairs", ItemIds.lightstone_tile_stairs, 4, rows("#  ", "## ", "###"),
                "#", ItemIds.lightstone_tile_slab);
        shaped(NS, "machine_ammo_press", ItemIds.machine_ammo_press, rows("IPI", "C C", "SSS"),
                "I", TagIds.ingots_iron, "P", VanillaIds.piston, "C", TagIds.ingots_copper, "S", VanillaIds.stone);
        shaped(NS, "machine_armor_table", ItemIds.machine_armor_table, rows("PPP", "TCT", "TST"),
                "P", TagIds.plates_steel, "T", TagIds.ingots_tungsten, "C", VanillaIds.crafting_table,
                "S", TagIds.storage_blocks_steel);
        shaped(NS, "machine_autocrafter", ItemIds.machine_autocrafter, rows("SCS", "MWM", "SCS"),
                "S", TagIds.plates_steel, "C", ItemIds.circuit_vacuum_tube, "M", ItemIds.motor,
                "W", VanillaIds.crafting_table);
        shaped(NS, "machine_funnel", "hbm:machine_funnel", rows("S S", "SRS", " S "),
                "S", TagIds.ingots_steel, "R", TagIds.dusts_redstone);
        shaped(NS, "machine_battery_socket", ItemIds.machine_battery_socket, rows("I I", "I I", "IRI"),
                "I", ItemIds.plate_polymer, "R", ItemIds.coil_copper);
        shaped(NS, "machine_battery_socket_1", ItemIds.machine_battery_socket, rows("PRP"),
                "P", TagIds.plates_steel, "R", TagIds.ingots_red_copper);
        shaped(NS, "machine_condenser", ItemIds.machine_condenser, rows("SIS", "ICI", "SIS"),
                "S", TagIds.ingots_steel, "I", TagIds.plates_iron, "C", TagIds.plates_cast_copper);
    }

    private static void shaped5() {
        shaped(NS, "machine_controller", ItemIds.machine_controller, rows("TDT", "DCD", "TDT"),
                "T", TagIds.ingots_any_resistant_alloy, "D", ItemIds.crt_display, "C", ItemIds.circuit_advanced);
        shaped(NS, "machine_conveyor_press", ItemIds.machine_conveyor_press, rows("CPC", "CBC", "CCC"),
                "C", TagIds.plates_copper, "P", ItemIds.machine_epress, "B", ItemIds.conveyor_wand_regular);
        shaped(NS, "machine_conveyor_press_block", ItemIds.machine_conveyor_press, rows("CPC", "CBC", "CCC"),
                "C", TagIds.plates_copper, "P", ItemIds.machine_epress, "B", ItemIds.conveyor);
        shaped(NS, "machine_detector", ItemIds.machine_detector, rows("IRI", "CTC", "IRI"),
                "I", ItemIds.plate_polymer, "R", TagIds.dusts_redstone, "C", TagIds.wires_red_copper,
                "T", ItemIds.coil_tungsten);
        shaped(NS, "machine_drain", ItemIds.machine_drain, rows("PPP", "T  ", "PPP"),
                "P", TagIds.plates_cast_steel, "T", ItemIds.tank_steel);
        shaped(NS, "machine_electric_furnace_off", ItemIds.machine_electric_furnace_off, rows("BBB", "WFW", "RRR"),
                "B", TagIds.ingots_beryllium, "R", ItemIds.coil_tungsten, "W", TagIds.plates_cast_copper,
                "F", VanillaIds.furnace);
        shaped(NS, "machine_fraction_tower", ItemIds.machine_fraction_tower, rows("H", "G", "H"),
                "H", TagIds.plates_welded_steel, "G", ItemIds.steel_grate);
        shaped(NS, "machine_furnace_brick_off", ItemIds.machine_furnace_brick_off, rows("III", "I I", "BBB"),
                "I", VanillaIds.brick, "B", VanillaIds.stone);
        shaped(NS, "machine_intake", ItemIds.machine_intake, rows("GGG", "PMP", "PTP"),
                "G", ItemIds.steel_grate, "P", TagIds.plates_steel, "M", ItemIds.motor, "T", ItemIds.tank_steel);
        shaped(NS, "machine_keyforge", ItemIds.machine_keyforge, rows("PCP", "WSW", "WSW"),
                "P", TagIds.plates_steel, "S", TagIds.ingots_tungsten, "C", ItemIds.padlock,
                "W", TagIds.minecraft_planks);
        shaped(NS, "machine_microwave", ItemIds.machine_microwave, rows("III", "SGM", "IDI"),
                "I", ItemIds.plate_polymer, "S", TagIds.plates_steel, "G", TagIds.glass_panes, "M", ItemIds.magnetron,
                "D", ItemIds.motor);
        shaped(NS, "machine_mixer", ItemIds.machine_mixer, rows("PIP", "GCG", "PMP"),
                "P", TagIds.plates_steel, "I", TagIds.ingots_dura_steel, "G", TagIds.glass_panes,
                "C", ItemIds.circuit_vacuum_tube, "M", ItemIds.motor);
        shaped(NS, "machine_press", ItemIds.machine_press, rows("IRI", "IPI", "IBI"),
                "I", TagIds.ingots_iron, "R", VanillaIds.furnace, "B", TagIds.storage_blocks_iron,
                "P", VanillaIds.piston);
        shaped(NS, "machine_siren", ItemIds.machine_siren, rows("SIS", "ICI", "SRS"),
                "S", TagIds.plates_steel, "I", TagIds.ingots_any_rubber, "C", ItemIds.circuit_vacuum_tube,
                "R", TagIds.dusts_redstone);
        shaped(NS, "machine_solar_boiler", ItemIds.machine_solar_boiler, rows("SHS", "DHD", "SHS"),
                "S", TagIds.ingots_steel, "H", TagIds.shells_steel, "D", VanillaIds.black_dye);
        shaped(NS, "machine_storage_drum", ItemIds.machine_storage_drum, rows("LLL", "L#L", "LLL"),
                "L", TagIds.plates_lead, "#", ItemIds.tank_steel);
        shaped(NS, "machine_transformer", ItemIds.machine_transformer, rows("SCS", "MDM", "SCS"),
                "S", TagIds.ingots_iron, "D", TagIds.ingots_red_copper, "M", ItemIds.coil_copper,
                "C", ItemIds.circuit_capacitor);
        shaped(NS, "machine_transformer_dnt", ItemIds.machine_transformer_dnt, rows("SDS", "MCM", "MCM"),
                "S", TagIds.ingots_starmetal, "D", TagIds.ingots_desh, "M", TagIds.wires_dense_magnetized_tungsten,
                "C", ItemIds.circuit_bismoid);
        shaped(NS, "machine_turbine", ItemIds.machine_turbine, rows("SMS", "PTP", "SMS"),
                "S", TagIds.ingots_steel, "T", ItemIds.turbine_titanium, "M", ItemIds.coil_copper,
                "P", TagIds.ingots_any_plastic);
        shaped(NS, "machine_waste_drum", ItemIds.machine_waste_drum, rows("LRL", "BRB", "LRL"),
                "L", TagIds.ingots_lead, "B", VanillaIds.iron_bars, "R", ItemIds.rod_quad_empty);
        shaped(NS, "machine_weapon_table", ItemIds.machine_weapon_table, rows("PPP", "TCT", "TST"),
                "P", TagIds.plates_gunmetal, "T", TagIds.ingots_steel, "C", VanillaIds.crafting_table,
                "S", ItemIds.block_steel);
        shaped(NS, "machine_wood_burner", ItemIds.machine_wood_burner, rows("PPP", "CFC", "I I"),
                "P", TagIds.plates_steel, "C", ItemIds.coil_copper, "I", TagIds.ingots_iron, "F", VanillaIds.furnace);
        shaped(NS, "machine_zirnox", ItemIds.machine_zirnox, rows("SUS", "RCR", "SUS"),
                "S", ItemIds.plate_steel, "U", ItemIds.block_uranium, "R", ItemIds.rod_zirnox_empty,
                "C", ItemIds.coil_copper);
        shaped(NS, "mass_storage_iron", ItemIds.mass_storage_iron, rows(" L ", "ICI", " I "),
                "I", TagIds.ingots_titanium, "C", ItemIds.crate_steel, "L", ItemIds.circuit_vacuum_tube);
        shaped(NS, "mass_storage_wood", ItemIds.mass_storage_wood, rows("PPP", "PIP", "PPP"),
                "P", TagIds.minecraft_planks, "I", TagIds.plates_iron);
        shaped(NS, "med_bag", ItemIds.med_bag, rows("LLL", "SIS", "LLL"),
                "L", VanillaIds.leather, "S", ItemIds.syringe_metal_stimpak, "I", ItemIds.pill_iodine);
        shaped(NS, "med_bag_1", ItemIds.med_bag, rows("LL", "SI", "LL"),
                "L", VanillaIds.leather, "S", ItemIds.syringe_metal_super, "I", ItemIds.radaway);
        shaped(NS, "med_bag_2", ItemIds.med_bag, rows("LLL", "SIS", "LLL"),
                "L", TagIds.ingots_any_rubber, "S", ItemIds.syringe_metal_stimpak, "I", ItemIds.syringe_antidote);
        shaped(NS, "med_bag_3", ItemIds.med_bag, rows("LLL", "SIS", "LLL"),
                "L", TagIds.ingots_any_rubber, "S", ItemIds.syringe_metal_stimpak, "I", ItemIds.pill_iodine);
        shaped(NS, "med_bag_4", ItemIds.med_bag, rows("LL", "SI", "LL"),
                "L", TagIds.ingots_any_rubber, "S", ItemIds.syringe_metal_super, "I", ItemIds.radaway);
        shaped(NS, "meteor_battery", ItemIds.meteor_battery, rows("MSM", "MWM", "MSM"),
                "M", ItemIds.meteor_polished, "S", TagIds.storage_blocks_starmetal, "W", TagIds.wires_schrabidium);
        shaped(NS, "meteor_brick", ItemIds.meteor_brick, 4, rows("CC", "CC"), "C", ItemIds.meteor_polished);
        shaped(NS, "meteor_brick_cracked", ItemIds.meteor_brick_cracked, 6, rows(" C ", "C C", " C "),
                "C", ItemIds.meteor_brick);
        shaped(NS, "meteor_brick_mossy", ItemIds.meteor_brick_mossy, 8, rows("CCC", "CVC", "CCC"),
                "C", ItemIds.meteor_brick, "V", VanillaIds.vine);
        shaped(NS, "meteor_pillar", ItemIds.meteor_pillar, 2, rows("C", "C"), "C", ItemIds.meteor_polished);
        shaped(NS, "meteor_polished", ItemIds.meteor_polished, 4, rows("CC", "CC"), "C", ItemIds.block_meteor_broken);
        shaped(NS, "mine_ap", ItemIds.mine_ap, 4, rows("I", "C", "S"),
                "I", ItemIds.plate_polymer, "C", TagIds.dusts_any_smokeless, "S", TagIds.ingots_steel);
        shaped(NS, "mine_fat", ItemIds.mine_fat, rows("CDN"),
                "C", ItemIds.circuit_analog, "D", ItemIds.ducttape, "N", ItemIds.ammo_standard_nuke_demo);
        shaped(NS, "mine_he", ItemIds.mine_he, rows(" C ", "PTP"),
                "C", ItemIds.circuit_basic, "P", TagIds.plates_steel, "T", TagIds.ingots_any_highexplosive);
        shaped(NS, "mine_shrap", ItemIds.mine_shrap, rows("L", "M"),
                "L", ItemIds.pellet_buckshot, "M", ItemIds.mine_ap);
        shaped(NS, "mirror_tool", ItemIds.mirror_tool, rows(" A ", " IA", "I  "),
                "A", TagIds.ingots_aluminium, "I", TagIds.ingots_iron);
        shaped(NS, "mold_base", ItemIds.mold_base, rows(" B ", "BIB", " B "),
                "B", ItemIds.ingot_firebrick, "I", TagIds.ingots_iron);
        shaped(NS, "motor", ItemIds.motor, 2, rows(" R ", "ICI", "ITI"),
                "R", TagIds.wires_red_copper, "T", ItemIds.coil_copper_torus, "I", TagIds.plates_iron,
                "C", ItemIds.coil_copper);
        shaped(NS, "motor_1", ItemIds.motor, 2, rows(" R ", "ICI", " T "),
                "R", TagIds.wires_red_copper, "T", ItemIds.coil_copper_torus, "I", TagIds.plates_steel,
                "C", ItemIds.coil_copper);
        shaped(NS, "motor_bismuth", ItemIds.motor_bismuth, rows("BCB", "SDS", "BCB"),
                "B", TagIds.nuggets_bismuth, "C", TagIds.wires_dense_neodymium, "S", TagIds.plates_cast_steel,
                "D", TagIds.ingots_dura_steel);
        shaped(NS, "motor_desh", ItemIds.motor_desh, rows("PCP", "DMD", "PCP"),
                "P", TagIds.ingots_any_plastic, "C", TagIds.wires_dense_gold, "D", TagIds.ingots_desh,
                "M", ItemIds.motor);
        shaped(NS, "name_tag", VanillaIds.name_tag, rows("SB ", "BPB", " BP"),
                "S", VanillaIds.string, "B", VanillaIds.slime_ball, "P", VanillaIds.paper);
        shaped(NS, "niter_1", ItemIds.niter, 9, rows("#"), "#", ItemIds.block_niter);
        shaped(NS, "nitra", ItemIds.nitra, rows("##", "##"), "#", ItemIds.nitra_small);
        shaped(NS, "nitra_small", ItemIds.nitra_small, 4, rows("#"), "#", ItemIds.nitra);
        shaped(NS, "nuclear_waste_tiny", ItemIds.nuclear_waste_tiny, 9, rows("#"), "#", ItemIds.nuclear_waste);
        shaped(NS, "nugget_australium_1", ItemIds.nugget_australium, 9, rows("#"), "#", ItemIds.ingot_australium);
        shaped(NS, "nugget_beryllium", ItemIds.nugget_beryllium, 9, rows("#"), "#", ItemIds.ingot_beryllium);
        shaped(NS, "nugget_euphemium", ItemIds.nugget_euphemium, 9, rows("#"), "#", ItemIds.ingot_euphemium);
        shaped(NS, "nugget_hes", ItemIds.nugget_hes, 9, rows("#"), "#", ItemIds.ingot_hes);
        shaped(NS, "nugget_lead", ItemIds.nugget_lead, 9, rows("#"), "#", ItemIds.ingot_lead);
        shaped(NS, "nugget_les", ItemIds.nugget_les, 9, rows("#"), "#", ItemIds.ingot_les);
        shaped(NS, "nugget_mercury_1", ItemIds.nugget_mercury, 8, rows("#"), "#", ItemIds.bottle_mercury);
        shaped(NS, "nugget_mox_fuel", ItemIds.nugget_mox_fuel, 9, rows("#"), "#", ItemIds.ingot_mox_fuel);
        shaped(NS, "nugget_neptunium", ItemIds.nugget_neptunium, 9, rows("#"), "#", ItemIds.ingot_neptunium);
        shaped(NS, "nugget_plutonium", ItemIds.nugget_plutonium, 9, rows("#"), "#", ItemIds.ingot_plutonium);
        shaped(NS, "nugget_plutonium_fuel", ItemIds.nugget_plutonium_fuel, 9, rows("#"),
                "#", ItemIds.ingot_plutonium_fuel);
        shaped(NS, "nugget_polonium", ItemIds.nugget_polonium, 9, rows("#"), "#", ItemIds.ingot_polonium);
        shaped(NS, "nugget_pu238", ItemIds.nugget_pu238, 9, rows("#"), "#", ItemIds.ingot_pu238);
        shaped(NS, "nugget_pu239", ItemIds.nugget_pu239, 9, rows("#"), "#", ItemIds.ingot_pu239);
        shaped(NS, "nugget_pu240", ItemIds.nugget_pu240, 9, rows("#"), "#", ItemIds.ingot_pu240);
        shaped(NS, "nugget_schrabidium_2", ItemIds.nugget_schrabidium, 9, rows("#"), "#", ItemIds.ingot_schrabidium);
        shaped(NS, "nugget_schrabidium_fuel", ItemIds.nugget_schrabidium_fuel, 9, rows("#"),
                "#", ItemIds.ingot_schrabidium_fuel);
        shaped(NS, "nugget_solinium", ItemIds.nugget_solinium, 9, rows("#"), "#", ItemIds.ingot_solinium);
        shaped(NS, "nugget_th232", ItemIds.nugget_th232, 9, rows("#"), "#", ItemIds.ingot_th232);
        shaped(NS, "nugget_thorium_fuel", ItemIds.nugget_thorium_fuel, 9, rows("#"), "#", ItemIds.ingot_thorium_fuel);
        shaped(NS, "nugget_u233", ItemIds.nugget_u233, 9, rows("#"), "#", ItemIds.ingot_u233);
        shaped(NS, "nugget_u235", ItemIds.nugget_u235, 9, rows("#"), "#", ItemIds.ingot_u235);
        shaped(NS, "nugget_u238", ItemIds.nugget_u238, 9, rows("#"), "#", ItemIds.ingot_u238);
        shaped(NS, "nugget_uranium", ItemIds.nugget_uranium, 9, rows("#"), "#", ItemIds.ingot_uranium);
        shaped(NS, "nugget_uranium_fuel", ItemIds.nugget_uranium_fuel, 9, rows("#"), "#", ItemIds.ingot_uranium_fuel);
        shaped(NS, "paa_boots", ItemIds.paa_boots, rows("E E", "N N"),
                "E", ItemIds.plate_paa, "N", ItemIds.neutron_reflector);
        shaped(NS, "paa_legs", ItemIds.paa_legs, rows("EEE", "N N", "E E"),
                "E", ItemIds.plate_paa, "N", ItemIds.neutron_reflector);
        shaped(NS, "paa_plate", ItemIds.paa_plate, rows("E E", "NEN", "ENE"),
                "E", ItemIds.plate_paa, "N", ItemIds.neutron_reflector);
        shaped(NS, "padlock", ItemIds.padlock, rows(" P ", "PBP", "PPP"),
                "P", TagIds.plates_steel, "B", TagIds.bolts_steel);
        shaped(NS, "padlock_reinforced", ItemIds.padlock_reinforced, rows(" P ", "PBP", "PDP"),
                "P", TagIds.plates_dura_steel, "D", ItemIds.plate_desh, "B", TagIds.bolts_dura_steel);
        shaped(NS, "padlock_rusty", ItemIds.padlock_rusty, rows("I", "B", "I"),
                "I", TagIds.ingots_iron, "B", TagIds.bolts_steel);
        shaped(NS, "padlock_unbreakable", ItemIds.padlock_unbreakable, rows(" P ", "PBP", "PDP"),
                "P", TagIds.plates_saturnite, "D", TagIds.gems_diamond, "B", TagIds.bolts_dura_steel);
        shaped(NS, "paper", VanillaIds.paper, 3, rows("SSS"), "S", ItemIds.powder_sawdust);
        shaped(NS, "part_generic_piston_electric", ItemIds.part_generic_piston_electric, 4, rows(" I ", "CPC", " I "),
                "I", TagIds.ingots_any_resistant_alloy, "C", TagIds.ingots_any_plastic, "P", ItemIds.motor);
        shaped(NS, "part_generic_piston_pneumatic", ItemIds.part_generic_piston_pneumatic, 4, rows(" I ", "CPC", " I "),
                "I", TagIds.ingots_iron, "C", TagIds.ingots_copper, "P", TagIds.plates_iron);
        shaped(NS, "part_grip_bakelite", ItemIds.part_grip_bakelite, rows("W ", " W", " W"),
                "W", TagIds.ingots_bakelite);
        shaped(NS, "part_grip_bone", ItemIds.part_grip_bone, rows("W ", " W", " W"), "W", VanillaIds.bone);
        shaped(NS, "part_grip_pc", ItemIds.part_grip_pc, rows("W ", " W", " W"), "W", TagIds.ingots_pc);
        shaped(NS, "part_grip_polymer", ItemIds.part_grip_polymer, rows("W ", " W", " W"), "W", TagIds.ingots_polymer);
        shaped(NS, "part_grip_pvc", ItemIds.part_grip_pvc, rows("W ", " W", " W"), "W", TagIds.ingots_pvc);
        shaped(NS, "part_grip_rubber", ItemIds.part_grip_rubber, rows("W ", " W", " W"), "W", TagIds.ingots_rubber);
        shaped(NS, "part_grip_wood", ItemIds.part_grip_wood, rows("W ", " W", " W"), "W", TagIds.minecraft_planks);
        shaped(NS, "part_stock_bakelite", ItemIds.part_stock_bakelite, rows("WWW", "  W"), "W", TagIds.ingots_bakelite);
        shaped(NS, "part_stock_pc", ItemIds.part_stock_pc, rows("WWW", "  W"), "W", TagIds.ingots_pc);
        shaped(NS, "part_stock_polymer", ItemIds.part_stock_polymer, rows("WWW", "  W"), "W", TagIds.ingots_polymer);
        shaped(NS, "part_stock_pvc", ItemIds.part_stock_pvc, rows("WWW", "  W"), "W", TagIds.ingots_pvc);
        shaped(NS, "part_stock_wood", ItemIds.part_stock_wood, rows("WWW", "  W"), "W", TagIds.minecraft_planks);
        shaped(NS, "particle_empty", ItemIds.particle_empty, 2, rows("STS", "G G", "STS"),
                "S", TagIds.plates_cast_lead, "T", ItemIds.coil_gold, "G", TagIds.glass_panes);
        shaped(NS, "pedestal_steel", ItemIds.pedestal_steel, rows("P P", "P P", "III"),
                "P", TagIds.plates_steel, "I", TagIds.ingots_steel);
        shaped(NS, "photo_panel", ItemIds.photo_panel, rows(" G ", "IPI", " C "),
                "G", TagIds.glass_panes, "I", ItemIds.plate_polymer, "P", TagIds.dusts_quartz, "C", ItemIds.circuit_pcb);
        shaped(NS, "pile_device_control", stack(ItemIds.pile_device).with(ComponentIds.item_variant, 2),
                rows(" B ", "SBS", "SBS"),
                "B", TagIds.ingots_boron, "S", TagIds.plates_steel);
        shaped(NS, "pile_device_loader", stack(ItemIds.pile_device).with(ComponentIds.item_variant, 0),
                rows(" A ", "CBS"),
                "A", TagIds.plates_aluminium, "C", TagIds.plates_cast_steel, "B", TagIds.ingots_boron,
                "S", TagIds.shells_steel);
        shaped(NS, "pile_device_vent", stack(ItemIds.pile_device).with(ComponentIds.item_variant, 1),
                rows(" M ", "ACA", " S "),
                "M", ItemIds.motor, "A", TagIds.plates_aluminium, "C", TagIds.shells_copper,
                "S", TagIds.plates_cast_steel);
        shaped(NS, "pin", ItemIds.pin, rows("W ", " W", " W"), "W", TagIds.wires_copper);
        shaped(NS, "pink_slab", ItemIds.pink_slab, 6, rows("###"), "#", ItemIds.pink_planks);
        shaped(NS, "pink_slab_1", ItemIds.pink_slab, 6, rows("WWW"), "W", ItemIds.pink_planks);
        shaped(NS, "pink_stairs", ItemIds.pink_stairs, 8, rows("#  ", "## ", "###"), "#", ItemIds.pink_planks);
        shaped(NS, "pink_stairs_1", ItemIds.pink_stairs, 4, rows("#  ", "## ", "###"), "#", ItemIds.pink_slab);
        shaped(NS, "pink_stairs_2", ItemIds.pink_stairs, 6, rows("W  ", "WW ", "WWW"), "W", ItemIds.pink_planks);
    }

    private static void shaped6() {
        shaped(NS, "pipe_anchor", ItemIds.pipe_anchor, 2, rows("P", "P", "S"),
                "P", TagIds.pipes_steel, "S", TagIds.ingots_steel);
        shaped(NS, "pipes_steel", ItemIds.pipes_steel, rows("B", "B", "B"), "B", TagIds.storage_blocks_steel);
        shaped(NS, "piston_selenium", ItemIds.piston_selenium, rows("SSS", "STS", " D "),
                "S", TagIds.plates_steel, "T", TagIds.ingots_tungsten, "D", TagIds.bolts_dura_steel);
        shaped(NS, "plan_c", ItemIds.plan_c, rows("PFP"), "P", ItemIds.powder_poison, "F", TagIds.dusts_fluorite);
        shaped(NS, "plate_cast_iron", ItemIds.plate_cast_iron, rows("BPB", "BPB", "BPB"),
                "B", TagIds.bolts_steel, "P", TagIds.plates_iron);
        shaped(NS, "plate_polymer", ItemIds.plate_polymer, 8, rows("DD"), "D", TagIds.ingots_any_plastic);
        shaped(NS, "plate_polymer_1", ItemIds.plate_polymer, 8, rows("DD"), "D", TagIds.ingots_any_rubber);
        shaped(NS, "plate_polymer_2", ItemIds.plate_polymer, 16, rows("DD"), "D", TagIds.ingots_asbestos);
        shaped(NS, "plate_polymer_3", ItemIds.plate_polymer, 4, rows("SWS"),
                "S", VanillaIds.string, "W", TagIds.minecraft_wool);
        shaped(NS, "plate_polymer_4", ItemIds.plate_polymer, 4, rows("BB"), "B", VanillaIds.brick);
        shaped(NS, "plate_polymer_from_insulator", ItemIds.plate_polymer, 9, rows("#"), "#", ItemIds.block_insulator);
        shaped(NS, "platemetal_black", ItemIds.platemetal_black, 8, rows("PPP", "PDP", "PPP"),
                "P", ItemIds.platemetal_base, "D", VanillaIds.black_dye);
        shaped(NS, "platemetal_blue", ItemIds.platemetal_blue, 8, rows("PPP", "PDP", "PPP"),
                "P", ItemIds.platemetal_base, "D", VanillaIds.blue_dye);
        shaped(NS, "platemetal_cyan", ItemIds.platemetal_cyan, 8, rows("PPP", "PDP", "PPP"),
                "P", ItemIds.platemetal_base, "D", VanillaIds.cyan_dye);
        shaped(NS, "platemetal_green", ItemIds.platemetal_green, 8, rows("PPP", "PDP", "PPP"),
                "P", ItemIds.platemetal_base, "D", VanillaIds.green_dye);
        shaped(NS, "platemetal_light_blue", ItemIds.platemetal_light_blue, 8, rows("PPP", "PDP", "PPP"),
                "P", ItemIds.platemetal_base, "D", VanillaIds.light_blue_dye);
        shaped(NS, "platemetal_light_gray", ItemIds.platemetal_light_gray, 8, rows("PPP", "PDP", "PPP"),
                "P", ItemIds.platemetal_base, "D", VanillaIds.light_gray_dye);
        shaped(NS, "platemetal_lime", ItemIds.platemetal_lime, 8, rows("PPP", "PDP", "PPP"),
                "P", ItemIds.platemetal_base, "D", VanillaIds.lime_dye);
        shaped(NS, "platemetal_magenta", ItemIds.platemetal_magenta, 8, rows("PPP", "PDP", "PPP"),
                "P", ItemIds.platemetal_base, "D", VanillaIds.magenta_dye);
        shaped(NS, "platemetal_orange", ItemIds.platemetal_orange, 8, rows("PPP", "PDP", "PPP"),
                "P", ItemIds.platemetal_base, "D", VanillaIds.orange_dye);
        shaped(NS, "platemetal_pink", ItemIds.platemetal_pink, 8, rows("PPP", "PDP", "PPP"),
                "P", ItemIds.platemetal_base, "D", VanillaIds.pink_dye);
        shaped(NS, "platemetal_purple", ItemIds.platemetal_purple, 8, rows("PPP", "PDP", "PPP"),
                "P", ItemIds.platemetal_base, "D", VanillaIds.purple_dye);
        shaped(NS, "platemetal_red", ItemIds.platemetal_red, 8, rows("PPP", "PDP", "PPP"),
                "P", ItemIds.platemetal_base, "D", VanillaIds.red_dye);
        shaped(NS, "platemetal_white", ItemIds.platemetal_white, 8, rows("PPP", "PDP", "PPP"),
                "P", ItemIds.platemetal_base, "D", VanillaIds.white_dye);
        shaped(NS, "platemetal_yellow", ItemIds.platemetal_yellow, 8, rows("PPP", "PDP", "PPP"),
                "P", ItemIds.platemetal_base, "D", VanillaIds.yellow_dye);
        shaped(NS, "pneumatic_tube", ItemIds.pneumatic_tube, 8, rows("CRC"),
                "C", TagIds.plates_cast_copper, "R", TagIds.ingots_any_rubber);
        shaped(NS, "pneumatic_tube_paintable", ItemIds.pneumatic_tube_paintable, 4, rows("SAS", "A A", "SAS"),
                "S", TagIds.plates_steel, "A", ItemIds.pneumatic_tube);
        shaped(NS, "pneumatic_tube_welded", ItemIds.pneumatic_tube, 24, rows("CRC"),
                "C", TagIds.plates_welded_copper, "R", TagIds.ingots_any_rubber);
        shaped(NS, "pole_satellite_receiver", ItemIds.pole_satellite_receiver, rows("SS ", "SCR", "SS "),
                "S", TagIds.ingots_steel, "C", ItemIds.circuit_vacuum_tube, "R", TagIds.wires_red_copper);
        shaped(NS, "pole_top", ItemIds.pole_top, rows("T T", "TRT", "BBB"),
                "T", TagIds.ingots_tungsten, "B", TagIds.ingots_beryllium, "R", TagIds.ingots_red_copper);
        shaped(NS, "powder_actinium_tiny", ItemIds.powder_actinium_tiny, 9, rows("#"), "#", ItemIds.powder_actinium);
        shaped(NS, "powder_cerium_tiny", ItemIds.powder_cerium_tiny, 9, rows("#"), "#", ItemIds.powder_cerium);
        shaped(NS, "powder_cobalt_tiny", ItemIds.powder_cobalt_tiny, 9, rows("#"), "#", ItemIds.powder_cobalt);
        shaped(NS, "powder_fire_1", ItemIds.powder_fire, 9, rows("#"), "#", ItemIds.block_red_phosphorus);
        shaped(NS, "powder_lanthanium_tiny", ItemIds.powder_lanthanium_tiny, 9, rows("#"),
                "#", ItemIds.powder_lanthanium);
        shaped(NS, "powder_lithium_tiny", ItemIds.powder_lithium_tiny, 9, rows("#"), "#", ItemIds.powder_lithium);
        shaped(NS, "powder_meteorite_tiny", ItemIds.powder_meteorite_tiny, 9, rows("#"), "#", ItemIds.powder_meteorite);
        shaped(NS, "powder_neodymium_tiny", ItemIds.powder_neodymium_tiny, 9, rows("#"), "#", ItemIds.powder_neodymium);
        shaped(NS, "powder_niobium_tiny", ItemIds.powder_niobium_tiny, 9, rows("#"), "#", ItemIds.powder_niobium);
        shaped(NS, "powder_steel_tiny", ItemIds.powder_steel_tiny, 9, rows("#"), "#", ItemIds.powder_steel);
        shaped(NS, "pwr_fuel_bfb_am_mix", ItemIds.pwr_fuel_bfb_am_mix, rows("NFN", "NIN", "NBN"),
                "F", ItemIds.billet_am_mix, "I", ItemIds.plate_polymer, "B", TagIds.billets_bismuth,
                "N", ItemIds.nugget_plutonium_fuel);
        shaped(NS, "pwr_fuel_bfb_pu241", ItemIds.pwr_fuel_bfb_pu241, rows("NFN", "NIN", "NBN"),
                "F", TagIds.billets_pu241, "I", ItemIds.plate_polymer, "B", TagIds.billets_bismuth,
                "N", ItemIds.nugget_uranium_fuel);
        shaped(NS, "pwr_fuel_hea242", ItemIds.pwr_fuel_hea242, rows("F", "I", "F"),
                "F", TagIds.billets_am242, "I", ItemIds.plate_polymer);
        shaped(NS, "pwr_fuel_hen237", ItemIds.pwr_fuel_hen237, rows("F", "I", "F"),
                "F", TagIds.billets_neptunium, "I", ItemIds.plate_polymer);
        shaped(NS, "pwr_fuel_hep239", ItemIds.pwr_fuel_hep239, rows("F", "I", "F"),
                "F", TagIds.billets_pu239, "I", ItemIds.plate_polymer);
        shaped(NS, "pwr_fuel_hep241", ItemIds.pwr_fuel_hep241, rows("F", "I", "F"),
                "F", TagIds.billets_pu241, "I", ItemIds.plate_polymer);
        shaped(NS, "pwr_fuel_hes326", ItemIds.pwr_fuel_hes326, rows("F", "I", "F"),
                "F", TagIds.billets_schrabidium, "I", ItemIds.plate_polymer);
        shaped(NS, "pwr_fuel_hes327", ItemIds.pwr_fuel_hes327, rows("F", "I", "F"),
                "F", TagIds.billets_solinium, "I", ItemIds.plate_polymer);
        shaped(NS, "pwr_fuel_heu233", ItemIds.pwr_fuel_heu233, rows("F", "I", "F"),
                "F", TagIds.billets_u233, "I", ItemIds.plate_polymer);
        shaped(NS, "pwr_fuel_heu235", ItemIds.pwr_fuel_heu235, rows("F", "I", "F"),
                "F", TagIds.billets_u235, "I", ItemIds.plate_polymer);
        shaped(NS, "pwr_fuel_mea", ItemIds.pwr_fuel_mea, rows("F", "I", "F"),
                "F", ItemIds.billet_am_mix, "I", ItemIds.plate_polymer);
        shaped(NS, "pwr_fuel_men", ItemIds.pwr_fuel_men, rows("F", "I", "F"),
                "F", ItemIds.billet_neptunium_fuel, "I", ItemIds.plate_polymer);
        shaped(NS, "pwr_fuel_mep", ItemIds.pwr_fuel_mep, rows("F", "I", "F"),
                "F", ItemIds.billet_pu_mix, "I", ItemIds.plate_polymer);
        shaped(NS, "pwr_fuel_meu", ItemIds.pwr_fuel_meu, rows("F", "I", "F"),
                "F", ItemIds.billet_uranium_fuel, "I", ItemIds.plate_polymer);
        shaped(NS, "pwr_fuel_mox", ItemIds.pwr_fuel_mox, rows("F", "I", "F"),
                "F", ItemIds.billet_mox_fuel, "I", ItemIds.plate_polymer);
        shaped(NS, "radio_autocal", ItemIds.radio_autocal, rows("TAR", "PAP", "PAP"),
                "T", ItemIds.radio_torch_sender, "R", ItemIds.radio_torch_receiver, "A", ItemIds.circuit_analog,
                "P", TagIds.plates_cast_copper);
        shaped(NS, "radio_telex", ItemIds.radio_telex, 2, rows("SCR", "W#W", "WWW"),
                "S", ItemIds.radio_torch_sender, "C", ItemIds.crt_display, "R", ItemIds.radio_torch_receiver,
                "W", TagIds.minecraft_planks, "#", ItemIds.circuit_analog);
        shaped(NS, "radio_torch_controller", ItemIds.radio_torch_controller, 4, rows(" G ", "IRI"),
                "G", VanillaIds.glowstone_dust, "R", VanillaIds.redstone_torch, "I", ItemIds.circuit_chip);
        shaped(NS, "radio_torch_counter", ItemIds.radio_torch_counter, 4, rows("G", "R", "I"),
                "G", VanillaIds.glowstone_dust, "R", VanillaIds.redstone_torch, "I", ItemIds.circuit_vacuum_tube);
        shaped(NS, "radio_torch_logic", ItemIds.radio_torch_logic, 4, rows("G", "R", "I"),
                "G", VanillaIds.glowstone_dust, "R", VanillaIds.redstone_torch, "I", ItemIds.circuit_chip);
        shaped(NS, "radio_torch_reader", ItemIds.radio_torch_reader, 4, rows(" G ", "IRI"),
                "G", VanillaIds.glowstone_dust, "R", VanillaIds.redstone_torch, "I", ItemIds.circuit_vacuum_tube);
        shaped(NS, "radio_torch_receiver", ItemIds.radio_torch_receiver, 4, rows("G", "R", "I"),
                "G", VanillaIds.glowstone_dust, "R", VanillaIds.redstone_torch, "I", TagIds.ingots_iron);
        shaped(NS, "radio_torch_sender", ItemIds.radio_torch_sender, 4, rows("G", "R", "I"),
                "G", VanillaIds.glowstone_dust, "R", VanillaIds.redstone_torch, "I", TagIds.gems_quartz);
        shaped(NS, "rbmk_absorber", ItemIds.rbmk_absorber, rows("GGG", "GRG", "GGG"),
                "G", TagIds.ingots_boron, "R", ItemIds.rbmk_blank);
        shaped(NS, "rbmk_blank", ItemIds.rbmk_blank, rows("RRR", "R R", "RRR"), "R", ItemIds.deco_rbmk);
        shaped(NS, "rbmk_blank_1", ItemIds.rbmk_blank, rows("RRR", "R R", "RRR"), "R", ItemIds.deco_rbmk_smooth);
        shaped(NS, "rbmk_blank_from_deco", ItemIds.rbmk_blank, rows("RRR", "R R", "RRR"), "R", ItemIds.deco_rbmk);
        shaped(NS, "rbmk_blank_from_deco_smooth", ItemIds.rbmk_blank, rows("RRR", "R R", "RRR"),
                "R", ItemIds.deco_rbmk_smooth);
        shaped(NS, "rbmk_boiler", ItemIds.rbmk_boiler, rows("CPC", "CRC", "CPC"),
                "C", TagIds.pipes_copper, "P", TagIds.shells_copper, "R", ItemIds.rbmk_blank);
        shaped(NS, "rbmk_console", ItemIds.rbmk_console, rows("BBB", "DGD", "DCD"),
                "B", TagIds.ingots_boron, "D", ItemIds.deco_rbmk, "G", TagIds.glass_panes, "C", ItemIds.circuit_analog);
        shaped(NS, "rbmk_control", ItemIds.rbmk_control, rows(" B ", "GRG", " B "),
                "G", TagIds.ingots_graphite, "B", ItemIds.motor, "R", ItemIds.rbmk_absorber);
        shaped(NS, "rbmk_control_auto", ItemIds.rbmk_control_auto, rows("C", "R", "D"),
                "C", ItemIds.circuit_advanced, "R", ItemIds.rbmk_control, "D", ItemIds.crt_display);
        shaped(NS, "rbmk_control_mod", ItemIds.rbmk_control_mod, rows("BGB", "GRG", "BGB"),
                "G", TagIds.storage_blocks_graphite, "R", ItemIds.rbmk_control, "B", ItemIds.nugget_bismuth);
        shaped(NS, "rbmk_cooler", ItemIds.rbmk_cooler, rows("IGI", "GCG", "IGI"),
                "C", ItemIds.rbmk_blank, "I", ItemIds.plate_polymer, "G", ItemIds.steel_grate);
        shaped(NS, "rbmk_crane_console", ItemIds.rbmk_crane_console, rows("BCD", "DDD"),
                "B", TagIds.ingots_boron, "D", ItemIds.deco_rbmk, "C", ItemIds.circuit_analog);
        shaped(NS, "rbmk_fuel_empty", ItemIds.rbmk_fuel_empty, rows("ZRZ", "Z Z", "ZRZ"),
                "Z", TagIds.ingots_zirconium, "R", ItemIds.rod_quad_empty);
        shaped(NS, "rbmk_fuel_rod", ItemIds.rbmk_fuel_rod, rows("C", "R", "C"),
                "C", TagIds.shells_steel, "R", ItemIds.rbmk_blank);
        shaped(NS, "rbmk_heater", ItemIds.rbmk_heater, rows("CIC", "PRP", "CIC"),
                "C", TagIds.pipes_copper, "P", TagIds.shells_steel, "R", ItemIds.rbmk_blank,
                "I", TagIds.ingots_any_plastic);
        shaped(NS, "rbmk_lid", ItemIds.rbmk_lid, 4, rows("PPP", "CCC", "PPP"),
                "P", TagIds.plates_steel, "C", ItemIds.concrete_asbestos);
        shaped(NS, "rbmk_lid_glass", ItemIds.rbmk_lid_glass, 4, rows("LLL", "BBB", "P P"),
                "P", TagIds.plates_steel, "L", ItemIds.glass_lead, "B", ItemIds.glass_boron);
        shaped(NS, "rbmk_lid_glass_1", ItemIds.rbmk_lid_glass, 4, rows("BBB", "LLL", "P P"),
                "P", TagIds.plates_steel, "L", ItemIds.glass_lead, "B", ItemIds.glass_boron);
        shaped(NS, "rbmk_lid_glass_boron_over_lead", ItemIds.rbmk_lid_glass, 4, rows("BBB", "LLL", "P P"),
                "L", ItemIds.glass_lead, "B", ItemIds.glass_boron, "P", TagIds.plates_steel);
        shaped(NS, "rbmk_moderator", ItemIds.rbmk_moderator, rows(" G ", "GRG", " G "),
                "G", TagIds.storage_blocks_graphite, "R", ItemIds.rbmk_blank);
        shaped(NS, "rbmk_outgasser", ItemIds.rbmk_outgasser, rows("GHG", "GRG", "GTG"),
                "G", ItemIds.steel_grate, "H", VanillaIds.hopper, "T", ItemIds.tank_steel, "R", ItemIds.rbmk_blank);
        shaped(NS, "rbmk_reflector", ItemIds.rbmk_reflector, rows("GGG", "GRG", "GGG"),
                "G", ItemIds.neutron_reflector, "R", ItemIds.rbmk_blank);
        shaped(NS, "rbmk_steam_inlet", ItemIds.rbmk_steam_inlet, rows("SCS", "CBC", "SCS"),
                "S", TagIds.ingots_steel, "C", TagIds.plates_iron, "B", ItemIds.tank_steel);
        shaped(NS, "rbmk_steam_outlet", ItemIds.rbmk_steam_outlet, rows("SCS", "CBC", "SCS"),
                "S", TagIds.ingots_steel, "C", TagIds.plates_copper, "B", ItemIds.tank_steel);
        shaped(NS, "rbmk_storage", ItemIds.rbmk_storage, rows("C", "R", "C"),
                "C", ItemIds.crate_steel, "R", ItemIds.rbmk_blank);
        shaped(NS, "rbmk_tool", ItemIds.rbmk_tool, rows(" A ", " IA", "I  "),
                "A", TagIds.ingots_lead, "I", TagIds.ingots_iron);
        shaped(NS, "reacher", ItemIds.reacher, rows("BIB", "P P", "B B"),
                "B", TagIds.bolts_tungsten, "I", TagIds.ingots_tungsten, "P", TagIds.ingots_any_rubber);
        shaped(NS, "reactor_sensor", ItemIds.reactor_sensor, rows("WPW", "CMC", "PPP"),
                "W", TagIds.wires_tungsten, "P", TagIds.plates_lead, "C", ItemIds.circuit_basic, "M", ItemIds.magnetron);
        shaped(NS, "red_cable", ItemIds.red_cable, 16, rows(" W ", "RRR", " W "),
                "W", ItemIds.plate_polymer, "R", TagIds.wires_red_copper);
        shaped(NS, "red_cable_paintable", ItemIds.red_cable_paintable, 16, rows("WRW", "RIR", "WRW"),
                "W", TagIds.plates_steel, "I", TagIds.ingots_red_copper, "R", TagIds.wires_red_copper);
        shaped(NS, "red_connector", ItemIds.red_connector, 4, rows("C", "I", "S"),
                "C", ItemIds.coil_copper, "I", ItemIds.plate_polymer, "S", TagIds.ingots_steel);
        shaped(NS, "red_connector_super", ItemIds.red_connector_super, 2, rows("CCC", "III", " S "),
                "C", ItemIds.coil_copper, "I", ItemIds.plate_polymer, "S", TagIds.ingots_any_resistant_alloy);
        shaped(NS, "red_pylon", ItemIds.red_pylon, 4, rows("CWC", "PWP", " S "),
                "C", ItemIds.coil_copper, "W", TagIds.minecraft_planks, "P", ItemIds.plate_polymer,
                "S", TagIds.cobblestones);
        shaped(NS, "red_pylon_medium_steel", ItemIds.red_pylon_medium_steel, 2, rows("CCW", "IIW", "  S"),
                "C", ItemIds.coil_copper, "W", TagIds.pipes_steel, "I", ItemIds.plate_polymer, "S", TagIds.cobblestones);
        shaped(NS, "red_pylon_medium_wood", ItemIds.red_pylon_medium_wood, 2, rows("CCW", "IIW", "  S"),
                "C", ItemIds.coil_copper, "W", TagIds.minecraft_planks, "I", ItemIds.plate_polymer,
                "S", TagIds.cobblestones);
        shaped(NS, "red_pylon_steel", ItemIds.red_pylon_steel, 4, rows("CWC", "PWP", " S "),
                "C", ItemIds.coil_copper, "W", TagIds.pipes_steel, "P", ItemIds.plate_polymer, "S", TagIds.cobblestones);
        shaped(NS, "refueler", ItemIds.refueler, rows("SS", "HC", "SS"),
                "S", TagIds.plates_titanium, "H", ItemIds.part_generic_piston_hydraulic, "C", ItemIds.circuit_basic);
        shaped(NS, "reinforced_brick_2", ItemIds.reinforced_brick, 4, rows("FBF", "BFB", "FBF"),
                "F", VanillaIds.iron_bars, "B", ItemIds.brick_concrete);
        shaped(NS, "reinforced_brick_slab", ItemIds.reinforced_brick_slab, 6, rows("###"),
                "#", ItemIds.reinforced_brick);
        shaped(NS, "reinforced_brick_stairs", ItemIds.reinforced_brick_stairs, 8, rows("#  ", "## ", "###"),
                "#", ItemIds.reinforced_brick);
        shaped(NS, "reinforced_brick_stairs_1", ItemIds.reinforced_brick_stairs, 4, rows("#  ", "## ", "###"),
                "#", ItemIds.reinforced_brick_slab);
        shaped(NS, "reinforced_ducrete", ItemIds.reinforced_ducrete, 4, rows("DSD", "SUS", "DSD"),
                "D", ItemIds.brick_ducrete, "S", TagIds.plates_steel, "U", TagIds.billets_u238);
        shaped(NS, "reinforced_glass", ItemIds.reinforced_glass, 4, rows("FBF", "BFB", "FBF"),
                "F", VanillaIds.iron_bars, "B", VanillaIds.glass);
        shaped(NS, "reinforced_glass_pane", ItemIds.reinforced_glass_pane, 16, rows("GGG", "GGG"),
                "G", ItemIds.reinforced_glass);
        shaped(NS, "reinforced_laminate_pane", ItemIds.reinforced_laminate_pane, 16, rows("LLL", "LLL"),
                "L", ItemIds.reinforced_laminate);
        shaped(NS, "reinforced_lamp_off", ItemIds.reinforced_lamp_off, rows("FFF", "FBF", "FFF"),
                "F", VanillaIds.iron_bars, "B", VanillaIds.redstone_lamp);
        shaped(NS, "reinforced_light", ItemIds.reinforced_light, rows("FFF", "FBF", "FFF"),
                "F", VanillaIds.iron_bars, "B", VanillaIds.glowstone);
    }

    private static void shaped7() {
        shaped(NS, "reinforced_sand_2", ItemIds.reinforced_sand, 4, rows("FBF", "BFB", "FBF"),
                "F", VanillaIds.iron_bars, "B", VanillaIds.sandstone);
        shaped(NS, "reinforced_sand_slab", ItemIds.reinforced_sand_slab, 6, rows("###"), "#", ItemIds.reinforced_sand);
        shaped(NS, "reinforced_sand_stairs", ItemIds.reinforced_sand_stairs, 8, rows("#  ", "## ", "###"),
                "#", ItemIds.reinforced_sand);
        shaped(NS, "reinforced_sand_stairs_1", ItemIds.reinforced_sand_stairs, 4, rows("#  ", "## ", "###"),
                "#", ItemIds.reinforced_sand_slab);
        shaped(NS, "reinforced_stone_2", ItemIds.reinforced_stone, 4, rows("FBF", "BFB", "FBF"),
                "F", VanillaIds.cobblestone, "B", VanillaIds.stone);
        shaped(NS, "reinforced_stone_slab", ItemIds.reinforced_stone_slab, 6, rows("###"),
                "#", ItemIds.reinforced_stone);
        shaped(NS, "reinforced_stone_stairs", ItemIds.reinforced_stone_stairs, 8, rows("#  ", "## ", "###"),
                "#", ItemIds.reinforced_stone);
        shaped(NS, "reinforced_stone_stairs_1", ItemIds.reinforced_stone_stairs, 4, rows("#  ", "## ", "###"),
                "#", ItemIds.reinforced_stone_slab);
        shaped(NS, "ring_starmetal", ItemIds.ring_starmetal, rows(" S ", "S S", " S "), "S", TagIds.ingots_starmetal);
        shaped(NS, "rod_empty", ItemIds.rod_empty, 16, rows("SSS", "L L", "SSS"),
                "S", TagIds.plates_steel, "L", TagIds.plates_lead);
        shaped(NS, "rod_zirnox_empty", ItemIds.rod_zirnox_empty, 4, rows("Z Z", "ZBZ", "Z Z"),
                "Z", TagIds.nuggets_zirconium, "B", TagIds.ingots_beryllium);
        shaped(NS, "rune_blank", ItemIds.rune_blank, rows("PSP", "SDS", "PSP"),
                "P", ItemIds.powder_magic, "S", TagIds.ingots_starmetal, "D", ItemIds.circuit_bismoid);
        shaped(NS, "safety_fuse", ItemIds.safety_fuse, 8, rows("SSS", "SGS", "SSS"),
                "S", VanillaIds.string, "G", VanillaIds.gunpowder);
        shaped(NS, "sawblade", ItemIds.sawblade, rows("III", "ICI", "III"),
                "I", TagIds.plates_steel, "C", TagIds.ingots_iron);
        shaped(NS, "schrabidium_boots", ItemIds.schrabidium_boots, rows("EPE", "ESE"),
                "E", TagIds.ingots_schrabidium, "S", ItemIds.starmetal_boots, "P", ItemIds.pellet_charged);
        shaped(NS, "schrabidium_helmet", ItemIds.schrabidium_helmet, rows("EEE", "ESE", " P "),
                "E", TagIds.ingots_schrabidium, "S", ItemIds.starmetal_helmet, "P", ItemIds.pellet_charged);
        shaped(NS, "schrabidium_legs", ItemIds.schrabidium_legs, rows("EEE", "ESE", "EPE"),
                "E", TagIds.ingots_schrabidium, "S", ItemIds.starmetal_legs, "P", ItemIds.pellet_charged);
        shaped(NS, "schrabidium_plate", ItemIds.schrabidium_plate, rows("ESE", "EPE", "EEE"),
                "E", TagIds.ingots_schrabidium, "S", ItemIds.starmetal_plate, "P", ItemIds.pellet_charged);
        shaped(NS, "screwdriver", ItemIds.screwdriver, rows("  I", " I ", "S  "),
                "S", TagIds.ingots_steel, "I", TagIds.ingots_iron);
        shaped(NS, "security_boots", ItemIds.security_boots, rows("P P", "I I"),
                "P", TagIds.plates_steel, "I", TagIds.ingots_any_rubber);
        shaped(NS, "security_helmet", ItemIds.security_helmet, rows("SSS", "IGI"),
                "S", TagIds.plates_steel, "I", TagIds.ingots_any_rubber, "G", TagIds.glass_panes);
        shaped(NS, "security_legs", ItemIds.security_legs, rows("IWI", "K K", "W W"),
                "K", ItemIds.plate_kevlar, "I", TagIds.ingots_any_plastic, "W", TagIds.minecraft_wool);
        shaped(NS, "security_plate", ItemIds.security_plate, rows("KWK", "IKI", "WKW"),
                "K", ItemIds.plate_kevlar, "I", TagIds.ingots_any_plastic, "W", TagIds.minecraft_wool);
        shaped(NS, "seg_10", ItemIds.seg_10, rows("P", "S", "B"),
                "P", TagIds.plates_aluminium, "S", ItemIds.steel_scaffold, "B", ItemIds.steel_beam);
        shaped(NS, "seg_15", ItemIds.seg_15, rows("PP", "SS", "BB"),
                "P", TagIds.plates_titanium, "S", ItemIds.steel_scaffold, "B", ItemIds.steel_beam);
        shaped(NS, "seg_20", ItemIds.seg_20, rows("PGP", "SSS", "BBB"),
                "P", TagIds.plates_steel, "G", TagIds.plates_gold, "S", ItemIds.steel_scaffold, "B", ItemIds.steel_beam);
        shaped(NS, "semtex", ItemIds.semtex, rows("DDD", "DSD", "DDD"),
                "D", ItemIds.stick_semtex, "S", ItemIds.safety_fuse);
        shaped(NS, "shimmer_axe_head", ItemIds.shimmer_axe_head, rows("PII", "PBB", "PII"),
                "P", TagIds.plates_steel, "B", TagIds.storage_blocks_desh, "I", TagIds.ingots_tungsten);
        shaped(NS, "shimmer_handle", ItemIds.shimmer_handle, rows("GP", "GP", "GP"),
                "G", TagIds.plates_gold, "P", TagIds.ingots_any_plastic);
        shaped(NS, "shimmer_head", ItemIds.shimmer_head, rows("SSS", "DTD", "SSS"),
                "S", TagIds.ingots_steel, "D", TagIds.storage_blocks_desh, "T", TagIds.storage_blocks_tungsten);
        shaped(NS, "singularity_counter_resonant", ItemIds.singularity_counter_resonant, rows("CTC", "TST", "CTC"),
                "C", TagIds.plates_combine_steel, "T", TagIds.ingots_magnetized_tungsten, "S", ItemIds.singularity);
        shaped(NS, "singularity_spark", ItemIds.singularity_spark, rows("XAX", "BCB", "XAX"),
                "X", ItemIds.plate_dineutronium, "A", ItemIds.singularity_counter_resonant,
                "B", ItemIds.singularity_super_heated, "C", ItemIds.black_hole);
        shaped(NS, "singularity_spark_1", ItemIds.singularity_spark, rows("XBX", "ACA", "XBX"),
                "X", ItemIds.plate_dineutronium, "A", ItemIds.singularity_counter_resonant,
                "B", ItemIds.singularity_super_heated, "C", ItemIds.black_hole);
        shaped(NS, "singularity_super_heated", ItemIds.singularity_super_heated, rows("CTC", "TST", "CTC"),
                "C", TagIds.plates_saturnite, "T", ItemIds.powder_power, "S", ItemIds.singularity);
        shaped(NS, "siphon", ItemIds.siphon, rows(" GR", " GR", " G "),
                "G", VanillaIds.glass, "R", TagIds.ingots_any_rubber);
        shaped(NS, "solar_mirror", ItemIds.solar_mirror, 3, rows("AAA", " B ", "SSS"),
                "A", TagIds.plates_aluminium, "B", ItemIds.steel_beam, "S", TagIds.ingots_steel);
        shaped(NS, "solid_fuel_presto", ItemIds.solid_fuel_presto, rows(" P ", "SRS", " P "),
                "P", VanillaIds.paper, "S", ItemIds.solid_fuel, "R", TagIds.dusts_redstone);
        shaped(NS, "solid_fuel_presto_bf", ItemIds.solid_fuel_presto_bf, rows(" P ", "SRS", " P "),
                "P", VanillaIds.paper, "S", ItemIds.solid_fuel_bf, "R", TagIds.dusts_redstone);
        shaped(NS, "sphere_steel", ItemIds.sphere_steel, rows("PIP", "I I", "PIP"),
                "P", TagIds.plates_steel, "I", TagIds.ingots_steel);
        shaped(NS, "spikes", ItemIds.spikes, 4, rows("BBB", "BBB", "TTT"),
                "B", TagIds.bolts_steel, "T", TagIds.ingots_steel);
        shaped(NS, "spotlight_fluoro", ItemIds.spotlight_fluoro, 8, rows("G", "M", "A"),
                "G", TagIds.glass_panes, "M", ItemIds.nugget_mercury, "A", TagIds.plates_aluminium);
        shaped(NS, "spotlight_halogen", ItemIds.spotlight_halogen, 8, rows("G", "B", "S"),
                "G", TagIds.glass_panes, "B", ItemIds.powder_bromine, "S", TagIds.plates_steel);
        shaped(NS, "spotlight_incandescent", ItemIds.spotlight_incandescent, 8, rows("G", "T", "I"),
                "G", TagIds.glass_panes, "T", TagIds.wires_tungsten, "I", TagIds.ingots_iron);
        shaped(NS, "stamp_desh_flat", ItemIds.stamp_desh_flat, rows("BDB", "DSD", "BDB"),
                "B", VanillaIds.brick, "D", TagIds.ingots_desh, "S", TagIds.ingots_ferrouranium);
        shaped(NS, "stamp_desh_flat_1", ItemIds.stamp_desh_flat, rows("BDB", "DSD", "BDB"),
                "B", VanillaIds.nether_brick, "D", TagIds.ingots_desh, "S", TagIds.ingots_ferrouranium);
        shaped(NS, "stamp_iron_flat", ItemIds.stamp_iron_flat, rows("III", "SSS"),
                "I", VanillaIds.brick, "S", TagIds.ingots_iron);
        shaped(NS, "stamp_iron_flat_1", ItemIds.stamp_iron_flat, rows("III", "SSS"),
                "I", VanillaIds.nether_brick, "S", TagIds.ingots_iron);
        shaped(NS, "stamp_obsidian_flat", ItemIds.stamp_obsidian_flat, rows("III", "SSS"),
                "I", VanillaIds.brick, "S", VanillaIds.obsidian);
        shaped(NS, "stamp_obsidian_flat_1", ItemIds.stamp_obsidian_flat, rows("III", "SSS"),
                "I", VanillaIds.nether_brick, "S", VanillaIds.obsidian);
        shaped(NS, "stamp_steel_flat", ItemIds.stamp_steel_flat, rows("III", "SSS"),
                "I", VanillaIds.brick, "S", TagIds.ingots_steel);
        shaped(NS, "stamp_steel_flat_1", ItemIds.stamp_steel_flat, rows("III", "SSS"),
                "I", VanillaIds.nether_brick, "S", TagIds.ingots_steel);
        shaped(NS, "stamp_stone_flat", ItemIds.stamp_stone_flat, rows("III", "SSS"),
                "I", VanillaIds.brick, "S", VanillaIds.stone);
        shaped(NS, "stamp_stone_flat_1", ItemIds.stamp_stone_flat, rows("III", "SSS"),
                "I", VanillaIds.nether_brick, "S", VanillaIds.stone);
        shaped(NS, "stamp_titanium_flat", ItemIds.stamp_titanium_flat, rows("III", "SSS"),
                "I", VanillaIds.brick, "S", TagIds.ingots_titanium);
        shaped(NS, "stamp_titanium_flat_1", ItemIds.stamp_titanium_flat, rows("III", "SSS"),
                "I", VanillaIds.nether_brick, "S", TagIds.ingots_titanium);
        shaped(NS, "starmetal_boots", ItemIds.starmetal_boots, rows("E E", "ECE"),
                "E", TagIds.ingots_starmetal, "C", ItemIds.cobalt_boots);
        shaped(NS, "starmetal_helmet", ItemIds.starmetal_helmet, rows("EEE", "ECE"),
                "E", TagIds.ingots_starmetal, "C", ItemIds.cobalt_helmet);
        shaped(NS, "starmetal_legs", ItemIds.starmetal_legs, rows("EEE", "ECE", "E E"),
                "E", TagIds.ingots_starmetal, "C", ItemIds.cobalt_legs);
        shaped(NS, "starmetal_plate", ItemIds.starmetal_plate, rows("ECE", "EEE", "EEE"),
                "E", TagIds.ingots_starmetal, "C", ItemIds.cobalt_plate);
        shaped(NS, "steel_beam", ItemIds.steel_beam, 8, rows("S", "S", "S"), "S", TagIds.ingots_steel);
        shaped(NS, "steel_beam_1", ItemIds.steel_beam, 8, rows("S", "S", "S"), "S", ItemIds.steel_scaffold);
        shaped(NS, "steel_boots", ItemIds.steel_boots, rows("X X", "X X"), "X", TagIds.ingots_steel);
        shaped(NS, "steel_grate", ItemIds.steel_grate, 4, rows("SS", "SS"), "S", ItemIds.steel_beam);
        shaped(NS, "steel_grate_from_wide", ItemIds.steel_grate, rows("SS"), "S", ItemIds.steel_grate_wide);
        shaped(NS, "steel_grate_wide", ItemIds.steel_grate_wide, 4, rows("SS"), "S", ItemIds.steel_grate);
        shaped(NS, "steel_helmet", ItemIds.steel_helmet, rows("XXX", "X X"), "X", TagIds.ingots_steel);
        shaped(NS, "steel_legs", ItemIds.steel_legs, rows("XXX", "X X", "X X"), "X", TagIds.ingots_steel);
        shaped(NS, "steel_plate", ItemIds.steel_plate, rows("X X", "XXX", "XXX"), "X", TagIds.ingots_steel);
        shaped(NS, "steel_poles", ItemIds.steel_poles, 16, rows("S S", "SSS", "S S"), "S", TagIds.ingots_steel);
        shaped(NS, "steel_roof", ItemIds.steel_roof, 2, rows("SSS"), "S", TagIds.ingots_steel);
        shaped(NS, "steel_scaffold", ItemIds.steel_scaffold, 8, rows("SSS", " S ", "SSS"), "S", TagIds.ingots_steel);
        shaped(NS, "steel_scaffold_1", ItemIds.steel_scaffold, 8, rows("SSS", "SDS", "SSS"),
                "S", ItemIds.steel_scaffold, "D", VanillaIds.gray_dye);
        shaped(NS, "steel_wall", ItemIds.steel_wall, 4, rows("SSS", "SSS"), "S", TagIds.ingots_steel);
        shaped(NS, "sulfur_1", ItemIds.sulfur, 9, rows("#"), "#", ItemIds.block_sulfur);
        shaped(NS, "syringe_antidote", ItemIds.syringe_antidote, 6, rows("SSS", "PMP", "SSS"),
                "S", ItemIds.syringe_empty, "P", VanillaIds.pumpkin_seeds, "M", VanillaIds.milk_bucket);
        shaped(NS, "syringe_antidote_1", ItemIds.syringe_antidote, 6, rows("SPS", "SMS", "SPS"),
                "S", ItemIds.syringe_empty, "P", VanillaIds.pumpkin_seeds, "M", VanillaIds.milk_bucket);
        shaped(NS, "syringe_antidote_2", ItemIds.syringe_antidote, 6, rows("SSS", "PMP", "SSS"),
                "S", ItemIds.syringe_empty, "P", VanillaIds.pumpkin_seeds, "M", VanillaIds.sugar_cane);
        shaped(NS, "syringe_antidote_3", ItemIds.syringe_antidote, 6, rows("SPS", "SMS", "SPS"),
                "S", ItemIds.syringe_empty, "P", VanillaIds.pumpkin_seeds, "M", VanillaIds.sugar_cane);
        shaped(NS, "syringe_awesome", ItemIds.syringe_awesome, rows("SPS", "NCN", "SPS"),
                "C", ItemIds.syringe_empty, "S", TagIds.dusts_sulfur, "P", TagIds.nuggets_pu239,
                "N", TagIds.nuggets_pu238);
        shaped(NS, "syringe_awesome_1", ItemIds.syringe_awesome, rows("SNS", "PCP", "SNS"),
                "C", ItemIds.syringe_empty, "S", TagIds.dusts_sulfur, "P", TagIds.nuggets_pu239,
                "N", TagIds.nuggets_pu238);
        shaped(NS, "syringe_metal_empty", ItemIds.syringe_metal_empty, 6, rows("P", "C", "B"),
                "B", VanillaIds.iron_bars, "C", ItemIds.rod_empty, "P", TagIds.plates_iron);
        shaped(NS, "syringe_metal_medx", ItemIds.syringe_metal_medx, rows(" N ", "NSN", " N "),
                "N", VanillaIds.quartz, "S", ItemIds.syringe_metal_empty);
        shaped(NS, "syringe_metal_psycho", ItemIds.syringe_metal_psycho, rows(" N ", "NSN", " N "),
                "N", VanillaIds.glowstone_dust, "S", ItemIds.syringe_metal_empty);
        shaped(NS, "syringe_metal_stimpak", ItemIds.syringe_metal_stimpak, rows(" N ", "NSN", " N "),
                "N", VanillaIds.nether_wart, "S", ItemIds.syringe_metal_empty);
        shaped(NS, "syringe_poison", ItemIds.syringe_poison, rows("SLS", "LCL", "SLS"),
                "C", ItemIds.syringe_empty, "S", VanillaIds.spider_eye, "L", TagIds.dusts_lead);
        shaped(NS, "syringe_poison_1", ItemIds.syringe_poison, rows("SLS", "LCL", "SLS"),
                "C", ItemIds.syringe_empty, "S", VanillaIds.spider_eye, "L", ItemIds.powder_poison);
        shaped(NS, "t51_boots", ItemIds.t51_boots, rows("P P", "PBP"),
                "P", ItemIds.plate_armor_titanium, "B", ItemIds.titanium_boots);
        shaped(NS, "t51_helmet", ItemIds.t51_helmet, rows("PPC", "PBP", "IXI"),
                "P", ItemIds.plate_armor_titanium, "C", ItemIds.circuit_basic, "I", TagIds.ingots_any_rubber,
                "X", ItemIds.gas_mask_m65, "B", ItemIds.titanium_helmet);
        shaped(NS, "t51_legs", ItemIds.t51_legs, rows("MPM", "PBP", "P P"),
                "M", ItemIds.motor, "P", ItemIds.plate_armor_titanium, "B", ItemIds.titanium_legs);
        shaped(NS, "t51_plate", ItemIds.t51_plate, rows("MPM", "TBT", "PPP"),
                "M", ItemIds.motor, "P", ItemIds.plate_armor_titanium, "T", ItemIds.gas_empty,
                "B", ItemIds.titanium_plate);
        shaped(NS, "tank_steel", ItemIds.tank_steel, 2, rows("STS", "S S", "STS"),
                "S", TagIds.plates_steel, "T", TagIds.plates_titanium);
        shaped(NS, "tape_recorder", ItemIds.tape_recorder, 4, rows("TST", "SSS"),
                "T", TagIds.ingots_tungsten, "S", TagIds.ingots_steel);
        shaped(NS, "template_folder", ItemIds.template_folder, rows("LPL", "BPB", "LPL"),
                "P", VanillaIds.paper, "L", TagIds.minecraft_dyes, "B", TagIds.minecraft_dyes);
        shaped(NS, "terrain_plant_item_rope_from_hemp", ItemIds.plant_item_rope, 4, rows("W", "W", "W"),
                "W", ItemIds.plant_flower_weed);
        shaped(NS, "tesla", ItemIds.tesla, rows("CCC", "PIP", "WTW"),
                "C", ItemIds.coil_copper, "I", TagIds.ingots_iron, "P", TagIds.ingots_any_plastic,
                "T", ItemIds.machine_transformer, "W", TagIds.minecraft_planks);
        shaped(NS, "tile_lab_2", ItemIds.tile_lab, 4, rows("CBC", "CBC", "CBC"),
                "C", VanillaIds.brick, "B", TagIds.ingots_asbestos);
        shaped(NS, "tile_lab_broken_2", ItemIds.tile_lab_broken, 6, rows(" C ", "C C", " C "),
                "C", ItemIds.tile_lab_cracked);
        shaped(NS, "tile_lab_broken_slab", ItemIds.tile_lab_broken_slab, 6, rows("###"), "#", ItemIds.tile_lab_broken);
        shaped(NS, "tile_lab_broken_stairs", ItemIds.tile_lab_broken_stairs, 8, rows("#  ", "## ", "###"),
                "#", ItemIds.tile_lab_broken);
        shaped(NS, "tile_lab_broken_stairs_1", ItemIds.tile_lab_broken_stairs, 4, rows("#  ", "## ", "###"),
                "#", ItemIds.tile_lab_broken_slab);
        shaped(NS, "tile_lab_cracked_2", ItemIds.tile_lab_cracked, 6, rows(" C ", "C C", " C "), "C", ItemIds.tile_lab);
        shaped(NS, "tile_lab_cracked_slab", ItemIds.tile_lab_cracked_slab, 6, rows("###"),
                "#", ItemIds.tile_lab_cracked);
        shaped(NS, "tile_lab_cracked_stairs", ItemIds.tile_lab_cracked_stairs, 8, rows("#  ", "## ", "###"),
                "#", ItemIds.tile_lab_cracked);
        shaped(NS, "tile_lab_cracked_stairs_1", ItemIds.tile_lab_cracked_stairs, 4, rows("#  ", "## ", "###"),
                "#", ItemIds.tile_lab_cracked_slab);
        shaped(NS, "tile_lab_slab", ItemIds.tile_lab_slab, 6, rows("###"), "#", ItemIds.tile_lab);
        shaped(NS, "tile_lab_stairs", ItemIds.tile_lab_stairs, 8, rows("#  ", "## ", "###"), "#", ItemIds.tile_lab);
        shaped(NS, "tile_lab_stairs_1", ItemIds.tile_lab_stairs, 4, rows("#  ", "## ", "###"),
                "#", ItemIds.tile_lab_slab);
        shaped(NS, "titanium_boots", ItemIds.titanium_boots, rows("X X", "X X"), "X", TagIds.ingots_titanium);
        shaped(NS, "titanium_helmet", ItemIds.titanium_helmet, rows("XXX", "X X"), "X", TagIds.ingots_titanium);
        shaped(NS, "titanium_legs", ItemIds.titanium_legs, rows("XXX", "X X", "X X"), "X", TagIds.ingots_titanium);
    }

    private static void shaped8() {
        shaped(NS, "titanium_plate", ItemIds.titanium_plate, rows("X X", "XXX", "XXX"), "X", TagIds.ingots_titanium);
        shaped(NS, "tnt_ntm", ItemIds.tnt_ntm, rows("DDD", "DSD", "DDD"),
                "D", ItemIds.stick_tnt, "S", ItemIds.safety_fuse);
        shaped(NS, "torch", VanillaIds.torch, 3, rows("L", "S"), "L", TagIds.gems_lignite, "S", VanillaIds.stick);
        shaped(NS, "trinitite", ItemIds.trinitite, 9, rows("#"), "#", ItemIds.block_trinitite);
        shaped(NS, "turbine_titanium", ItemIds.turbine_titanium, rows("BBB", "BSB", "BBB"),
                "B", ItemIds.blade_titanium, "S", TagIds.ingots_steel);
        shaped(NS, "turbine_tungsten", ItemIds.turbine_tungsten, rows("BBB", "BSB", "BBB"),
                "B", ItemIds.blade_tungsten, "S", TagIds.ingots_dura_steel);
        shaped(NS, "turret_chip", ItemIds.turret_chip, rows("WWW", "CPC", "WWW"),
                "W", TagIds.wires_gold, "P", TagIds.ingots_any_plastic, "C", ItemIds.circuit_advanced);
        shaped(NS, "turret_mob_filter", ItemIds.turret_mob_filter, rows("SCS", "TBT", "IRI"),
                "S", ItemIds.plate_steel, "C", VanillaIds.comparator, "T", VanillaIds.redstone_block,
                "B", ItemIds.turret_chip, "R", ItemIds.circuit_chip, "I", ItemIds.plate_iron);
        shaped(NS, "turret_sentry", ItemIds.turret_sentry, rows("PPL", " MD", " SC"),
                "P", TagIds.plates_steel, "M", ItemIds.motor, "L", TagIds.mechanisms_gunmetal,
                "S", ItemIds.steel_scaffold, "C", ItemIds.circuit_basic, "D", ItemIds.crt_display);
        shaped(NS, "upgrade_centrifuge", ItemIds.upgrade_centrifuge, rows("PHP", "PUP", "DTD"),
                "P", ItemIds.centrifuge_element, "H", VanillaIds.hopper, "U", ItemIds.upgrade_shredder,
                "D", TagIds.ingots_any_plastic, "T", ItemIds.machine_transformer);
        shaped(NS, "upgrade_ejector_1", ItemIds.upgrade_ejector_1, rows(" C ", "PUP", " C "),
                "C", ItemIds.plate_copper, "P", ItemIds.motor, "U", ItemIds.upgrade_template);
        shaped(NS, "upgrade_ejector_2", ItemIds.upgrade_ejector_2, rows(" C ", "PUP", " C "),
                "C", ItemIds.plate_gold, "P", ItemIds.motor, "U", ItemIds.upgrade_ejector_1);
        shaped(NS, "upgrade_ejector_3", ItemIds.upgrade_ejector_3, rows(" C ", "PUP", " C "),
                "C", ItemIds.plate_saturnite, "P", ItemIds.motor, "U", ItemIds.upgrade_ejector_2);
        shaped(NS, "upgrade_gc_speed", ItemIds.upgrade_gc_speed, rows("GNG", "RUR", "GMG"),
                "R", TagIds.ingots_rubber, "M", ItemIds.motor, "G", ItemIds.coil_gold, "N", TagIds.ingots_niobium,
                "U", ItemIds.upgrade_template);
        shaped(NS, "upgrade_nullifier", ItemIds.upgrade_nullifier, rows("SPS", "PUP", "SPS"),
                "S", TagIds.plates_steel, "P", ItemIds.powder_fire, "U", ItemIds.upgrade_template);
        shaped(NS, "upgrade_screm", ItemIds.upgrade_screm, rows("SUS", "SCS", "SUS"),
                "S", TagIds.plates_steel, "U", ItemIds.upgrade_template, "C", ItemIds.crystal_xen);
        shaped(NS, "upgrade_shredder", ItemIds.upgrade_shredder, rows("PHP", "CUC", "DTD"),
                "P", ItemIds.motor, "H", VanillaIds.hopper, "C", ItemIds.blades_titanium, "U", ItemIds.upgrade_smelter,
                "D", TagIds.plates_titanium, "T", ItemIds.machine_transformer);
        shaped(NS, "upgrade_smelter", ItemIds.upgrade_smelter, rows("PHP", "CUC", "DTD"),
                "P", TagIds.plates_copper, "H", VanillaIds.hopper, "C", ItemIds.coil_tungsten,
                "U", ItemIds.upgrade_template, "D", ItemIds.coil_copper, "T", ItemIds.machine_transformer);
        shaped(NS, "upgrade_stack_1", ItemIds.upgrade_stack_1, rows(" C ", "PUP", " C "),
                "C", ItemIds.circuit_vacuum_tube, "P", ItemIds.part_generic_piston_pneumatic,
                "U", ItemIds.upgrade_template);
        shaped(NS, "upgrade_stack_2", ItemIds.upgrade_stack_2, rows(" C ", "PUP", " C "),
                "C", ItemIds.circuit_capacitor, "P", ItemIds.part_generic_piston_hydraulic, "U", ItemIds.upgrade_stack_1);
        shaped(NS, "upgrade_stack_3", ItemIds.upgrade_stack_3, rows(" C ", "PUP", " C "),
                "C", ItemIds.circuit_chip, "P", ItemIds.part_generic_piston_electric, "U", ItemIds.upgrade_stack_2);
        shaped(NS, "upgrade_template", ItemIds.upgrade_template, rows("WIW", "PCP", "WIW"),
                "W", TagIds.wires_copper, "I", TagIds.plates_iron, "C", ItemIds.circuit_analog,
                "P", ItemIds.plate_polymer);
        shaped(NS, "upgrade_template_1", ItemIds.upgrade_template, rows("WIW", "PCP", "WIW"),
                "W", TagIds.wires_copper, "I", TagIds.ingots_any_plastic, "C", ItemIds.circuit_basic,
                "P", ItemIds.plate_polymer);
        shaped(NS, "vinyl_tile_large", ItemIds.vinyl_tile_large, 4, rows(" I ", "IBI", " I "),
                "I", ItemIds.plate_polymer, "B", ItemIds.brick_light);
        shaped(NS, "vinyl_tile_small", ItemIds.vinyl_tile_small, 4, rows("BB", "BB"), "B", ItemIds.vinyl_tile_large);
        shaped(NS, "watch", ItemIds.watch, rows("LYL", "EWE", "LYL"),
                "E", TagIds.ingots_euphemium, "L", VanillaIds.blue_dye, "W", VanillaIds.clock,
                "Y", ItemIds.billet_yharonite);
        shaped(NS, "watz_pellet_boron", ItemIds.watz_pellet_boron, rows(" I ", "IGI", " I "),
                "I", TagIds.ingots_boron, "G", TagIds.ingots_graphite);
        shaped(NS, "watz_pellet_du", ItemIds.watz_pellet_du, rows(" I ", "IGI", " I "),
                "I", TagIds.ingots_u238, "G", TagIds.ingots_graphite);
        shaped(NS, "watz_pellet_hen", ItemIds.watz_pellet_hen, rows(" I ", "IGI", " I "),
                "I", TagIds.ingots_neptunium, "G", TagIds.ingots_graphite);
        shaped(NS, "watz_pellet_hes", ItemIds.watz_pellet_hes, rows(" I ", "IGI", " I "),
                "I", ItemIds.ingot_hes, "G", TagIds.ingots_graphite);
        shaped(NS, "watz_pellet_lead", ItemIds.watz_pellet_lead, rows(" I ", "IGI", " I "),
                "I", TagIds.ingots_lead, "G", TagIds.ingots_graphite);
        shaped(NS, "watz_pellet_les", ItemIds.watz_pellet_les, rows(" I ", "IGI", " I "),
                "I", ItemIds.ingot_les, "G", TagIds.ingots_graphite);
        shaped(NS, "watz_pellet_mep", ItemIds.watz_pellet_mep, rows(" I ", "IGI", " I "),
                "I", ItemIds.ingot_pu_mix, "G", TagIds.ingots_graphite);
        shaped(NS, "watz_pellet_mes", ItemIds.watz_pellet_mes, rows(" I ", "IGI", " I "),
                "I", ItemIds.ingot_schrabidium_fuel, "G", TagIds.ingots_graphite);
        shaped(NS, "watz_pellet_meu", ItemIds.watz_pellet_meu, rows(" I ", "IGI", " I "),
                "I", ItemIds.ingot_uranium_fuel, "G", TagIds.ingots_graphite);
        shaped(NS, "watz_pellet_schrabidium", ItemIds.watz_pellet_schrabidium, rows(" I ", "IGI", " I "),
                "I", TagIds.ingots_schrabidium, "G", TagIds.ingots_graphite);
        shaped(NS, "wiring_red_copper", ItemIds.wiring_red_copper, rows("PPP", "PIP", "PPP"),
                "P", TagIds.plates_steel, "I", TagIds.ingots_steel);
        shaped(NS, "wood_barrier", ItemIds.wood_barrier, 8, rows("SFS", "SFS"),
                "S", TagIds.minecraft_wooden_slabs, "F", TagIds.minecraft_wooden_fences);
    }
}
