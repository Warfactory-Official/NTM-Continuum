package com.hbm.items;

import com.google.gson.JsonObject;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Tints;
import dev.golem.core.render.display.DisplaySlot;
import dev.golem.core.render.display.Pose17;
import org.joml.Matrix4f;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every item model hbm can state in code, declared through Golem Core's {@code dejson} vocabulary
 * instead of written as JSON. These 1866 declarations replaced 3375 asset files.
 *
 * <p>An item model is the most repetitive file in the mod and the most numerous: {@value #SPRITES}
 * of the items here are one sprite named after the item and nothing else, written out twice — once
 * as {@code models/item/<id>.json} for the targets that load a model directly, and once as
 * {@code items/<id>.json} for 26.1, which resolves through an item definition instead. One
 * declaration emits both.
 *
 * <p>Items whose block owns them are not here; a block's own item is declared beside the block in
 * {@link com.hbm.blocks.ModBlockModels}, so the two cannot drift apart. Neither are the items whose
 * 26.1 definition is a {@code select}, a {@code condition} or a {@code special} — a gun or an RBMK
 * column drawn from an OBJ by its own renderer says something this vocabulary deliberately cannot.
 */
public final class ModItemModels {

    private static final String NS = "hbm";

    /** How many items are the plain one-sprite case; quoted in this class's own javadoc. */
    static final int SPRITES = 1371;

    /**
     * The shape art each material-shape sprite is computed from, and the materials it is computed for.
     *
     * <p>These sprites are not files. Golem Core recolours one greyscale plate per shape onto every
     * material's own light and dark colours while the atlas is stitched, which is what the 1.12.2
     * {@code ItemAutogen} did with a mutable atlas icon and what this mod did with 187 pre-baked PNGs
     * until they were deleted. {@link com.hbm.client.AutogenSprites} declares the remap; this says the
     * same thing to the build, so the sprite check holds the model against the art it is derived from
     * instead of reporting a file that is never going to be there.
     *
     * <p>{@code bedrock_ore_fragment_bismuth} is deliberately absent: bismuth declares one flat colour
     * for both ends of its band, so a remap would hand it a silhouette. 1.7 answered that with an
     * authored sprite ({@code ItemAutogen.aot}) and so does this — it stays a file.
     */
    private static final Map<String, String[]> REMAPPED_SHAPES = new LinkedHashMap<>();

    static {
        REMAPPED_SHAPES.put("shell", new String[] {
                "titanium", "aluminium", "steel", "copper", "weaponsteel", "saturnite"});
        REMAPPED_SHAPES.put("pipe", new String[] {
                "iron", "aluminium", "steel", "lead", "copper", "dura_steel", "rubber"});
        REMAPPED_SHAPES.put("ingot_raw", new String[] {
                "neodymium", "sodium", "strontium", "borax", "slag", "redstone"});
        REMAPPED_SHAPES.put("plate_cast", new String[] {
                "iron", "gold", "titanium", "aluminium", "steel", "lead", "copper", "weaponsteel",
                "combine_steel", "dura_steel", "saturnite", "schrabidium", "desh", "tungsten", "zirconium",
                "schrabidate", "osmiridium", "starmetal", "ferrouranium", "tcalloy", "cdalloy",
                "bismuth_bronze", "arsenic_bronze"});
        REMAPPED_SHAPES.put("plate_welded", new String[] {
                "iron", "titanium", "aluminium", "steel", "copper", "combine_steel", "tungsten",
                "zirconium", "osmiridium", "tcalloy", "cdalloy"});
        REMAPPED_SHAPES.put("wire_fine", new String[] {
                "gold", "aluminium", "steel", "lead", "copper", "schrabidium", "red_copper",
                "magnetized_tungsten", "tungsten", "zirconium", "carbon"});
        REMAPPED_SHAPES.put("wire_dense", new String[] {
                "gold", "titanium", "copper", "schrabidium", "red_copper", "magnetized_tungsten",
                "tungsten", "niobium", "neodymium", "schrabidate", "starmetal", "bscco", "dineutronium"});
        REMAPPED_SHAPES.put("bolt", new String[] {
                "steel", "lead", "dura_steel", "tungsten"});
        REMAPPED_SHAPES.put("part_barrel_light", new String[] {
                "steel", "gunmetal", "weaponsteel", "dura_steel", "saturnite", "desh", "tcalloy", "cdalloy",
                "bismuth_bronze", "arsenic_bronze"});
        REMAPPED_SHAPES.put("part_barrel_heavy", new String[] {
                "steel", "gunmetal", "weaponsteel", "dura_steel", "saturnite", "desh", "ferrouranium",
                "tcalloy", "cdalloy"});
        REMAPPED_SHAPES.put("part_receiver_light", new String[] {
                "steel", "gunmetal", "weaponsteel", "dura_steel", "saturnite", "desh", "tcalloy", "cdalloy",
                "bismuth_bronze", "arsenic_bronze"});
        REMAPPED_SHAPES.put("part_receiver_heavy", new String[] {
                "gunmetal", "weaponsteel", "dura_steel", "saturnite", "ferrouranium", "tcalloy", "cdalloy",
                "bismuth_bronze", "arsenic_bronze"});
        REMAPPED_SHAPES.put("part_mechanism", new String[] {
                "gunmetal", "weaponsteel", "saturnite"});
        REMAPPED_SHAPES.put("part_stock", new String[] {
                "wood", "gunmetal", "weaponsteel", "saturnite", "desh", "polymer", "bakelite", "pc", "pvc"});
        REMAPPED_SHAPES.put("part_grip", new String[] {
                "wood", "steel", "gunmetal", "weaponsteel", "dura_steel", "saturnite", "desh", "polymer",
                "bakelite", "rubber", "pc", "pvc", "bone"});
        REMAPPED_SHAPES.put("bedrock_ore_fragment", new String[] {
                "iron", "gold", "titanium", "aluminium", "lead", "copper", "tungsten", "beryllium",
                "cobalt", "niobium", "zirconium", "boron", "neodymium", "lanthanium", "asbestos", "uranium",
                "thorium", "polonium", "u238", "technetium", "ra226", "tantalium", "silicon", "sodium",
                "strontium", "lithium", "sulfur", "fluorite", "red_phosphorus", "chlorocalcite", "molysite",
                "cinnabar", "borax", "sodalite", "redstone", "coal", "lignite", "diamond", "bauxite",
                "cryolite", "emerald", "niter", "rare"});
    }

    private static boolean declared;

    private ModItemModels() {}

    public static void declare() {
        if (declared) {
            return;
        }
        declared = true;

        sprites();
        variants();
        remappedShapeSprites();
        renamedSprites();
        layeredSprites();
        tinted();
        shared();
        tools();
        blockItems();
        machineFrames();
        funnelModel();
        entityItems();
    }

    /**
     * The sixteen dye colours, as {@code chemical_dye} and {@code crayon} carry them. Both families
     * paint the same overlay in the same sixteen colours, so one table serves both.
     */
    private static final int[] DYES = {
            0x1E1B1B, 0x253192, 0x51301A, 0x287697, 0x434343, 0x3B511A, 0x6689D3, 0x41CD34,
            0xC354CD, 0xEB8844, 0xD88198, 0x7B2FBE, 0xB3312C, 0xABABAB, 0xF0F0F0, 0xDECF2A,
    };

    private static final String[] DYE_NAMES = {
            "black", "blue", "brown", "cyan", "gray", "green", "lightblue", "lime",
            "magenta", "orange", "pink", "purple", "red", "silver", "white", "yellow",
    };

    /**
     * Items whose overlay takes a colour from the stack. The base layer is pinned white so only the
     * overlay tints; where an item is one layer there is nothing to pin and the tint list is one
     * entry long.
     */
    private static void tinted() {
        for (int i = 0; i < DYE_NAMES.length; i++) {
            dyed("chemical_dye_" + DYE_NAMES[i], "hbm:items/chemical_dye", DYES[i]);
            dyed("crayon_" + DYE_NAMES[i], "hbm:items/crayon", DYES[i]);
        }
        fluidTinted("fluid_barrel", "hbm:items/fluid_barrel", "hbm:items/fluid_barrel_overlay");
        fluidTinted("fluid_bucket", "minecraft:item/bucket", "hbm:items/fluid_bucket_fill");
        fluidTinted("fluid_identifier", "hbm:items/fluid_identifier_multi", "hbm:items/fluid_identifier_overlay");
        fluidTinted("fluid_tank", "hbm:items/fluid_tank", "hbm:items/fluid_tank_overlay");
        Dejson.item(NS, "fluid_icon")
                .models(Models.generated("hbm:items/fluid_icon"))
                .tints(Tints.customModelData(0, 0xFFFFFF))
                .register();
    }

    private static void dyed(String item, String base, int colour) {
        Dejson.item(NS, item)
                .models(Models.generated(base, base + "_overlay"))
                .tints(Tints.constant(-1), Tints.customModelData(0, colour))
                .register();
    }

    private static void fluidTinted(String item, String base, String overlay) {
        Dejson.item(NS, item)
                .models(Models.generated(base, overlay))
                .tints(Tints.constant(-1), Tints.customModelData(0, 0xFFFFFF))
                .register();
    }

    /**
     * The families that pick their model from {@code hbm:item_variant}, one sprite per enum constant.
     *
     * <p>Order is the declaration: case <i>n</i> is constant <i>n</i>, so these lists are the enum's
     * own order and nothing else. The six other component-selected families stay written, because
     * their cases are not all sprites.
     *
     * <p>{@code ammo_arty} is here because it was not anywhere: it shipped as a single sprite, so all
     * twelve shells drew the plain one.
     */
    private static void variants() {
        selectedSprites("ammo_arty", "", "_classic", "_he", "_mini_nuke", "_nuke", "_phosphorus",
                "_mini_nuke_multi", "_phosphorus_multi", "_cargo", "_chlorine", "_phosgene",
                "_mustard_gas");
    }

    private static void selectedSprites(String item, String... suffixes) {
        Models.Group models = Models.group();
        for (String suffix : suffixes) {
            models = models.and(suffix, Models.generated(NS + ":items/" + item + suffix));
        }
        Dejson.item(NS, item).models(models).selects(NS + ":item_variant").register();
    }

    /** Items that show a model another item already declares. */
    private static void shared() {
        for (String waste : ZIRNOX_WASTE) {
            Dejson.item(NS, waste).shares("hbm:item/zirnox_waste").register();
        }
    }

    private static final String[] ZIRNOX_WASTE = {
            "waste_les", "waste_mox", "waste_natural_uranium", "waste_plutonium", "waste_schrabidium",
            "waste_thorium", "waste_u233", "waste_u235", "waste_uranium", "waste_zfb_mox",
    };

    private static final String[] SPRITE_ITEMS_0 = {
            "ammo_debug", "ammo_dgk", "ammo_fireext", "ammo_fireext_foam",
            "ammo_fireext_sand", "ammo_secret_m44_equestrian", "ammo_shell", "ammo_shell_apfsds_du",
            "ammo_shell_apfsds_t", "ammo_shell_explosive", "ammo_shell_w9", "ammo_standard_bmg50_ap",
            "ammo_standard_bmg50_du", "ammo_standard_bmg50_fmj", "ammo_standard_bmg50_he",
            "ammo_standard_bmg50_jhp", "ammo_standard_bmg50_sm", "ammo_standard_bmg50_sp",
            "ammo_standard_flame_diesel", "ammo_standard_m357_ap", "ammo_standard_m357_bp",
            "ammo_standard_m357_express", "ammo_standard_m357_fmj", "ammo_standard_m357_jhp",
            "ammo_standard_m357_sp", "ammo_standard_m44_ap", "ammo_standard_m44_bp",
            "ammo_standard_m44_express", "ammo_standard_m44_fmj", "ammo_standard_m44_jhp",
            "ammo_standard_m44_sp", "ammo_standard_nuke_high", "ammo_standard_p22_ap",
            "ammo_standard_p22_fmj", "ammo_standard_p22_jhp", "ammo_standard_p22_sp",
            "ammo_standard_p9_ap", "ammo_standard_p9_fmj", "ammo_standard_p9_jhp",
            "ammo_standard_p9_sp", "ammo_standard_r556_ap", "ammo_standard_r556_fmj",
            "ammo_standard_r556_jhp", "ammo_standard_r556_sp", "ammo_standard_rocket_demo",
            "ammo_standard_rocket_he", "ammo_standard_rocket_heat", "ammo_standard_rocket_inc",
            "ammo_standard_rocket_phosphorus", "ammo_standard_stone", "ammo_standard_stone_ap",
            "ammo_standard_stone_iron", "ammo_standard_stone_shot", "ammo_standard_tau_uranium",
            "ams_catalyst_aluminium", "ams_catalyst_beryllium", "ams_catalyst_blank",
            "ams_catalyst_caesium", "ams_catalyst_cerium", "ams_catalyst_cobalt", "ams_catalyst_copper",
            "ams_catalyst_dineutronium", "ams_catalyst_euphemium", "ams_catalyst_iron",
            "ams_catalyst_lithium", "ams_catalyst_niobium", "ams_catalyst_schrabidium",
            "ams_catalyst_strontium", "ams_catalyst_thorium", "ams_catalyst_tungsten", "ams_muzzle",
            "armor_battery", "armor_battery_mk2", "armor_battery_mk3", "armor_polish", "asbestos_boots",
            "asbestos_cloth", "asbestos_helmet", "asbestos_legs", "asbestos_plate", "assembly_nuke",
            "ball_dynamite", "ball_fireclay", "ball_resin", "ball_tatb", "ball_tnt", "ballistite",
            "battery_sc_americium", "battery_sc_gold", "battery_sc_lead", "battery_sc_plutonium",
            "battery_sc_polonium", "battery_sc_technetium", "battery_sc_uranium", "battery_spark",
            "battery_trixite", "bedrock_ore_fragment_bismuth", "billet_actinium", "billet_am241",
            "billet_am242", "billet_am_mix", "billet_americium_fuel", "billet_au198",
            "billet_australium", "billet_australium_greater", "billet_australium_lesser",
            "billet_balefire_gold", "billet_beryllium", "billet_bismuth", "billet_co60",
            "billet_cobalt", "billet_flashlead", "billet_gh336", "billet_hes", "billet_les",
            "billet_mox_fuel", "billet_neptunium", "billet_neptunium_fuel", "billet_nuclear_waste",
            "billet_pb209", "billet_plutonium", "billet_plutonium_fuel", "billet_po210be",
            "billet_polonium", "billet_pu238", "billet_pu238be", "billet_pu239", "billet_pu240",
            "billet_pu241", "billet_pu_mix", "billet_ra226", "billet_ra226be", "billet_schrabidium",
            "billet_schrabidium_fuel", "billet_silicon", "billet_solinium", "billet_sr90",
            "billet_technetium", "billet_th232", "billet_thorium_fuel", "billet_u233", "billet_u235",
            "billet_u238", "billet_unobtainium", "billet_uranium", "billet_uranium_fuel", "billet_uzh",
            "billet_yharonite", "billet_zfb_am_mix", "billet_zfb_bismuth", "billet_zfb_pu241",
            "billet_zirconium", "bio_wafer", "biomass", "biomass_compressed", "black_hole",
            "blade_titanium", "blade_tungsten", "blades_desh", "blades_steel", "blades_titanium",
            "blueprint_folder_discover", "blueprints_528", "blueprints_discover", "blueprints_secret",
            "board_copper", "bolt", "bolt_compound", "bolt_spike", "book_secret", "bottle2_empty",
            "bottle_empty", "bottle_mercury", "boy_bullet", "boy_igniter", "boy_propellant",
            "boy_shielding", "boy_target", "broken_item", "burnt_bark", "can_empty", "can_key",
            "canister_empty", "canister_napalm", "cap_aluminium", "cap_fritz", "cap_korl", "cap_nuka",
            "cap_quantum", "cap_rad", "cap_sparkle", "cap_star", "cap_sunset", "casing_357",
            "casing_44", "casing_50", "casing_9", "casing_buckshot", "catalyst_clay",
            "catalytic_converter", "cbt_device", "cell_balefire", "centrifuge_element",
            "charge_railgun", "cheese", "chocolate", "chopper_blades", "chopper_gun", "chopper_head",
            "chopper_tail", "chopper_torso", "chopper_wing", "cinnabar", "circuit_star",
            "cladding_desh", "cladding_ghiorsium", "cladding_lead", "cladding_paint", "cladding_rubber",
            "coal_eternal", "coal_infernal", "cobalt_boots", "cobalt_helmet", "cobalt_legs",
            "cobalt_plate", "coil_copper", "coil_copper_torus", "coil_gold", "coil_gold_torus",
            "coil_magnetized_tungsten", "coil_tungsten", "coin_creeper", "coin_maskman",
            "coin_radiation", "coin_token", "coin_ufo", "coin_worm", "coke_coal", "coke_lignite",
            "coke_petroleum", "combine_scrap", "cordite", "crt_display", "crystal_aluminium",
            "crystal_asbestos", "crystal_beryllium", "crystal_charred", "crystal_cinnabar",
            "crystal_coal", "crystal_cobalt", "crystal_copper", "crystal_diamond", "crystal_energy",
            "crystal_fluorite", "crystal_gold", "crystal_horn", "crystal_iron", "crystal_lapis",
            "crystal_lead", "crystal_lithium", "crystal_niter", "crystal_osmiridium",
            "crystal_phosphorus", "crystal_plutonium", "crystal_rare", "crystal_redstone",
            "crystal_schrabidium", "crystal_schraranium", "crystal_starmetal", "crystal_sulfur",
            "crystal_thorium", "crystal_titanium", "crystal_trixite", "crystal_tungsten",
            "crystal_uranium", "crystal_xen", "custom_amat", "custom_dirty", "custom_euph",
            "custom_fall", "custom_hydro", "custom_nuke", "custom_schrab", "custom_sol", "custom_tnt",
            "debris_concrete", "debris_element", "debris_exchanger", "debris_fuel", "debris_graphite",
            "debris_metal", "debris_shrapnel", "definitelyfood", "demon_core_closed", "designator",
            "designator_arty_range", "deuterium_filter", "dosimeter", "drill_titanium", "ducc",
            "ducttape", "dust", "dust_tiny", "dysfunctional_reactor", "egg_balefire"
    };

    private static final String[] SPRITE_ITEMS_1 = {
            "egg_balefire_shard", "egg_glyphid", "entanglement_kit", "euphemium_capacitor",
            "euphemium_stopper", "filter_coal", "fins_big_steel", "fins_flat", "fins_quad_titanium",
            "fins_small_steel", "fins_tri_steel", "five_htp", "flame_conspiracy", "flame_opinion",
            "flame_politics", "flame_pony", "fluid_pack_empty", "fluid_tank_lead_empty", "fluorite",
            "flywheel_beryllium", "fmn", "fooditem", "fragment_actinium", "fragment_boron",
            "fragment_cerium", "fragment_cobalt", "fragment_coltan", "fragment_lanthanium",
            "fragment_meteorite", "fragment_neodymium", "fragment_niobium", "fuel_tank_large",
            "fuel_tank_medium", "fuel_tank_small", "fuse", "fusion_core_infinite", "gadget_core",
            "gadget_explosive", "gadget_explosive8", "gadget_wireing", "gas_empty", "geiger_counter",
            "gem_alexandrite", "gem_rad", "gem_sodalite", "gem_tantalium", "gem_volcanic",
            "generator_steel", "glyphid_meat", "glyphid_meat_grilled", "hazmat_boots",
            "hazmat_boots_grey", "hazmat_boots_red", "hazmat_cloth", "hazmat_cloth_grey",
            "hazmat_cloth_red", "hazmat_helmet", "hazmat_helmet_grey", "hazmat_helmet_red",
            "hazmat_legs", "hazmat_legs_grey", "hazmat_legs_red", "hazmat_plate", "hazmat_plate_grey",
            "hazmat_plate_red", "holotape_damaged", "icf_pellet_depleted", "icf_pellet_empty",
            "ingot_actinium", "ingot_aluminium", "ingot_am241", "ingot_am242", "ingot_am_mix",
            "ingot_americium_fuel", "ingot_arsenic", "ingot_arsenic_bronze", "ingot_asbestos",
            "ingot_astatine", "ingot_au198", "ingot_australium", "ingot_bakelite", "ingot_beryllium",
            "ingot_biorubber", "ingot_bismuth", "ingot_bismuth_bronze", "ingot_boron", "ingot_bromine",
            "ingot_bscco", "ingot_c4", "ingot_cadmium", "ingot_caesium", "ingot_calcium",
            "ingot_cdalloy", "ingot_cerium", "ingot_cft", "ingot_chainsteel", "ingot_co60",
            "ingot_cobalt", "ingot_combine_steel", "ingot_copper", "ingot_daffergon", "ingot_desh",
            "ingot_dineutronium", "ingot_dura_steel", "ingot_euphemium", "ingot_ferrouranium",
            "ingot_fiberglass", "ingot_firebrick", "ingot_gh336", "ingot_graphite", "ingot_gunmetal",
            "ingot_hes", "ingot_i131", "ingot_iodine", "ingot_lanthanium", "ingot_lead", "ingot_les",
            "ingot_magnetized_tungsten", "ingot_mox_fuel", "ingot_mud", "ingot_neptunium",
            "ingot_neptunium_fuel", "ingot_niobium", "ingot_osmiridium", "ingot_pb209", "ingot_pc",
            "ingot_phosphorus", "ingot_plutonium", "ingot_plutonium_fuel", "ingot_polonium",
            "ingot_polymer", "ingot_pu238", "ingot_pu239", "ingot_pu240", "ingot_pu241", "ingot_pu_mix",
            "ingot_pvc", "ingot_ra226", "ingot_red_copper", "ingot_reiium", "ingot_rubber",
            "ingot_saturnite", "ingot_schrabidate", "ingot_schrabidium", "ingot_schrabidium_fuel",
            "ingot_schraranium", "ingot_semtex", "ingot_silicon", "ingot_solinium", "ingot_sr90",
            "ingot_starmetal", "ingot_steel", "ingot_steel_dusted", "ingot_tantalium", "ingot_tcalloy",
            "ingot_technetium", "ingot_tennessine", "ingot_th232", "ingot_thorium_fuel",
            "ingot_titanium", "ingot_tungsten", "ingot_tungsten_carbide", "ingot_u233", "ingot_u235",
            "ingot_u238", "ingot_unobtainium", "ingot_uranium", "ingot_uranium_fuel", "ingot_verticium",
            "ingot_weaponsteel", "ingot_weidanium", "ingot_zirconium", "insert_doxium", "insert_du",
            "insert_esapi", "insert_ghiorsium", "insert_kevlar", "insert_polonium", "insert_sapi",
            "insert_steel", "insert_xsapi", "insert_yharonite", "iv_empty", "jetpack_boost",
            "jetpack_break", "jetpack_fly", "jetpack_tank", "jetpack_vector", "journal_bj",
            "journal_pip", "journal_silver", "key_red_cracked", "laser_crystal_bale",
            "laser_crystal_bismuth", "laser_crystal_cmb", "laser_crystal_co2", "laser_crystal_digamma",
            "launch_code", "launch_code_piece", "launch_key", "lemon", "levitation_unit", "lignite",
            "lithium", "loop_stew", "loops", "magnet_circular", "man_core", "man_explosive",
            "man_explosive8", "man_igniter", "marshmallow", "marshmallow_roasted", "med_bag",
            "med_ipecac", "med_ptsd", "med_schizophrenia", "mike_cooling_unit", "mike_core",
            "mike_deut", "missile_assembly", "missile_carrier", "missile_endo", "missile_exo",
            "missile_skin_camo", "missile_skin_desert", "missile_skin_flames", "missile_skin_manly_pink",
            "missile_skin_metal", "missile_skin_orange_insulation", "missile_skin_sleek",
            "missile_skin_soviet_glory", "missile_skin_soviet_stank",
            "mold_barrel_heavy", "mold_barrel_light", "mold_base", "mold_billet",
            "mold_blade", "mold_blades", "mold_block", "mold_c50", "mold_c9", "mold_grip", "mold_ingot",
            "mold_ingots", "mold_mechanism", "mold_nugget", "mold_pipe", "mold_pipes", "mold_plate",
            "mold_plate_cast", "mold_plates", "mold_plates_cast", "mold_receiver_heavy",
            "mold_receiver_light", "mold_shell", "mold_stamp", "mold_stock", "mold_wire",
            "mold_wire_dense", "mold_wires_dense", "motor", "motor_bismuth", "motor_desh", "n2_charge",
            "neutron_reflector", "niter", "nitra", "nitra_small", "nuclear_waste", "nuclear_waste_long",
            "nuclear_waste_long_depleted", "nuclear_waste_long_depleted_tiny",
            "nuclear_waste_long_tiny", "nuclear_waste_short", "nuclear_waste_short_depleted",
            "nuclear_waste_short_depleted_tiny", "nuclear_waste_short_tiny", "nuclear_waste_tiny",
            "nuclear_waste_vitrified", "nuclear_waste_vitrified_tiny", "nugget", "nugget_actinium",
            "nugget_am241", "nugget_am242", "nugget_am_mix", "nugget_americium_fuel", "nugget_arsenic",
            "nugget_au198", "nugget_australium", "nugget_australium_greater",
            "nugget_australium_lesser", "nugget_beryllium", "nugget_bismuth", "nugget_cadmium",
            "nugget_co60", "nugget_cobalt", "nugget_daffergon", "nugget_desh", "nugget_dineutronium",
            "nugget_euphemium", "nugget_gh336", "nugget_hes", "nugget_lead", "nugget_les",
            "nugget_mercury", "nugget_mercury_tiny", "nugget_mox_fuel", "nugget_neptunium",
            "nugget_neptunium_fuel", "nugget_niobium", "nugget_osmiridium", "nugget_pb209",
            "nugget_plutonium", "nugget_plutonium_fuel", "nugget_polonium"
    };

    private static final String[] SPRITE_ITEMS_2 = {
            "nugget_pu238", "nugget_pu239", "nugget_pu240", "nugget_pu241", "nugget_pu_mix",
            "nugget_ra226", "nugget_reiium", "nugget_schrabidium", "nugget_schrabidium_fuel",
            "nugget_silicon", "nugget_solinium", "nugget_sr90", "nugget_strontium", "nugget_tantalium",
            "nugget_technetium", "nugget_th232", "nugget_thorium_fuel", "nugget_u233", "nugget_u235",
            "nugget_u238", "nugget_unobtainium", "nugget_unobtainium_greater",
            "nugget_unobtainium_lesser", "nugget_uranium", "nugget_uranium_fuel", "nugget_verticium",
            "nugget_weidanium", "nugget_zirconium", "oil_detector", "oil_tar_coal", "oil_tar_crack",
            "oil_tar_crude", "oil_tar_paraffin", "oil_tar_wax", "oil_tar_wood", "paa_boots", "paa_legs",
            "paa_plate", "part_barrel_heavy", "part_barrel_light", "part_beryllium", "part_carbon",
            "part_copper", "part_grip", "part_lithium", "part_mechanism", "part_plutonium",
            "part_receiver_heavy", "part_receiver_light", "part_stock", "particle_aelectron",
            "particle_amat", "particle_aproton", "particle_aschrab", "particle_copper", "particle_dark",
            "particle_digamma", "particle_empty", "particle_higgs", "particle_hydrogen",
            "particle_lead", "particle_lutece", "particle_muon", "particle_sparkticle",
            "particle_strange", "particle_tachyon", "pedestal_steel", "pellet_antimatter",
            "pellet_cluster", "pellet_coal", "pellet_coolant", "pellet_gas", "pellet_rtg",
            "pellet_rtg_actinium", "pellet_rtg_americium", "pellet_rtg_balefire", "pellet_rtg_cobalt",
            "pellet_rtg_gold", "pellet_rtg_lead", "pellet_rtg_polonium", "pellet_rtg_radium",
            "pellet_rtg_strontium", "pellet_rtg_weak", "photo_panel", "pill_herbal", "pill_iodine",
            "pill_red", "pin", "pipe", "pipes_steel", "piston_selenium", "piston_set_desh",
            "piston_set_dura", "piston_set_starmetal", "piston_set_steel", "plan_c", "plate_aluminium",
            "plate_armor_ajr", "plate_armor_dnt", "plate_armor_fau", "plate_armor_hev",
            "plate_armor_lunar", "plate_armor_titanium", "plate_bismuth", "plate_cast",
            "plate_combine_steel", "plate_copper", "plate_dalekanium", "plate_desh",
            "plate_dineutronium", "plate_dura_steel", "plate_euphemium", "plate_fuel_mox",
            "plate_fuel_pu238be", "plate_fuel_pu239", "plate_fuel_ra226be", "plate_fuel_sa326",
            "plate_fuel_u233", "plate_fuel_u235", "plate_gold", "plate_gunmetal", "plate_iron",
            "plate_kevlar", "plate_lead", "plate_mixed", "plate_paa", "plate_polymer",
            "plate_saturnite", "plate_schrabidium", "plate_steel", "plate_titanium",
            "plate_weaponsteel", "plate_welded", "powder_actinium", "powder_actinium_tiny",
            "powder_aluminium", "powder_asbestos", "powder_astatine", "powder_at209",
            "powder_at209_tiny", "powder_au198", "powder_au198_tiny", "powder_australium",
            "powder_bakelite", "powder_balefire", "powder_beryllium", "powder_bismuth", "powder_borax",
            "powder_boron", "powder_boron_tiny", "powder_bromine", "powder_cadmium", "powder_caesium",
            "powder_calcium", "powder_cdalloy", "powder_cement", "powder_cerium", "powder_cerium_tiny",
            "powder_chlorocalcite", "powder_chlorophyte", "powder_cloud", "powder_co60",
            "powder_co60_tiny", "powder_coal", "powder_coal_tiny", "powder_cobalt",
            "powder_cobalt_tiny", "powder_coltan", "powder_coltan_ore", "powder_combine_steel",
            "powder_copper", "powder_cs137", "powder_cs137_tiny", "powder_daffergon", "powder_desh",
            "powder_desh_mix", "powder_desh_ready", "powder_diamond", "powder_dineutronium",
            "powder_dura_steel", "powder_emerald", "powder_euphemium", "powder_fire", "powder_flux",
            "powder_gold", "powder_i131", "powder_i131_tiny", "powder_ice", "powder_impure_osmiridium",
            "powder_iodine", "powder_iodine_tiny", "powder_iron", "powder_lanthanium",
            "powder_lanthanium_tiny", "powder_lapis", "powder_lead", "powder_lignite",
            "powder_limestone", "powder_lithium", "powder_lithium_tiny", "powder_magic",
            "powder_magnetized_tungsten", "powder_meteorite", "powder_meteorite_tiny",
            "powder_molysite", "powder_neodymium", "powder_neodymium_tiny", "powder_neptunium",
            "powder_niobium", "powder_niobium_tiny", "powder_nitan_mix", "powder_osmiridium",
            "powder_paleogenite", "powder_paleogenite_tiny", "powder_pb209", "powder_pb209_tiny",
            "powder_plutonium", "powder_poison", "powder_polonium", "powder_polymer", "powder_quartz",
            "powder_ra226", "powder_red_copper", "powder_reiium", "powder_sawdust",
            "powder_schrabidate", "powder_schrabidium", "powder_semtex_mix", "powder_sodium",
            "powder_spark_mix", "powder_sr90", "powder_sr90_tiny", "powder_steel", "powder_steel_tiny",
            "powder_strontium", "powder_tantalium", "powder_tcalloy", "powder_tektite",
            "powder_tennessine", "powder_thermite", "powder_thorium", "powder_titanium",
            "powder_tungsten", "powder_unobtainium", "powder_uranium", "powder_verticium"
    };

    private static final String[] SPRITE_ITEMS_3 = {
            "powder_weidanium", "powder_xe135", "powder_xe135_tiny", "powder_yellowcake",
            "powder_zirconium", "primer_357", "primer_44", "primer_50", "primer_9", "primer_buckshot",
            "pudding", "radaway", "radaway_flush", "radaway_strong", "radx", "rag_damp", "rag_piss",
            "rbmk_fuel_balefire", "rbmk_fuel_balefire_gold", "rbmk_fuel_drx", "rbmk_fuel_empty",
            "rbmk_fuel_flashlead", "rbmk_fuel_hea241", "rbmk_fuel_hea242", "rbmk_fuel_heaus",
            "rbmk_fuel_hen", "rbmk_fuel_hep", "rbmk_fuel_hep241", "rbmk_fuel_hes", "rbmk_fuel_heu233",
            "rbmk_fuel_heu235", "rbmk_fuel_lea", "rbmk_fuel_leaus", "rbmk_fuel_lep", "rbmk_fuel_les",
            "rbmk_fuel_mea", "rbmk_fuel_men", "rbmk_fuel_mep", "rbmk_fuel_mes", "rbmk_fuel_meu",
            "rbmk_fuel_mox", "rbmk_fuel_po210be", "rbmk_fuel_pu238be", "rbmk_fuel_ra226be",
            "rbmk_fuel_test", "rbmk_fuel_thmeu", "rbmk_fuel_ueu", "rbmk_fuel_uzh",
            "rbmk_fuel_zfb_am_mix", "rbmk_fuel_zfb_bismuth", "rbmk_fuel_zfb_pu241", "rbmk_lid",
            "rbmk_lid_glass", "rbmk_tool", "reactor_core", "redstone_depleted", "ring_pull",
            "ring_starmetal", "robes_boots", "robes_helmet", "robes_legs", "robes_plate", "rocket_fuel",
            "rod_dual_empty", "rod_empty", "rod_quad_empty", "rod_zirnox_depleted_les_fuel",
            "rod_zirnox_depleted_mox_fuel", "rod_zirnox_depleted_natural_uranium_fuel",
            "rod_zirnox_depleted_plutonium_fuel", "rod_zirnox_depleted_thorium_fuel",
            "rod_zirnox_depleted_u233_fuel", "rod_zirnox_depleted_u235_fuel",
            "rod_zirnox_depleted_uranium_fuel", "rod_zirnox_depleted_zfb_mox_fuel", "rod_zirnox_empty",
            "rod_zirnox_les_fuel", "rod_zirnox_lithium_fuel", "rod_zirnox_mox_fuel",
            "rod_zirnox_natural_uranium_fuel", "rod_zirnox_plutonium_fuel", "rod_zirnox_th232_fuel",
            "rod_zirnox_thorium_fuel", "rod_zirnox_tritium", "rod_zirnox_u233_fuel",
            "rod_zirnox_u235_fuel", "rod_zirnox_uranium_fuel", "rod_zirnox_zfb_mox_fuel", "rotor_steel",
            "rtg_unit", "rune_blank", "rune_dagaz", "rune_hagalaz", "rune_isa", "rune_jera",
            "rune_thurisaz", "safety_fuse", "sat_base", "sat_head_laser", "sat_head_mapper",
            "sat_head_radar", "sat_head_resonator", "sat_head_scanner", "sawblade", "schrabidium_boots",
            "schrabidium_helmet", "schrabidium_legs", "schrabidium_plate", "scrap", "scrap_nuclear",
            "scrap_oil", "scrap_plastic", "scraps_liquid", "taurun_boots", "taurun_helmet",
            "taurun_legs", "taurun_plate", "security_boots", "security_helmet", "security_legs",
            "security_plate", "seg_10", "seg_15", "seg_20", "shell", "shimmer_axe_head",
            "shimmer_handle", "singularity", "singularity_counter_resonant", "singularity_spark",
            "singularity_super_heated", "siox", "solid_fuel", "solid_fuel_bf", "solid_fuel_presto",
            "solid_fuel_presto_bf", "solid_fuel_presto_triplet", "solid_fuel_presto_triplet_bf",
            "sphere_steel", "spongebob_macaroni", "stamp_357", "stamp_44", "stamp_50", "stamp_9",
            "stamp_desh_357", "stamp_desh_44", "stamp_desh_50", "stamp_desh_9", "stamp_desh_circuit",
            "stamp_desh_flat", "stamp_desh_plate", "stamp_desh_wire", "stamp_iron_circuit",
            "stamp_iron_flat", "stamp_iron_plate", "stamp_iron_wire", "stamp_obsidian_circuit",
            "stamp_obsidian_flat", "stamp_obsidian_plate", "stamp_obsidian_wire", "stamp_steel_circuit",
            "stamp_steel_flat", "stamp_steel_plate", "stamp_steel_wire", "stamp_stone_circuit",
            "stamp_stone_flat", "stamp_stone_plate", "stamp_stone_wire", "stamp_titanium_circuit",
            "stamp_titanium_flat", "stamp_titanium_plate", "stamp_titanium_wire", "starmetal_boots",
            "starmetal_helmet", "starmetal_legs", "starmetal_plate", "static_sandwich", "steel_boots",
            "steel_helmet", "steel_legs", "steel_plate", "stick_c4", "stick_semtex", "stick_tnt",
            "sulfur", "syringe_antidote", "syringe_awesome", "syringe_empty", "syringe_metal_empty",
            "syringe_metal_medx", "syringe_metal_psycho", "syringe_metal_stimpak",
            "syringe_metal_super", "syringe_mkunicorn", "syringe_poison", "syringe_taint", "tank_steel",
            "telepad", "template_folder", "thermo_element", "thermo_unit_empty", "thermo_unit_endo",
            "thermo_unit_exo", "thruster_large", "thruster_medium", "thruster_nuclear",
            "thruster_small", "titanium_boots", "titanium_helmet", "titanium_legs", "titanium_plate",
            "toothpicks", "trinitite", "tritium_deuterium_cake", "tsar_core", "turbine_titanium",
            "turbine_tungsten", "turret_chip", "turret_mob_filter", "twinkie", "undefined",
            "upgrade_afterburn_1", "upgrade_afterburn_2", "upgrade_afterburn_3", "upgrade_centrifuge",
            "upgrade_crystallizer", "upgrade_effect_1", "upgrade_effect_2", "upgrade_effect_3",
            "upgrade_fortune_1", "upgrade_fortune_2", "upgrade_fortune_3", "upgrade_gc_speed",
            "upgrade_nullifier", "upgrade_overdrive_1", "upgrade_overdrive_2", "upgrade_overdrive_3",
            "upgrade_power_1", "upgrade_power_2", "upgrade_power_3", "upgrade_screm",
            "upgrade_shredder", "upgrade_smelter", "upgrade_speed_1", "upgrade_speed_2",
            "upgrade_speed_3", "upgrade_template", "warhead_buster_large", "warhead_buster_medium",
            "warhead_buster_small", "warhead_cluster_large", "warhead_cluster_medium",
            "warhead_cluster_small", "warhead_generic_large", "warhead_generic_medium",
            "warhead_generic_small", "warhead_incendiary_large", "warhead_incendiary_medium",
            "warhead_incendiary_small", "warhead_mirv", "warhead_mirvlet", "warhead_n2",
            "warhead_nuclear", "warhead_thermo_endo", "warhead_thermo_exo", "warhead_volcano", "watch",
            "watz_pellet_boron", "watz_pellet_depleted_boron", "watz_pellet_depleted_du",
            "watz_pellet_depleted_hen", "watz_pellet_depleted_hes", "watz_pellet_depleted_lead",
            "watz_pellet_depleted_les", "watz_pellet_depleted_mep", "watz_pellet_depleted_mes",
            "watz_pellet_depleted_meu", "watz_pellet_depleted_nqd", "watz_pellet_depleted_nqr",
            "watz_pellet_depleted_schrabidium", "watz_pellet_du", "watz_pellet_hen", "watz_pellet_hes",
            "watz_pellet_lead", "watz_pellet_les", "watz_pellet_mep", "watz_pellet_mes",
            "watz_pellet_meu", "watz_pellet_nqd", "watz_pellet_nqr", "watz_pellet_schrabidium",
            "wings_limp", "wings_murk", "wire_dense", "wire_fine", "xanax"
    };

    /**
     * Items drawn as a single sprite named after the item, which is the overwhelming majority.
     * Split across several arrays only to keep any one static initialiser small.
     */
    private static void sprites() {
        for (String name : SPRITE_ITEMS_0) {
            Dejson.item(NS, name).models(Models.generated("hbm:items/" + name)).register();
        }
        for (String name : SPRITE_ITEMS_1) {
            Dejson.item(NS, name).models(Models.generated("hbm:items/" + name)).register();
        }
        for (String name : SPRITE_ITEMS_2) {
            Dejson.item(NS, name).models(Models.generated("hbm:items/" + name)).register();
        }
        for (String name : SPRITE_ITEMS_3) {
            Dejson.item(NS, name).models(Models.generated("hbm:items/" + name)).register();
        }
    }

    /**
     * The material-shape items, whose sprite is computed rather than shipped. The model is the plain
     * one-sprite case like any other; only the texture behind it is generated.
     */
    private static void remappedShapeSprites() {
        REMAPPED_SHAPES.forEach((art, materials) -> {
            Dejson.generatedSprites(NS + ":items/" + art, "_", List.of(materials));
            for (String material : materials) {
                String name = art + "_" + material;
                Dejson.item(NS, name).models(Models.generated(NS + ":items/" + name)).register();
            }
        });
    }

    /** Items whose sprite is not named after them; the sprite is given rather than derived. */
    private static void renamedSprites() {
        renamed0();
        renamed1();
        powerArmorSprites();
    }

    /** RPA and NCRPA both draw the original {@code rpa_*} icons (the original NCRPA setTextureName). */
    private static void powerArmorSprites() {
        for (String set : new String[] {"rpa", "ncrpa"}) {
            for (String part : new String[] {"helmet", "plate", "legs", "boots"}) {
                Dejson.item(NS, set + "_" + part).models(Models.generated("hbm:items/rpa_" + part)).register();
            }
        }
    }

    private static void renamed0() {
        Dejson.item(NS, "gun_b92_ammo").models(Models.generated("hbm:items/gun_b92_ammo_alt")).register();
        Dejson.item(NS, "gun_b92_flat").models(Models.generated("hbm:items/gun_b92")).register();
        Dejson.item(NS, "arc_electrode").models(Models.generated("hbm:items/arc_electrode.graphite")).register();
        Dejson.item(NS, "arc_electrode_burnt").models(Models.generated("hbm:items/arc_electrode_burnt.graphite")).register();
        Dejson.item(NS, "arc_electrode_burnt_desh").models(Models.generated("hbm:items/arc_electrode_burnt.desh")).register();
        Dejson.item(NS, "arc_electrode_burnt_lanthanium").models(Models.generated("hbm:items/arc_electrode_burnt.lanthanium")).register();
        Dejson.item(NS, "arc_electrode_burnt_saturnite").models(Models.generated("hbm:items/arc_electrode_burnt.saturnite")).register();
        Dejson.item(NS, "arc_electrode_desh").models(Models.generated("hbm:items/arc_electrode.desh")).register();
        Dejson.item(NS, "arc_electrode_lanthanium").models(Models.generated("hbm:items/arc_electrode.lanthanium")).register();
        Dejson.item(NS, "arc_electrode_saturnite").models(Models.generated("hbm:items/arc_electrode.saturnite")).register();
        Dejson.item(NS, "bedrock_ore_base").models(Models.generated("hbm:items/bedrock_ore_new")).register();
        Dejson.item(NS, "blueprint_plate").models(Models.generated("hbm:items/blueprints")).register();
        Dejson.item(NS, "casing_large").models(Models.generated("hbm:items/casing.large")).register();
        Dejson.item(NS, "casing_large_steel").models(Models.generated("hbm:items/casing.large_steel")).register();
        Dejson.item(NS, "casing_small").models(Models.generated("hbm:items/casing.small")).register();
        Dejson.item(NS, "casing_small_steel").models(Models.generated("hbm:items/casing.small_steel")).register();
        Dejson.item(NS, "cell_0").models(Models.generated("hbm:items/cell_empty")).register();
        Dejson.item(NS, "cell_28").models(Models.generated("hbm:items/cell_uf6")).register();
        Dejson.item(NS, "cell_29").models(Models.generated("hbm:items/cell_puf6")).register();
        Dejson.item(NS, "cell_30").models(Models.generated("hbm:items/cell_sas3")).register();
        Dejson.item(NS, "cell_33").models(Models.generated("hbm:items/cell_antimatter")).register();
        Dejson.item(NS, "cell_34").models(Models.generated("hbm:items/cell_anti_schrabidium")).register();
        Dejson.item(NS, "cell_8").models(Models.generated("hbm:items/cell_deuterium")).register();
        Dejson.item(NS, "cell_9").models(Models.generated("hbm:items/cell_tritium")).register();
        Dejson.item(NS, "cheese_quesadilla").models(Models.generated("hbm:items/quesadilla")).register();
        Dejson.item(NS, "chunk_ore_cryolite").models(Models.generated("hbm:items/chunk_ore.cryolite")).register();
        Dejson.item(NS, "chunk_ore_malachite").models(Models.generated("hbm:items/chunk_ore.malachite")).register();
        Dejson.item(NS, "chunk_ore_moonstone").models(Models.generated("hbm:items/chunk_ore.moonstone")).register();
        Dejson.item(NS, "chunk_ore_rare").models(Models.generated("hbm:items/chunk_ore.rare")).register();
        Dejson.item(NS, "circuit_advanced").models(Models.generated("hbm:items/circuit.advanced")).register();
        Dejson.item(NS, "circuit_analog").models(Models.generated("hbm:items/circuit.analog")).register();
        Dejson.item(NS, "circuit_atomic_clock").models(Models.generated("hbm:items/circuit.atomic_clock")).register();
        Dejson.item(NS, "circuit_basic").models(Models.generated("hbm:items/circuit.basic")).register();
        Dejson.item(NS, "circuit_bismoid").models(Models.generated("hbm:items/circuit.bismoid")).register();
        Dejson.item(NS, "circuit_capacitor").models(Models.generated("hbm:items/circuit.capacitor")).register();
        Dejson.item(NS, "circuit_capacitor_board").models(Models.generated("hbm:items/circuit.capacitor_board")).register();
        Dejson.item(NS, "circuit_capacitor_tantalium").models(Models.generated("hbm:items/circuit.capacitor_tantalium")).register();
        Dejson.item(NS, "circuit_chip").models(Models.generated("hbm:items/circuit.chip")).register();
        Dejson.item(NS, "circuit_chip_bismoid").models(Models.generated("hbm:items/circuit.chip_bismoid")).register();
        Dejson.item(NS, "circuit_chip_quantum").models(Models.generated("hbm:items/circuit.chip_quantum")).register();
        Dejson.item(NS, "circuit_controller").models(Models.generated("hbm:items/circuit.controller")).register();
        Dejson.item(NS, "circuit_controller_advanced").models(Models.generated("hbm:items/circuit.controller_advanced")).register();
        Dejson.item(NS, "circuit_controller_chassis").models(Models.generated("hbm:items/circuit.controller_chassis")).register();
        Dejson.item(NS, "circuit_controller_quantum").models(Models.generated("hbm:items/circuit.controller_quantum")).register();
        Dejson.item(NS, "circuit_numitron").models(Models.generated("hbm:items/circuit.numitron")).register();
        Dejson.item(NS, "circuit_pcb").models(Models.generated("hbm:items/circuit.pcb")).register();
        Dejson.item(NS, "circuit_quantum").models(Models.generated("hbm:items/circuit.quantum")).register();
        Dejson.item(NS, "circuit_silicon").models(Models.generated("hbm:items/circuit.silicon")).register();
        Dejson.item(NS, "circuit_vacuum_tube").models(Models.generated("hbm:items/circuit.vacuum_tube")).register();
        // The core is the one dark fusion block with no model of its own: it is RenderShape.INVISIBLE, as
        // 1.7's getRenderType() == -1, and 1.7 has no item renderer for it either, so the icon is the flat
        // sprite that render type falls back to. Its art is a drawn orb, not a tileable face — the four
        // aimed components next to it do have OBJ icons, declared as machine models.
        Dejson.item(NS, "dfc_core").models(Models.generated("hbm:blocks/dfc_core")).register();
        Dejson.item(NS, "disperser_canister_empty").models(Models.generated("hbm:items/disperser_canister")).register();
        Dejson.item(NS, "falloutitem").models(Models.generated("hbm:items/fallout")).register();
        Dejson.item(NS, "fuel_additive_antiknock").models(Models.generated("hbm:items/fuel_additive.antiknock")).register();
        Dejson.item(NS, "fuel_additive_deicer").models(Models.generated("hbm:items/fuel_additive.deicer")).register();
        Dejson.item(NS, "gas_mask").models(Models.generated("hbm:items/hazmat_helmet")).register();
        Dejson.item(NS, "gas_mask_filter").models(Models.generated("hbm:items/filter_coal")).register();
        Dejson.item(NS, "gas_mask_filter_combo").models(Models.generated("hbm:items/filter_coal")).register();
        Dejson.item(NS, "gas_mask_filter_mono").models(Models.generated("hbm:items/catalyst_clay")).register();
        Dejson.item(NS, "gas_mask_filter_piss").models(Models.generated("hbm:items/rag_piss")).register();
        Dejson.item(NS, "gas_mask_filter_rag").models(Models.generated("hbm:items/rag_damp")).register();
        Dejson.item(NS, "gas_mask_m65").models(Models.generated("hbm:items/hazmat_helmet")).register();
        Dejson.item(NS, "gas_mask_mono").models(Models.generated("hbm:items/hazmat_helmet")).register();
        Dejson.item(NS, "gas_mask_olde").models(Models.generated("hbm:items/hazmat_helmet")).register();
        Dejson.item(NS, "glyphid_gland_empty").models(Models.generated("hbm:items/glyphid_gland")).register();
        Dejson.item(NS, "item_expensive_bronze_tubes").models(Models.generated("hbm:items/item_expensive.bronze_tubes")).register();
        Dejson.item(NS, "item_expensive_circuit").models(Models.generated("hbm:items/item_expensive.circuit")).register();
        Dejson.item(NS, "item_expensive_computer").models(Models.generated("hbm:items/item_expensive.computer")).register();
        Dejson.item(NS, "item_expensive_degenerate_matter").models(Models.generated("hbm:items/item_expensive.degenerate_matter")).register();
        Dejson.item(NS, "item_expensive_ferro_plating").models(Models.generated("hbm:items/item_expensive.ferro_plating")).register();
        Dejson.item(NS, "item_expensive_gold_dust").models(Models.generated("hbm:items/item_expensive.gold_dust")).register();
        Dejson.item(NS, "item_expensive_heavy_frame").models(Models.generated("hbm:items/item_expensive.heavy_frame")).register();
        Dejson.item(NS, "item_expensive_lead_plating").models(Models.generated("hbm:items/item_expensive.lead_plating")).register();
        Dejson.item(NS, "item_expensive_plastic").models(Models.generated("hbm:items/item_expensive.plastic")).register();
        Dejson.item(NS, "item_expensive_steel_plating").models(Models.generated("hbm:items/item_expensive.steel_plating")).register();
        Dejson.item(NS, "magnetron").models(Models.generated("hbm:items/magnetron_alt")).register();
        Dejson.item(NS, "mechanism_launcher_1").models(Models.generated("hbm:items/mechanism_5")).register();
        Dejson.item(NS, "mechanism_launcher_2").models(Models.generated("hbm:items/mechanism_6")).register();
        Dejson.item(NS, "mechanism_revolver_1").models(Models.generated("hbm:items/mechanism_1")).register();
        Dejson.item(NS, "mechanism_revolver_2").models(Models.generated("hbm:items/mechanism_3")).register();
        Dejson.item(NS, "mechanism_rifle_1").models(Models.generated("hbm:items/mechanism_2")).register();
        Dejson.item(NS, "mechanism_rifle_2").models(Models.generated("hbm:items/mechanism_4")).register();
        Dejson.item(NS, "mechanism_special").models(Models.generated("hbm:items/mechanism_7")).register();
    }

    private static void renamed1() {
        Dejson.item(NS, "missile_soyuz_lander").models(Models.generated("hbm:items/soyuz_lander")).register();
        Dejson.item(NS, "mp_thruster_10_xenon").models(Models.generated("hbm:items/mp_thruster")).register();
        Dejson.item(NS, "part_generic_glass_polarized").models(Models.generated("hbm:items/part_generic.glass_polarized")).register();
        Dejson.item(NS, "part_generic_hde").models(Models.generated("hbm:items/part_generic.hde")).register();
        Dejson.item(NS, "part_generic_lde").models(Models.generated("hbm:items/part_generic.lde")).register();
        Dejson.item(NS, "part_generic_piston_electric").models(Models.generated("hbm:items/part_generic.piston_electric")).register();
        Dejson.item(NS, "part_generic_piston_hydraulic").models(Models.generated("hbm:items/part_generic.piston_hydraulic")).register();
        Dejson.item(NS, "part_generic_piston_pneumatic").models(Models.generated("hbm:items/part_generic.piston_pneumatic")).register();
        Dejson.item(NS, "pellet_buckshot").models(Models.generated("hbm:items/pellets_lead")).register();
        Dejson.item(NS, "pellet_canister").models(Models.generated("hbm:items/pellets_canister")).register();
        Dejson.item(NS, "pellet_charged").models(Models.generated("hbm:items/pellets_charged")).register();
        Dejson.item(NS, "pellet_chlorophyte").models(Models.generated("hbm:items/pellets_chlorophyte")).register();
        Dejson.item(NS, "pellet_claws").models(Models.generated("hbm:items/pellets_claws")).register();
        Dejson.item(NS, "pellet_flechette").models(Models.generated("hbm:items/pellets_flechette")).register();
        Dejson.item(NS, "pellet_meteorite").models(Models.generated("hbm:items/pellets_meteorite")).register();
        Dejson.item(NS, "pellet_rtg_depleted_bismuth").models(Models.generated("hbm:items/pellet_rtg_depleted.bismuth")).register();
        Dejson.item(NS, "pellet_rtg_depleted_lead").models(Models.generated("hbm:items/pellet_rtg_depleted.lead")).register();
        Dejson.item(NS, "pellet_rtg_depleted_mercury").models(Models.generated("hbm:items/pellet_rtg_depleted.mercury")).register();
        Dejson.item(NS, "pellet_rtg_depleted_neptunium").models(Models.generated("hbm:items/pellet_rtg_depleted.neptunium")).register();
        Dejson.item(NS, "pellet_rtg_depleted_nickel").models(Models.generated("hbm:items/pellet_rtg_depleted.nickel")).register();
        Dejson.item(NS, "pellet_rtg_depleted_zirconium").models(Models.generated("hbm:items/pellet_rtg_depleted.zirconium")).register();
        Dejson.item(NS, "powder_ash_coal").models(Models.generated("hbm:items/powder_ash.coal")).register();
        Dejson.item(NS, "powder_ash_fly").models(Models.generated("hbm:items/powder_ash.fly")).register();
        Dejson.item(NS, "powder_ash_fullerene").models(Models.generated("hbm:items/powder_ash.fullerene")).register();
        Dejson.item(NS, "powder_ash_misc").models(Models.generated("hbm:items/powder_ash.misc")).register();
        Dejson.item(NS, "powder_ash_soot").models(Models.generated("hbm:items/powder_ash.soot")).register();
        Dejson.item(NS, "powder_ash_wood").models(Models.generated("hbm:items/powder_ash.wood")).register();
        Dejson.item(NS, "powder_power").models(Models.generated("hbm:items/powder_energy_alt")).register();
        Dejson.item(NS, "radio_torch_controller").models(Models.generated("hbm:blocks/rtty_controller")).register();
        Dejson.item(NS, "radio_torch_counter").models(Models.generated("hbm:blocks/rtty_counter")).register();
        Dejson.item(NS, "radio_torch_logic").models(Models.generated("hbm:blocks/rtty_logic_on")).register();
        Dejson.item(NS, "radio_torch_reader").models(Models.generated("hbm:blocks/rtty_reader")).register();
        Dejson.item(NS, "radio_torch_receiver").models(Models.generated("hbm:blocks/rtty_rec_on")).register();
        Dejson.item(NS, "radio_torch_sender").models(Models.generated("hbm:blocks/rtty_sender_on")).register();
        Dejson.item(NS, "shimmer_head").models(Models.generated("hbm:items/shimmer_head_original")).register();
        Dejson.item(NS, "stalactite_asbestos").models(Models.generated("hbm:blocks/stalactite.asbestos")).register();
        Dejson.item(NS, "stalactite_sulfur").models(Models.generated("hbm:blocks/stalactite.sulfur")).register();
        Dejson.item(NS, "stalagmite_asbestos").models(Models.generated("hbm:blocks/stalagmite.asbestos")).register();
        Dejson.item(NS, "stalagmite_sulfur").models(Models.generated("hbm:blocks/stalagmite.sulfur")).register();
        Dejson.item(NS, "steel_grate").models(Models.generated("hbm:blocks/grate_top")).register();
        Dejson.item(NS, "turret_biometry").models(Models.generated("hbm:items/rei_scanner")).register();
        Dejson.item(NS, "zirnox_depleted_rod").models(Models.generated("hbm:items/rod_zirnox_empty")).register();
        Dejson.item(NS, "zirnox_rod").models(Models.generated("hbm:items/rod_zirnox_empty")).register();
        Dejson.item(NS, "zirnox_waste").models(Models.generated("hbm:items/rod_zirnox_empty")).register();
    }

    /** Items stacked from two or three sprites, drawn bottom layer first. */
    private static void layeredSprites() {
        layered0();
        layered1();
        layered2();
        layered3();
    }

    private static void layered0() {
        Dejson.item(NS, "rbmk_pellet_balefire_e0").models(Models.generated("hbm:items/rbmk_pellet_balefire", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_balefire", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_e1").models(Models.generated("hbm:items/rbmk_pellet_balefire", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_balefire", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_e2").models(Models.generated("hbm:items/rbmk_pellet_balefire", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_balefire", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_e3").models(Models.generated("hbm:items/rbmk_pellet_balefire", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_balefire", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_e4").models(Models.generated("hbm:items/rbmk_pellet_balefire", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_balefire", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_gold_e0").models(Models.generated("hbm:items/rbmk_pellet_balefire_gold", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_gold_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_balefire_gold", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_gold_e1").models(Models.generated("hbm:items/rbmk_pellet_balefire_gold", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_gold_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_balefire_gold", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_gold_e2").models(Models.generated("hbm:items/rbmk_pellet_balefire_gold", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_gold_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_balefire_gold", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_gold_e3").models(Models.generated("hbm:items/rbmk_pellet_balefire_gold", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_gold_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_balefire_gold", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_gold_e4").models(Models.generated("hbm:items/rbmk_pellet_balefire_gold", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_balefire_gold_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_balefire_gold", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_drx_e0").models(Models.generated("hbm:items/rbmk_pellet_drx", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_drx_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_drx", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_drx_e1").models(Models.generated("hbm:items/rbmk_pellet_drx", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_drx_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_drx", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_drx_e2").models(Models.generated("hbm:items/rbmk_pellet_drx", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_drx_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_drx", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_drx_e3").models(Models.generated("hbm:items/rbmk_pellet_drx", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_drx_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_drx", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_drx_e4").models(Models.generated("hbm:items/rbmk_pellet_drx", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_drx_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_drx", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_flashlead_e0").models(Models.generated("hbm:items/rbmk_pellet_flashlead", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_flashlead_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_flashlead", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_flashlead_e1").models(Models.generated("hbm:items/rbmk_pellet_flashlead", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_flashlead_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_flashlead", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_flashlead_e2").models(Models.generated("hbm:items/rbmk_pellet_flashlead", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_flashlead_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_flashlead", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_flashlead_e3").models(Models.generated("hbm:items/rbmk_pellet_flashlead", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_flashlead_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_flashlead", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_flashlead_e4").models(Models.generated("hbm:items/rbmk_pellet_flashlead", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_flashlead_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_flashlead", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hea241_e0").models(Models.generated("hbm:items/rbmk_pellet_hea241", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_hea241_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_hea241", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hea241_e1").models(Models.generated("hbm:items/rbmk_pellet_hea241", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_hea241_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_hea241", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hea241_e2").models(Models.generated("hbm:items/rbmk_pellet_hea241", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_hea241_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_hea241", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hea241_e3").models(Models.generated("hbm:items/rbmk_pellet_hea241", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_hea241_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_hea241", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hea241_e4").models(Models.generated("hbm:items/rbmk_pellet_hea241", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_hea241_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_hea241", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hea242_e0").models(Models.generated("hbm:items/rbmk_pellet_hea242", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_hea242_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_hea242", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hea242_e1").models(Models.generated("hbm:items/rbmk_pellet_hea242", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_hea242_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_hea242", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hea242_e2").models(Models.generated("hbm:items/rbmk_pellet_hea242", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_hea242_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_hea242", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hea242_e3").models(Models.generated("hbm:items/rbmk_pellet_hea242", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_hea242_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_hea242", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hea242_e4").models(Models.generated("hbm:items/rbmk_pellet_hea242", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_hea242_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_hea242", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_heaus_e0").models(Models.generated("hbm:items/rbmk_pellet_heaus", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_heaus_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_heaus", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_heaus_e1").models(Models.generated("hbm:items/rbmk_pellet_heaus", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_heaus_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_heaus", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_heaus_e2").models(Models.generated("hbm:items/rbmk_pellet_heaus", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_heaus_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_heaus", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_heaus_e3").models(Models.generated("hbm:items/rbmk_pellet_heaus", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_heaus_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_heaus", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_heaus_e4").models(Models.generated("hbm:items/rbmk_pellet_heaus", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_heaus_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_heaus", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hen_e0").models(Models.generated("hbm:items/rbmk_pellet_hen", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_hen_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_hen", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hen_e1").models(Models.generated("hbm:items/rbmk_pellet_hen", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_hen_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_hen", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hen_e2").models(Models.generated("hbm:items/rbmk_pellet_hen", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_hen_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_hen", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hen_e3").models(Models.generated("hbm:items/rbmk_pellet_hen", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_hen_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_hen", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hen_e4").models(Models.generated("hbm:items/rbmk_pellet_hen", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_hen_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_hen", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
    }

    private static void layered1() {
        Dejson.item(NS, "rbmk_pellet_hep241_e0").models(Models.generated("hbm:items/rbmk_pellet_hep241", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_hep241_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_hep241", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hep241_e1").models(Models.generated("hbm:items/rbmk_pellet_hep241", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_hep241_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_hep241", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hep241_e2").models(Models.generated("hbm:items/rbmk_pellet_hep241", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_hep241_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_hep241", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hep241_e3").models(Models.generated("hbm:items/rbmk_pellet_hep241", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_hep241_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_hep241", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hep241_e4").models(Models.generated("hbm:items/rbmk_pellet_hep241", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_hep241_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_hep241", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hep_e0").models(Models.generated("hbm:items/rbmk_pellet_hep", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_hep_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_hep", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hep_e1").models(Models.generated("hbm:items/rbmk_pellet_hep", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_hep_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_hep", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hep_e2").models(Models.generated("hbm:items/rbmk_pellet_hep", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_hep_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_hep", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hep_e3").models(Models.generated("hbm:items/rbmk_pellet_hep", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_hep_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_hep", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hep_e4").models(Models.generated("hbm:items/rbmk_pellet_hep", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_hep_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_hep", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hes_e0").models(Models.generated("hbm:items/rbmk_pellet_hes", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_hes_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_hes", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hes_e1").models(Models.generated("hbm:items/rbmk_pellet_hes", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_hes_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_hes", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hes_e2").models(Models.generated("hbm:items/rbmk_pellet_hes", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_hes_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_hes", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hes_e3").models(Models.generated("hbm:items/rbmk_pellet_hes", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_hes_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_hes", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_hes_e4").models(Models.generated("hbm:items/rbmk_pellet_hes", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_hes_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_hes", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_heu233_e0").models(Models.generated("hbm:items/rbmk_pellet_heu233", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_heu233_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_heu233", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_heu233_e1").models(Models.generated("hbm:items/rbmk_pellet_heu233", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_heu233_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_heu233", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_heu233_e2").models(Models.generated("hbm:items/rbmk_pellet_heu233", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_heu233_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_heu233", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_heu233_e3").models(Models.generated("hbm:items/rbmk_pellet_heu233", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_heu233_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_heu233", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_heu233_e4").models(Models.generated("hbm:items/rbmk_pellet_heu233", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_heu233_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_heu233", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_heu235_e0").models(Models.generated("hbm:items/rbmk_pellet_heu235", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_heu235_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_heu235", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_heu235_e1").models(Models.generated("hbm:items/rbmk_pellet_heu235", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_heu235_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_heu235", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_heu235_e2").models(Models.generated("hbm:items/rbmk_pellet_heu235", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_heu235_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_heu235", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_heu235_e3").models(Models.generated("hbm:items/rbmk_pellet_heu235", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_heu235_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_heu235", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_heu235_e4").models(Models.generated("hbm:items/rbmk_pellet_heu235", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_heu235_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_heu235", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_lea_e0").models(Models.generated("hbm:items/rbmk_pellet_lea", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_lea_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_lea", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_lea_e1").models(Models.generated("hbm:items/rbmk_pellet_lea", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_lea_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_lea", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_lea_e2").models(Models.generated("hbm:items/rbmk_pellet_lea", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_lea_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_lea", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_lea_e3").models(Models.generated("hbm:items/rbmk_pellet_lea", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_lea_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_lea", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_lea_e4").models(Models.generated("hbm:items/rbmk_pellet_lea", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_lea_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_lea", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_leaus_e0").models(Models.generated("hbm:items/rbmk_pellet_leaus", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_leaus_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_leaus", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_leaus_e1").models(Models.generated("hbm:items/rbmk_pellet_leaus", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_leaus_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_leaus", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_leaus_e2").models(Models.generated("hbm:items/rbmk_pellet_leaus", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_leaus_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_leaus", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_leaus_e3").models(Models.generated("hbm:items/rbmk_pellet_leaus", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_leaus_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_leaus", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_leaus_e4").models(Models.generated("hbm:items/rbmk_pellet_leaus", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_leaus_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_leaus", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_lep_e0").models(Models.generated("hbm:items/rbmk_pellet_lep", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_lep_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_lep", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_lep_e1").models(Models.generated("hbm:items/rbmk_pellet_lep", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_lep_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_lep", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_lep_e2").models(Models.generated("hbm:items/rbmk_pellet_lep", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_lep_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_lep", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_lep_e3").models(Models.generated("hbm:items/rbmk_pellet_lep", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_lep_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_lep", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_lep_e4").models(Models.generated("hbm:items/rbmk_pellet_lep", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_lep_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_lep", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
    }

    private static void layered2() {
        Dejson.item(NS, "rbmk_pellet_les_e0").models(Models.generated("hbm:items/rbmk_pellet_les", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_les_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_les", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_les_e1").models(Models.generated("hbm:items/rbmk_pellet_les", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_les_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_les", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_les_e2").models(Models.generated("hbm:items/rbmk_pellet_les", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_les_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_les", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_les_e3").models(Models.generated("hbm:items/rbmk_pellet_les", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_les_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_les", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_les_e4").models(Models.generated("hbm:items/rbmk_pellet_les", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_les_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_les", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mea_e0").models(Models.generated("hbm:items/rbmk_pellet_mea", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_mea_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_mea", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mea_e1").models(Models.generated("hbm:items/rbmk_pellet_mea", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_mea_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_mea", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mea_e2").models(Models.generated("hbm:items/rbmk_pellet_mea", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_mea_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_mea", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mea_e3").models(Models.generated("hbm:items/rbmk_pellet_mea", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_mea_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_mea", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mea_e4").models(Models.generated("hbm:items/rbmk_pellet_mea", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_mea_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_mea", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_men_e0").models(Models.generated("hbm:items/rbmk_pellet_men", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_men_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_men", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_men_e1").models(Models.generated("hbm:items/rbmk_pellet_men", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_men_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_men", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_men_e2").models(Models.generated("hbm:items/rbmk_pellet_men", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_men_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_men", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_men_e3").models(Models.generated("hbm:items/rbmk_pellet_men", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_men_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_men", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_men_e4").models(Models.generated("hbm:items/rbmk_pellet_men", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_men_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_men", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mep_e0").models(Models.generated("hbm:items/rbmk_pellet_mep", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_mep_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_mep", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mep_e1").models(Models.generated("hbm:items/rbmk_pellet_mep", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_mep_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_mep", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mep_e2").models(Models.generated("hbm:items/rbmk_pellet_mep", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_mep_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_mep", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mep_e3").models(Models.generated("hbm:items/rbmk_pellet_mep", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_mep_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_mep", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mep_e4").models(Models.generated("hbm:items/rbmk_pellet_mep", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_mep_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_mep", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mes_e0").models(Models.generated("hbm:items/rbmk_pellet_mes", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_mes_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_mes", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mes_e1").models(Models.generated("hbm:items/rbmk_pellet_mes", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_mes_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_mes", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mes_e2").models(Models.generated("hbm:items/rbmk_pellet_mes", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_mes_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_mes", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mes_e3").models(Models.generated("hbm:items/rbmk_pellet_mes", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_mes_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_mes", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mes_e4").models(Models.generated("hbm:items/rbmk_pellet_mes", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_mes_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_mes", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_meu_e0").models(Models.generated("hbm:items/rbmk_pellet_meu", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_meu_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_meu", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_meu_e1").models(Models.generated("hbm:items/rbmk_pellet_meu", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_meu_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_meu", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_meu_e2").models(Models.generated("hbm:items/rbmk_pellet_meu", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_meu_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_meu", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_meu_e3").models(Models.generated("hbm:items/rbmk_pellet_meu", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_meu_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_meu", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_meu_e4").models(Models.generated("hbm:items/rbmk_pellet_meu", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_meu_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_meu", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mox_e0").models(Models.generated("hbm:items/rbmk_pellet_mox", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_mox_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_mox", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mox_e1").models(Models.generated("hbm:items/rbmk_pellet_mox", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_mox_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_mox", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mox_e2").models(Models.generated("hbm:items/rbmk_pellet_mox", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_mox_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_mox", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mox_e3").models(Models.generated("hbm:items/rbmk_pellet_mox", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_mox_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_mox", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_mox_e4").models(Models.generated("hbm:items/rbmk_pellet_mox", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_mox_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_mox", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_po210be_e0").models(Models.generated("hbm:items/rbmk_pellet_po210be", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_po210be_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_po210be", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_po210be_e1").models(Models.generated("hbm:items/rbmk_pellet_po210be", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_po210be_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_po210be", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_po210be_e2").models(Models.generated("hbm:items/rbmk_pellet_po210be", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_po210be_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_po210be", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_po210be_e3").models(Models.generated("hbm:items/rbmk_pellet_po210be", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_po210be_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_po210be", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_po210be_e4").models(Models.generated("hbm:items/rbmk_pellet_po210be", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_po210be_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_po210be", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
    }

    private static void layered3() {
        Dejson.item(NS, "rbmk_pellet_pu238be_e0").models(Models.generated("hbm:items/rbmk_pellet_pu238be", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_pu238be_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_pu238be", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_pu238be_e1").models(Models.generated("hbm:items/rbmk_pellet_pu238be", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_pu238be_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_pu238be", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_pu238be_e2").models(Models.generated("hbm:items/rbmk_pellet_pu238be", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_pu238be_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_pu238be", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_pu238be_e3").models(Models.generated("hbm:items/rbmk_pellet_pu238be", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_pu238be_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_pu238be", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_pu238be_e4").models(Models.generated("hbm:items/rbmk_pellet_pu238be", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_pu238be_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_pu238be", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_ra226be_e0").models(Models.generated("hbm:items/rbmk_pellet_ra226be", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_ra226be_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_ra226be", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_ra226be_e1").models(Models.generated("hbm:items/rbmk_pellet_ra226be", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_ra226be_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_ra226be", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_ra226be_e2").models(Models.generated("hbm:items/rbmk_pellet_ra226be", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_ra226be_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_ra226be", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_ra226be_e3").models(Models.generated("hbm:items/rbmk_pellet_ra226be", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_ra226be_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_ra226be", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_ra226be_e4").models(Models.generated("hbm:items/rbmk_pellet_ra226be", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_ra226be_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_ra226be", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_thmeu_e0").models(Models.generated("hbm:items/rbmk_pellet_thmeu", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_thmeu_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_thmeu", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_thmeu_e1").models(Models.generated("hbm:items/rbmk_pellet_thmeu", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_thmeu_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_thmeu", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_thmeu_e2").models(Models.generated("hbm:items/rbmk_pellet_thmeu", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_thmeu_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_thmeu", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_thmeu_e3").models(Models.generated("hbm:items/rbmk_pellet_thmeu", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_thmeu_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_thmeu", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_thmeu_e4").models(Models.generated("hbm:items/rbmk_pellet_thmeu", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_thmeu_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_thmeu", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_ueu_e0").models(Models.generated("hbm:items/rbmk_pellet_ueu", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_ueu_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_ueu", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_ueu_e1").models(Models.generated("hbm:items/rbmk_pellet_ueu", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_ueu_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_ueu", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_ueu_e2").models(Models.generated("hbm:items/rbmk_pellet_ueu", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_ueu_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_ueu", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_ueu_e3").models(Models.generated("hbm:items/rbmk_pellet_ueu", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_ueu_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_ueu", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_ueu_e4").models(Models.generated("hbm:items/rbmk_pellet_ueu", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_ueu_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_ueu", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_uzh_e0").models(Models.generated("hbm:items/rbmk_pellet_uzh", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_uzh_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_uzh", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_uzh_e1").models(Models.generated("hbm:items/rbmk_pellet_uzh", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_uzh_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_uzh", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_uzh_e2").models(Models.generated("hbm:items/rbmk_pellet_uzh", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_uzh_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_uzh", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_uzh_e3").models(Models.generated("hbm:items/rbmk_pellet_uzh", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_uzh_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_uzh", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_uzh_e4").models(Models.generated("hbm:items/rbmk_pellet_uzh", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_uzh_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_uzh", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_am_mix_e0").models(Models.generated("hbm:items/rbmk_pellet_zfb_am_mix", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_am_mix_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_zfb_am_mix", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_am_mix_e1").models(Models.generated("hbm:items/rbmk_pellet_zfb_am_mix", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_am_mix_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_zfb_am_mix", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_am_mix_e2").models(Models.generated("hbm:items/rbmk_pellet_zfb_am_mix", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_am_mix_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_zfb_am_mix", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_am_mix_e3").models(Models.generated("hbm:items/rbmk_pellet_zfb_am_mix", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_am_mix_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_zfb_am_mix", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_am_mix_e4").models(Models.generated("hbm:items/rbmk_pellet_zfb_am_mix", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_am_mix_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_zfb_am_mix", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_bismuth_e0").models(Models.generated("hbm:items/rbmk_pellet_zfb_bismuth", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_bismuth_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_zfb_bismuth", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_bismuth_e1").models(Models.generated("hbm:items/rbmk_pellet_zfb_bismuth", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_bismuth_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_zfb_bismuth", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_bismuth_e2").models(Models.generated("hbm:items/rbmk_pellet_zfb_bismuth", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_bismuth_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_zfb_bismuth", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_bismuth_e3").models(Models.generated("hbm:items/rbmk_pellet_zfb_bismuth", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_bismuth_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_zfb_bismuth", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_bismuth_e4").models(Models.generated("hbm:items/rbmk_pellet_zfb_bismuth", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_bismuth_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_zfb_bismuth", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_pu241_e0").models(Models.generated("hbm:items/rbmk_pellet_zfb_pu241", "hbm:items/rbmk_pellet_overlay_e0")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_pu241_e0_xe").models(Models.generated("hbm:items/rbmk_pellet_zfb_pu241", "hbm:items/rbmk_pellet_overlay_e0", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_pu241_e1").models(Models.generated("hbm:items/rbmk_pellet_zfb_pu241", "hbm:items/rbmk_pellet_overlay_e1")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_pu241_e1_xe").models(Models.generated("hbm:items/rbmk_pellet_zfb_pu241", "hbm:items/rbmk_pellet_overlay_e1", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_pu241_e2").models(Models.generated("hbm:items/rbmk_pellet_zfb_pu241", "hbm:items/rbmk_pellet_overlay_e2")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_pu241_e2_xe").models(Models.generated("hbm:items/rbmk_pellet_zfb_pu241", "hbm:items/rbmk_pellet_overlay_e2", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_pu241_e3").models(Models.generated("hbm:items/rbmk_pellet_zfb_pu241", "hbm:items/rbmk_pellet_overlay_e3")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_pu241_e3_xe").models(Models.generated("hbm:items/rbmk_pellet_zfb_pu241", "hbm:items/rbmk_pellet_overlay_e3", "hbm:items/rbmk_pellet_overlay_xenon")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_pu241_e4").models(Models.generated("hbm:items/rbmk_pellet_zfb_pu241", "hbm:items/rbmk_pellet_overlay_e4")).register();
        Dejson.item(NS, "rbmk_pellet_zfb_pu241_e4_xe").models(Models.generated("hbm:items/rbmk_pellet_zfb_pu241", "hbm:items/rbmk_pellet_overlay_e4", "hbm:items/rbmk_pellet_overlay_xenon")).register();
    }

    /** Tools, which vanilla holds at a different angle; the only difference is the parent. */
    private static void tools() {
        Dejson.item(NS, "blowtorch").models(Models.handheld("hbm:items/blowtorch")).register();
        Dejson.item(NS, "defuser").models(Models.handheld("hbm:items/defuser")).register();
        Dejson.item(NS, "designator_range").models(Models.handheld("hbm:items/designator_range_alt")).register();
        Dejson.item(NS, "detonator").models(Models.handheld("hbm:items/detonator")).register();
        Dejson.item(NS, "drillbit_desh").models(Models.handheld("hbm:items/drillbit.desh")).register();
        Dejson.item(NS, "drillbit_desh_diamond").models(Models.handheld("hbm:items/drillbit.desh_diamond")).register();
        Dejson.item(NS, "drillbit_ferro").models(Models.handheld("hbm:items/drillbit.ferro")).register();
        Dejson.item(NS, "drillbit_ferro_diamond").models(Models.handheld("hbm:items/drillbit.ferro_diamond")).register();
        Dejson.item(NS, "drillbit_hss").models(Models.handheld("hbm:items/drillbit.hss")).register();
        Dejson.item(NS, "drillbit_hss_diamond").models(Models.handheld("hbm:items/drillbit.hss_diamond")).register();
        Dejson.item(NS, "drillbit_steel").models(Models.handheld("hbm:items/drillbit.steel")).register();
        Dejson.item(NS, "drillbit_steel_diamond").models(Models.handheld("hbm:items/drillbit.steel_diamond")).register();
        Dejson.item(NS, "drillbit_tcalloy").models(Models.handheld("hbm:items/drillbit.tcalloy")).register();
        Dejson.item(NS, "drillbit_tcalloy_diamond").models(Models.handheld("hbm:items/drillbit.tcalloy_diamond")).register();
        Dejson.item(NS, "hand_drill").models(Models.handheld("hbm:items/hand_drill")).register();
        Dejson.item(NS, "crowbar").models(Models.handheld("hbm:items/crowbar")).register();
        Dejson.item(NS, "rangefinder").models(Models.generated("hbm:items/rangefinder")).register();
        Dejson.item(NS, "designator_manual").models(Models.generated("hbm:items/designator_manual")).register();
        for (String id : new String[]{"can_smart", "can_creature", "can_redbomb", "can_mrsugar", "can_overcharge",
                "can_luna", "can_bepis", "can_breen", "can_mug", "bottle_nuka", "bottle_cherry",
                "bottle_quantum", "bottle_sparkle", "bottle_rad", "bottle2_korl", "bottle2_fritz",
                "coffee", "coffee_radium", "bottle_opener"})
            Dejson.item(NS, id).models(Models.generated("hbm:items/" + id)).register();
        Dejson.item(NS, "igniter").models(Models.handheld("hbm:items/trigger")).register();
        Dejson.item(NS, "key_red").models(Models.handheld("hbm:items/key_red")).register();
        Dejson.item(NS, "mech_key").models(Models.handheld("hbm:items/mech_key")).register();
        Dejson.item(NS, "mirror_tool").models(Models.handheld("hbm:items/mirror_tool")).register();
        Dejson.item(NS, "overfuse").models(Models.handheld("hbm:items/overfuse")).register();
        Dejson.item(NS, "reacher").models(Models.handheld("hbm:items/reacher")).register();
        Dejson.item(NS, "screwdriver").models(Models.handheld("hbm:items/screwdriver")).register();
    }

    /**
     * The two base models an OBJ machine item wears its frame on.
     *
     * <p>A machine icon is a frame times a body. The body is the item's own verbatim 1.7
     * {@code renderInventory}/{@code renderCommon} numbers, which its item definition carries and
     * {@code MachineItemModel} applies at draw time. The frame is what 1.7's {@code ItemRenderBase}
     * wrapped around every such body, and it is shared — so it lives here, once, and every machine
     * inherits it by pointing at one of these two models.
     *
     * <p>Two spellings of one frame because the renderer differs, not the pose. 26.1 reads display
     * properties off {@code machine_obj_context_base} through the special-model system; below 1.21.4
     * the same items are routed to a per-loader BEWLR, which vanilla only reaches for a model whose
     * parent chain ends at {@code minecraft:builtin/entity}.
     *
     * <p>Composed rather than written out. A {@code display{}} entry is a translate/rotate/scale triple
     * applied as {@code T . R . S}; the 1.7 chains are not in that shape until
     * {@link dev.golem.core.render.display.DisplayFrame} decomposes them, and copying one field by
     * field is how these icons ended up most of a slot too high.
     */
    private static void machineFrames() {
        machineFrame("machine", Pose17.Klass.ITEM_RENDER_BASE);
        machineFrame("missile", Pose17.Klass.ITEM_RENDER_MISSILE);
        gunFrame();
    }

    /**
     * The one base model every Sedna gun wears.
     *
     * <p>{@link Pose17.Klass#ITEM_RENDER_WEAPON} is the engine slot alone, so this carries none of
     * {@code ItemRenderWeaponBase}'s setup chain and none of any weapon's own: those are calls, and they
     * live in {@code WeaponRenderer}. What this replaces is a hand-fitted {@code display{}} that stood
     * in for all of it — one pose shared by sixty-six guns, in place of the fifty-two the oracle has.
     */
    private static void gunFrame() {
        Dejson.item(NS, "gun_obj_base")
                .models(Models.guiLight("side", Models.display(Pose17.frames(Pose17.Klass.ITEM_RENDER_WEAPON),
                        Models.parented("hbm:item/gun_obj_legacy", Slots.EMPTY))))
                .register();
    }

    /**
     * One 1.7 item-render class's frame, in the two spellings. {@code missile} is
     * {@code ItemRenderMissileGeneric}, which is not an {@code ItemRenderBase} at all: it wraps the body in
     * nothing, so its GUI frame is the bare 1.7 pixel slot and its hand poses end on that renderer's own
     * equipped preamble.
     */
    private static void machineFrame(String name, Pose17.Klass klass) {
        Map<DisplaySlot, Matrix4f> frame = Pose17.frames(klass);
        Dejson.item(NS, name + "_obj_context_base")
                .models(Models.guiLight("side", Models.display(frame,
                        Models.parented("minecraft:block/block",
                                Slots.of("particle", "hbm:blocks/block_steel")))))
                .register();
        Dejson.item(NS, name + "_obj_context_legacy")
                .models(Models.guiLight("side", Models.display(frame,
                        Models.parented("minecraft:builtin/entity", Slots.EMPTY))))
                .register();
    }

    /**
     * The funnel's inventory icon is the old block OBJ with separate Top/Bottom/Side skins. Dejson's
     * ordinary item vocabulary intentionally does not claim renderer-owned special models, so this
     * declaration owns both the legacy marker and the special-model definition from the same registry.
     * {@link Models#blockItem(String)} emits the {@code builtin/entity} marker directly, while the
     * legacy-assets pass also recognises the special definition and keeps that marker authoritative.
     */
    private static void funnelModel() {
        Dejson.item(NS, "machine_funnel")
                .models(Models.blockItem("hbm:item/machine_obj_legacy"))
                .register();

        // Dejson.itemDefinition returns the object stored in its definition registry (not a copy), and
        // DejsonWriter writes that same object. This is the narrow escape hatch for the custom OBJ
        // definition that the generic model-shape vocabulary deliberately cannot represent.
        JsonObject definition = Dejson.itemDefinition("hbm:item/machine_funnel");
        JsonObject special = new JsonObject();
        special.addProperty("type", "minecraft:special");
        special.addProperty("base", "hbm:item/machine_obj_base");

        JsonObject machine = new JsonObject();
        machine.addProperty("type", "hbm:machine_obj");
        machine.addProperty("obj", "hbm:models/blocks/funnel.obj");
        machine.addProperty("texture", "hbm:textures/blocks/machine_funnel_side.png");
        JsonObject partTextures = new JsonObject();
        partTextures.addProperty("Top", "hbm:textures/blocks/machine_funnel_top.png");
        partTextures.addProperty("Bottom", "hbm:textures/blocks/machine_funnel_bottom.png");
        partTextures.addProperty("Side", "hbm:textures/blocks/machine_funnel_side.png");
        machine.add("partTextures", partTextures);
        machine.addProperty("scale", 1.0F);
        machine.addProperty("yaw", 0.0F);
        machine.addProperty("ty", -0.5F);
        special.add("model", machine);
        definition.add("model", special);
    }

    /**
     * The ntm-next entity wave's items ({@link ModEntityItems}), drawn from the 1.7 sprites: the
     * {@code ItemEnumMulti} families keep 1.7's {@code <base>.<constant>} sprite names, and the cart is
     * 1.7's two passes, chassis under overlay. The chassis pass is fixed to the vanilla sprite: the
     * {@code CART_BASE} component has no select vocabulary here, so a wood/steel/painted chassis draws
     * as vanilla in hand (the entity itself is unaffected).
     */
    private static void entityItems() {
        for (String name : new String[] {"chopper", "spawn_worm", "spawn_ufo", "spawn_duck", "boat_rubber",
                "coupling_tool", "stick_dynamite", "disperser_canister", "glyphid_gland", "plastic_bag",
                "bottled_cloud", "grenade_universal"}) {
            Dejson.item(NS, name).models(Models.generated("hbm:items/" + name)).register();
        }
        Dejson.item(NS, "battery_creative").models(Models.generated("hbm:items/battery_creative_new")).register();
        for (String cart : new String[] {"empty", "crate", "destroyer", "powder", "semtex"}) {
            Dejson.item(NS, "cart_" + cart)
                    .models(Models.generated("hbm:items/cart.vanilla", "hbm:items/cart_overlay." + cart)).register();
        }
        for (String drone : new String[] {"patrol", "patrol_chunkloading", "patrol_express",
                "patrol_express_chunkloading", "request"}) {
            Dejson.item(NS, "drone_" + drone).models(Models.generated("hbm:items/drone." + drone)).register();
        }
        for (String train : new String[] {"cargo_tram", "cargo_tram_trailer"}) {
            Dejson.item(NS, "train_" + train).models(Models.generated("hbm:items/train." + train)).register();
        }
        for (String shell : new String[] {"frag", "nuke", "stick", "tech"}) {
            Dejson.item(NS, "grenade_shell_" + shell).models(Models.generated("hbm:items/grenade_shell." + shell)).register();
        }
        for (String filling : new String[] {"cluster", "cluster_heavy", "demo", "emp", "he", "inc", "laser", "nuclear",
                "nuclear_demo", "plasma", "powder", "schrab", "wp"}) {
            Dejson.item(NS, "grenade_filling_" + filling)
                    .models(Models.generated("hbm:items/grenade_filling." + filling)).register();
        }
        for (String fuze : new String[] {"airburst", "impact", "s15", "s3", "s7"}) {
            Dejson.item(NS, "grenade_fuze_" + fuze).models(Models.generated("hbm:items/grenade_fuze." + fuze)).register();
        }
        for (String extra : new String[] {"frag_sleeve", "glue", "proxy_fuze", "triplex"}) {
            Dejson.item(NS, "grenade_extra_" + extra).models(Models.generated("hbm:items/grenade_extra." + extra)).register();
        }
    }

    /**
     * Block items whose block still loads its model from a file: the item model is a bare parent
     * pointing at that model, and there is nothing about the block itself to declare.
     */
    private static void blockItems() {
        Dejson.item(NS, "fallout").models(Models.blockItem("hbm:block/fallout")).register();
        Dejson.item(NS, "foam_layer_height2").models(Models.blockItem("hbm:block/foam_layer_height2")).register();
        Dejson.item(NS, "jetpack_glider_base").models(Models.blockItem("hbm:item/machine_obj_base")).register();
        Dejson.item(NS, "ladder_aluminium").models(Models.blockItem("hbm:block/ladder_aluminium")).register();
        Dejson.item(NS, "ladder_cobalt").models(Models.blockItem("hbm:block/ladder_cobalt")).register();
        Dejson.item(NS, "ladder_copper").models(Models.blockItem("hbm:block/ladder_copper")).register();
        Dejson.item(NS, "ladder_gold").models(Models.blockItem("hbm:block/ladder_gold")).register();
        Dejson.item(NS, "ladder_iron").models(Models.blockItem("hbm:block/ladder_iron")).register();
        Dejson.item(NS, "ladder_lead").models(Models.blockItem("hbm:block/ladder_lead")).register();
        Dejson.item(NS, "ladder_red").models(Models.blockItem("hbm:block/ladder_red")).register();
        Dejson.item(NS, "ladder_red_top").models(Models.blockItem("hbm:block/ladder_red_top")).register();
        Dejson.item(NS, "ladder_steel").models(Models.blockItem("hbm:block/ladder_steel")).register();
        Dejson.item(NS, "ladder_sturdy").models(Models.blockItem("hbm:block/ladder_sturdy")).register();
        Dejson.item(NS, "ladder_titanium").models(Models.blockItem("hbm:block/ladder_titanium")).register();
        Dejson.item(NS, "ladder_tungsten").models(Models.blockItem("hbm:block/ladder_tungsten")).register();
        Dejson.item(NS, "ore_sellafield_diamond").models(Models.blockItem("hbm:block/ore_sellafield_diamond_0")).register();
        Dejson.item(NS, "ore_sellafield_emerald").models(Models.blockItem("hbm:block/ore_sellafield_emerald_0")).register();
        Dejson.item(NS, "ore_sellafield_radgem").models(Models.blockItem("hbm:block/ore_sellafield_radgem_0")).register();
        Dejson.item(NS, "ore_sellafield_schrabidium").models(Models.blockItem("hbm:block/ore_sellafield_schrabidium_0")).register();
        Dejson.item(NS, "ore_sellafield_uranium_scorched").models(Models.blockItem("hbm:block/ore_sellafield_uranium_scorched_0")).register();
        Dejson.item(NS, "sellafield_bedrock").models(Models.blockItem("hbm:block/sellafield_bedrock_0")).register();
        Dejson.item(NS, "sellafield_slaked").models(Models.blockItem("hbm:block/sellafield_slaked_0")).register();
        Dejson.item(NS, "steel_beam").models(Models.blockItem("hbm:block/steel_beam")).register();
        Dejson.item(NS, "steel_scaffold_red").models(Models.blockItem("hbm:item/machine_obj_legacy")).register();
        Dejson.item(NS, "steel_scaffold_steel").models(Models.blockItem("hbm:item/machine_obj_legacy")).register();
        Dejson.item(NS, "steel_scaffold_white").models(Models.blockItem("hbm:item/machine_obj_legacy")).register();
        Dejson.item(NS, "steel_scaffold_yellow").models(Models.blockItem("hbm:item/machine_obj_legacy")).register();
    }
}
