package com.hbm.inventory.recipes;

import com.hbm.lib.ItemIds;
import com.hbm.lib.TagIds;
import com.hbm.lib.VanillaIds;

import static dev.golem.core.dejson.Crafting.blast;
import static dev.golem.core.dejson.Crafting.compress;
import static dev.golem.core.dejson.Crafting.decompress;
import static dev.golem.core.dejson.Crafting.smelt;

/**
 * The hbm recipes whose shape carries no decision, declared through Golem Core's {@code dejson}
 * vocabulary instead of written as JSON.
 *
 * <p>This class stands in for {@value #FILES_REPLACED} recipe files: 191 that pack nine of something into
 * a block, 62 that unpack it again, and 120 furnace recipes. Each is a fixed arrangement around two ids
 * and a number: a full three-by-three grid of one ingredient, a single shapeless ingredient making nine,
 * two hundred ticks and an experience value.
 *
 * <p>The rest of the mod's crafting is in {@link ModShapedRecipes}, {@link ModShapelessRecipes} and
 * {@link ModComponentRecipes}. Nothing under {@code data/hbm/recipe/} is written by hand; those three add
 * a pattern and a set of symbols, the part of a recipe somebody actually decided.
 *
 * <p>Recipe ids are not derived from the output. They are visible (the recipe book remembers them, JEI
 * bookmarks them), so renaming {@code ingot_actinium_1} to something tidier would reset progress for no gain.
 */
public final class ModCraftingRecipes {

    private static final String NS = "hbm";

    /** How many recipe files the declarations below stand in for; quoted in this class's own javadoc. */
    static final int FILES_REPLACED = 373;

    private static boolean declared;

    private ModCraftingRecipes() {}

    public static void declare() {
        if (declared) {
            return;
        }
        declared = true;

        packing0();
        packing1();
        unpacking();
        rawNuggets();
        smelting0();
        smelting1();
        rawOres();
    }

    /** port: ntm-next's raw ores smelt and blast to one of what their ore smelts to. */
    private static void rawOres() {
        rawOre("aluminium", ItemIds.chunk_ore_cryolite, 2.5);
        rawOre("beryllium", ItemIds.ingot_beryllium, 2.0);
        rawOre("lead", ItemIds.ingot_lead, 3.0);
        rawOre("thorium", ItemIds.ingot_th232, 3.0);
        rawOre("titanium", ItemIds.ingot_titanium, 3.0);
        rawOre("tungsten", ItemIds.ingot_tungsten, 6.0);
        rawOre("uranium", ItemIds.ingot_uranium, 6.0);
        rawOre("plutonium", ItemIds.ingot_plutonium, 24.0);
        rawOre("cobalt", ItemIds.ingot_cobalt, 2.0);
        rawOre("schrabidium", ItemIds.ingot_schrabidium, 128.0);
        rawOre("lithium", ItemIds.lithium, 10.0);
    }

    private static void rawOre(String material, String result, double experience) {
        String raw = "hbm:raw_" + material;
        smelt(NS, "smelting/raw_" + material, raw, result, experience);
        blast(NS, "blasting/raw_" + material, raw, result, experience);
    }

    private static void packing0() {
        compress(NS, "bedrock_ore_fragment_aluminium_compression", TagIds.fragments_aluminium, ItemIds.powder_aluminium);
        compress(NS, "bedrock_ore_fragment_asbestos_compression", TagIds.fragments_asbestos, ItemIds.powder_asbestos);
        compress(NS, "bedrock_ore_fragment_bauxite_compression", TagIds.fragments_bauxite, ItemIds.stone_resource_bauxite);
        compress(NS, "bedrock_ore_fragment_beryllium_compression", TagIds.fragments_beryllium, ItemIds.powder_beryllium);
        compress(NS, "bedrock_ore_fragment_bismuth_compression", TagIds.fragments_bismuth, ItemIds.powder_bismuth);
        compress(NS, "bedrock_ore_fragment_borax_compression", TagIds.fragments_borax, ItemIds.powder_borax);
        compress(NS, "bedrock_ore_fragment_boron_compression", TagIds.fragments_boron, ItemIds.powder_boron);
        compress(NS, "bedrock_ore_fragment_chlorocalcite_compression", TagIds.fragments_chlorocalcite, ItemIds.powder_chlorocalcite);
        compress(NS, "bedrock_ore_fragment_cinnabar_compression", TagIds.fragments_cinnabar, ItemIds.cinnabar);
        compress(NS, "bedrock_ore_fragment_coal_compression", TagIds.fragments_coal, ItemIds.powder_coal);
        compress(NS, "bedrock_ore_fragment_cobalt_compression", TagIds.fragments_cobalt, ItemIds.powder_cobalt);
        compress(NS, "bedrock_ore_fragment_copper_compression", TagIds.fragments_copper, ItemIds.powder_copper);
        compress(NS, "bedrock_ore_fragment_cryolite_compression", TagIds.fragments_cryolite, ItemIds.chunk_ore_cryolite);
        compress(NS, "bedrock_ore_fragment_diamond_compression", TagIds.fragments_diamond, ItemIds.powder_diamond);
        compress(NS, "bedrock_ore_fragment_emerald_compression", TagIds.fragments_emerald, ItemIds.powder_emerald);
        compress(NS, "bedrock_ore_fragment_fluorite_compression", TagIds.fragments_fluorite, ItemIds.fluorite);
        compress(NS, "bedrock_ore_fragment_gold_compression", TagIds.fragments_gold, ItemIds.powder_gold);
        compress(NS, "bedrock_ore_fragment_ilmenite_compression", "#c:fragments/ilmenite", "hbm:chunk_ore_ilmenite");
        compress(NS, "bedrock_ore_fragment_iron_compression", TagIds.fragments_iron, ItemIds.powder_iron);
        compress(NS, "bedrock_ore_fragment_lanthanium_compression", TagIds.fragments_lanthanium, ItemIds.powder_lanthanium);
        compress(NS, "bedrock_ore_fragment_lead_compression", TagIds.fragments_lead, ItemIds.powder_lead);
        compress(NS, "bedrock_ore_fragment_lignite_compression", TagIds.fragments_lignite, ItemIds.powder_lignite);
        compress(NS, "bedrock_ore_fragment_lithium_compression", TagIds.fragments_lithium, ItemIds.powder_lithium);
        compress(NS, "bedrock_ore_fragment_molysite_compression", TagIds.fragments_molysite, ItemIds.powder_molysite);
        compress(NS, "bedrock_ore_fragment_neodymium_compression", TagIds.fragments_neodymium, ItemIds.powder_neodymium);
        compress(NS, "bedrock_ore_fragment_niobium_compression", TagIds.fragments_niobium, ItemIds.powder_niobium);
        compress(NS, "bedrock_ore_fragment_niter_compression", TagIds.fragments_niter, ItemIds.niter);
        compress(NS, "bedrock_ore_fragment_polonium_compression", TagIds.fragments_polonium, ItemIds.powder_polonium);
        compress(NS, "bedrock_ore_fragment_ra226_compression", TagIds.fragments_ra226, ItemIds.powder_ra226);
        compress(NS, "bedrock_ore_fragment_rare_compression", TagIds.fragments_rare, ItemIds.chunk_ore_rare);
        compress(NS, "bedrock_ore_fragment_red_phosphorus_compression", TagIds.fragments_red_phosphorus, ItemIds.powder_fire);
        compress(NS, "bedrock_ore_fragment_redstone_compression", TagIds.fragments_redstone, VanillaIds.redstone);
        compress(NS, "bedrock_ore_fragment_silicon_compression", TagIds.fragments_silicon, ItemIds.ingot_silicon);
        compress(NS, "bedrock_ore_fragment_sodalite_compression", TagIds.fragments_sodalite, ItemIds.gem_sodalite);
        compress(NS, "bedrock_ore_fragment_sodium_compression", TagIds.fragments_sodium, ItemIds.powder_sodium);
        compress(NS, "bedrock_ore_fragment_strontium_compression", TagIds.fragments_strontium, ItemIds.powder_strontium);
        compress(NS, "bedrock_ore_fragment_sulfur_compression", TagIds.fragments_sulfur, ItemIds.sulfur);
        compress(NS, "bedrock_ore_fragment_tantalium_compression", TagIds.fragments_tantalium, ItemIds.powder_tantalium);
        compress(NS, "bedrock_ore_fragment_technetium_compression", TagIds.fragments_technetium, ItemIds.ingot_technetium);
        compress(NS, "bedrock_ore_fragment_thorium_compression", TagIds.fragments_thorium, ItemIds.powder_thorium);
        compress(NS, "bedrock_ore_fragment_titanium_compression", TagIds.fragments_titanium, ItemIds.powder_titanium);
        compress(NS, "bedrock_ore_fragment_tungsten_compression", TagIds.fragments_tungsten, ItemIds.powder_tungsten);
        compress(NS, "bedrock_ore_fragment_u238_compression", TagIds.fragments_u238, ItemIds.ingot_u238);
        compress(NS, "bedrock_ore_fragment_uranium_compression", TagIds.fragments_uranium, ItemIds.powder_uranium);
        compress(NS, "bedrock_ore_fragment_zirconium_compression", TagIds.fragments_zirconium, ItemIds.powder_zirconium);
        compress(NS, "block_actinium", ItemIds.ingot_actinium, ItemIds.block_actinium);
        compress(NS, "block_aluminium", ItemIds.ingot_aluminium, ItemIds.block_aluminium);
        compress(NS, "block_asbestos", ItemIds.ingot_asbestos, ItemIds.block_asbestos);
        compress(NS, "block_bakelite", ItemIds.ingot_bakelite, ItemIds.block_bakelite);
        compress(NS, "block_beryllium", ItemIds.ingot_beryllium, ItemIds.block_beryllium);
        compress(NS, "block_bismuth", ItemIds.ingot_bismuth, ItemIds.block_bismuth);
        compress(NS, "block_boron", ItemIds.ingot_boron, ItemIds.block_boron);
        compress(NS, "block_cadmium", ItemIds.ingot_cadmium, ItemIds.block_cadmium);
        compress(NS, "block_cobalt", ItemIds.ingot_cobalt, ItemIds.block_cobalt);
        compress(NS, "block_coltan", ItemIds.fragment_coltan, ItemIds.block_coltan);
        compress(NS, "block_combine_steel", ItemIds.ingot_combine_steel, ItemIds.block_combine_steel);
        compress(NS, "block_desh", ItemIds.ingot_desh, ItemIds.block_desh);
        compress(NS, "block_dineutronium", ItemIds.ingot_dineutronium, ItemIds.block_dineutronium);
        compress(NS, "block_dura_steel", ItemIds.ingot_dura_steel, ItemIds.block_dura_steel);
        compress(NS, "block_euphemium", ItemIds.ingot_euphemium, ItemIds.block_euphemium);
        compress(NS, "block_fluorite", ItemIds.fluorite, ItemIds.block_fluorite);
        compress(NS, "block_graphite", ItemIds.ingot_graphite, ItemIds.block_graphite);
        compress(NS, "block_lanthanium", ItemIds.ingot_lanthanium, ItemIds.block_lanthanium);
        compress(NS, "block_lead", ItemIds.ingot_lead, ItemIds.block_lead);
        compress(NS, "block_magnetized_tungsten", ItemIds.ingot_magnetized_tungsten, ItemIds.block_magnetized_tungsten);
        compress(NS, "block_meteor_broken", ItemIds.fragment_meteorite, ItemIds.block_meteor_broken);
        compress(NS, "block_mox_fuel", ItemIds.ingot_mox_fuel, ItemIds.block_mox_fuel);
        compress(NS, "block_neptunium", ItemIds.ingot_neptunium, ItemIds.block_neptunium);
        compress(NS, "block_niobium", ItemIds.ingot_niobium, ItemIds.block_niobium);
        compress(NS, "block_niter", ItemIds.niter, ItemIds.block_niter);
        compress(NS, "block_plutonium", ItemIds.ingot_plutonium, ItemIds.block_plutonium);
        compress(NS, "block_plutonium_fuel", ItemIds.ingot_plutonium_fuel, ItemIds.block_plutonium_fuel);
        compress(NS, "block_polonium", ItemIds.ingot_polonium, ItemIds.block_polonium);
        compress(NS, "block_polymer", ItemIds.ingot_polymer, ItemIds.block_polymer);
        compress(NS, "block_pu238", ItemIds.ingot_pu238, ItemIds.block_pu238);
        compress(NS, "block_pu239", ItemIds.ingot_pu239, ItemIds.block_pu239);
        compress(NS, "block_pu240", ItemIds.ingot_pu240, ItemIds.block_pu240);
        compress(NS, "block_pu_mix", ItemIds.ingot_pu_mix, ItemIds.block_pu_mix);
        compress(NS, "block_ra226", ItemIds.ingot_ra226, ItemIds.block_ra226);
        compress(NS, "block_red_copper", ItemIds.ingot_red_copper, ItemIds.block_red_copper);
        compress(NS, "block_red_phosphorus", ItemIds.powder_fire, ItemIds.block_red_phosphorus);
        compress(NS, "block_rubber", ItemIds.ingot_rubber, ItemIds.block_rubber);
        compress(NS, "block_saturnite", ItemIds.ingot_saturnite, ItemIds.block_saturnite);
        compress(NS, "block_schrabidate", ItemIds.ingot_schrabidate, ItemIds.block_schrabidate);
        compress(NS, "block_schrabidium", ItemIds.ingot_schrabidium, ItemIds.block_schrabidium);
        compress(NS, "block_schrabidium_fuel", ItemIds.ingot_schrabidium_fuel, ItemIds.block_schrabidium_fuel);
        compress(NS, "block_schraranium", ItemIds.ingot_schraranium, ItemIds.block_schraranium);
        compress(NS, "block_scrap_1", ItemIds.dust, ItemIds.block_scrap);
        compress(NS, "block_slag", ItemIds.ingot_raw_slag, ItemIds.block_slag);
        compress(NS, "block_solinium", ItemIds.ingot_solinium, ItemIds.block_solinium);
        compress(NS, "block_starmetal", ItemIds.ingot_starmetal, ItemIds.block_starmetal);
        compress(NS, "block_steel", ItemIds.ingot_steel, ItemIds.block_steel);
        compress(NS, "block_sulfur", ItemIds.sulfur, ItemIds.block_sulfur);
        compress(NS, "block_tantalium", ItemIds.ingot_tantalium, ItemIds.block_tantalium);
        compress(NS, "block_thorium", ItemIds.ingot_th232, ItemIds.block_thorium);
    }

    private static void packing1() {
        compress(NS, "block_thorium_fuel", ItemIds.ingot_thorium_fuel, ItemIds.block_thorium_fuel);
        compress(NS, "block_titanium", ItemIds.ingot_titanium, ItemIds.block_titanium);
        compress(NS, "block_trinitite", ItemIds.trinitite, ItemIds.block_trinitite);
        compress(NS, "block_tungsten", ItemIds.ingot_tungsten, ItemIds.block_tungsten);
        compress(NS, "block_u233", ItemIds.ingot_u233, ItemIds.block_u233);
        compress(NS, "block_u235", ItemIds.ingot_u235, ItemIds.block_u235);
        compress(NS, "block_u238", ItemIds.ingot_u238, ItemIds.block_u238);
        compress(NS, "block_uranium", ItemIds.ingot_uranium, ItemIds.block_uranium);
        compress(NS, "block_uranium_fuel", ItemIds.ingot_uranium_fuel, ItemIds.block_uranium_fuel);
        compress(NS, "block_white_phosphorus", ItemIds.ingot_phosphorus, ItemIds.block_white_phosphorus);
        compress(NS, "block_zirconium", ItemIds.ingot_zirconium, ItemIds.block_zirconium);
        compress(NS, "dust", ItemIds.dust_tiny, ItemIds.dust);
        compress(NS, "egg_balefire", ItemIds.egg_balefire_shard, ItemIds.egg_balefire);
        compress(NS, "egg_balefire_shard_2", ItemIds.cell_balefire, ItemIds.egg_balefire_shard);
        compress(NS, "gold_ingot", TagIds.wires_gold, VanillaIds.gold_ingot);
        compress(NS, "ingot_actinium_2", ItemIds.nugget_actinium, ItemIds.ingot_actinium);
        compress(NS, "ingot_aluminium", TagIds.wires_aluminium, ItemIds.ingot_aluminium);
        compress(NS, "ingot_am241", ItemIds.nugget_am241, ItemIds.ingot_am241);
        compress(NS, "ingot_am242", ItemIds.nugget_am242, ItemIds.ingot_am242);
        compress(NS, "ingot_am_mix", ItemIds.nugget_am_mix, ItemIds.ingot_am_mix);
        compress(NS, "ingot_americium_fuel", ItemIds.nugget_americium_fuel, ItemIds.ingot_americium_fuel);
        compress(NS, "ingot_arsenic", ItemIds.nugget_arsenic, ItemIds.ingot_arsenic);
        compress(NS, "ingot_au198_1", ItemIds.nugget_au198, ItemIds.ingot_au198);
        compress(NS, "ingot_australium_2", ItemIds.nugget_australium, ItemIds.ingot_australium);
        compress(NS, "ingot_beryllium_5", ItemIds.nugget_beryllium, ItemIds.ingot_beryllium);
        compress(NS, "ingot_bismuth_1", ItemIds.nugget_bismuth, ItemIds.ingot_bismuth);
        compress(NS, "ingot_co60", ItemIds.nugget_co60, ItemIds.ingot_co60);
        compress(NS, "ingot_copper", TagIds.wires_copper, VanillaIds.copper_ingot);
        compress(NS, "ingot_dineutronium_1", ItemIds.nugget_dineutronium, ItemIds.ingot_dineutronium);
        compress(NS, "ingot_euphemium_2", ItemIds.nugget_euphemium, ItemIds.ingot_euphemium);
        compress(NS, "ingot_gh336", ItemIds.nugget_gh336, ItemIds.ingot_gh336);
        compress(NS, "ingot_graphite", TagIds.wires_carbon, ItemIds.ingot_graphite);
        compress(NS, "ingot_hes_1", ItemIds.nugget_hes, ItemIds.ingot_hes);
        compress(NS, "ingot_lead_4", ItemIds.nugget_lead, ItemIds.ingot_lead);
        compress(NS, "ingot_les_1", ItemIds.nugget_les, ItemIds.ingot_les);
        compress(NS, "ingot_magnetized_tungsten", TagIds.wires_magnetized_tungsten, ItemIds.ingot_magnetized_tungsten);
        compress(NS, "ingot_mox_fuel_2", ItemIds.nugget_mox_fuel, ItemIds.ingot_mox_fuel);
        compress(NS, "ingot_neptunium_3", ItemIds.nugget_neptunium, ItemIds.ingot_neptunium);
        compress(NS, "ingot_neptunium_fuel", ItemIds.nugget_neptunium_fuel, ItemIds.ingot_neptunium_fuel);
        compress(NS, "ingot_niobium_1", ItemIds.nugget_niobium, ItemIds.ingot_niobium);
        compress(NS, "ingot_osmiridium_1", ItemIds.nugget_osmiridium, ItemIds.ingot_osmiridium);
        compress(NS, "ingot_pb209", ItemIds.nugget_pb209, ItemIds.ingot_pb209);
        compress(NS, "ingot_plutonium_6", ItemIds.nugget_plutonium, ItemIds.ingot_plutonium);
        compress(NS, "ingot_plutonium_fuel_2", ItemIds.nugget_plutonium_fuel, ItemIds.ingot_plutonium_fuel);
        compress(NS, "ingot_polonium_3", ItemIds.nugget_polonium, ItemIds.ingot_polonium);
        compress(NS, "ingot_pu238_2", ItemIds.nugget_pu238, ItemIds.ingot_pu238);
        compress(NS, "ingot_pu239_2", ItemIds.nugget_pu239, ItemIds.ingot_pu239);
        compress(NS, "ingot_pu240_2", ItemIds.nugget_pu240, ItemIds.ingot_pu240);
        compress(NS, "ingot_pu241", ItemIds.nugget_pu241, ItemIds.ingot_pu241);
        compress(NS, "ingot_pu_mix", ItemIds.nugget_pu_mix, ItemIds.ingot_pu_mix);
        compress(NS, "ingot_ra226_2", ItemIds.nugget_ra226, ItemIds.ingot_ra226);
        compress(NS, "ingot_red_copper", TagIds.wires_red_copper, ItemIds.ingot_red_copper);
        compress(NS, "ingot_schrabidium", TagIds.wires_schrabidium, ItemIds.ingot_schrabidium);
        compress(NS, "ingot_schrabidium_8", ItemIds.nugget_schrabidium, ItemIds.ingot_schrabidium);
        compress(NS, "ingot_schrabidium_fuel_2", ItemIds.nugget_schrabidium_fuel, ItemIds.ingot_schrabidium_fuel);
        compress(NS, "ingot_silicon", ItemIds.nugget_silicon, ItemIds.ingot_silicon);
        compress(NS, "ingot_solinium_2", ItemIds.nugget_solinium, ItemIds.ingot_solinium);
        compress(NS, "ingot_sr90_1", ItemIds.nugget_sr90, ItemIds.ingot_sr90);
        compress(NS, "ingot_tantalium_1", ItemIds.nugget_tantalium, ItemIds.ingot_tantalium);
        compress(NS, "ingot_technetium", ItemIds.nugget_technetium, ItemIds.ingot_technetium);
        compress(NS, "ingot_th232_5", ItemIds.nugget_th232, ItemIds.ingot_th232);
        compress(NS, "ingot_thorium_fuel_2", ItemIds.nugget_thorium_fuel, ItemIds.ingot_thorium_fuel);
        compress(NS, "ingot_tungsten", TagIds.wires_tungsten, ItemIds.ingot_tungsten);
        compress(NS, "ingot_u233_2", ItemIds.nugget_u233, ItemIds.ingot_u233);
        compress(NS, "ingot_u235_2", ItemIds.nugget_u235, ItemIds.ingot_u235);
        compress(NS, "ingot_u238_2", ItemIds.nugget_u238, ItemIds.ingot_u238);
        compress(NS, "ingot_uranium_10", ItemIds.nugget_uranium, ItemIds.ingot_uranium);
        compress(NS, "ingot_uranium_fuel_2", ItemIds.nugget_uranium_fuel, ItemIds.ingot_uranium_fuel);
        compress(NS, "ingot_zirconium_1", ItemIds.nugget_zirconium, ItemIds.ingot_zirconium);
        compress(NS, "nuclear_waste_1", ItemIds.nuclear_waste_tiny, ItemIds.nuclear_waste);
        compress(NS, "nuclear_waste_vitrified", ItemIds.nuclear_waste_vitrified_tiny, ItemIds.nuclear_waste_vitrified);
        compress(NS, "nugget_mercury", ItemIds.nugget_mercury_tiny, ItemIds.nugget_mercury);
        compress(NS, "powder_actinium", ItemIds.powder_actinium_tiny, ItemIds.powder_actinium);
        compress(NS, "powder_boron", ItemIds.powder_boron_tiny, ItemIds.powder_boron);
        compress(NS, "powder_cerium", ItemIds.powder_cerium_tiny, ItemIds.powder_cerium);
        compress(NS, "powder_coal", ItemIds.powder_coal_tiny, ItemIds.powder_coal);
        compress(NS, "powder_cobalt", ItemIds.powder_cobalt_tiny, ItemIds.powder_cobalt);
        compress(NS, "powder_cs137", ItemIds.powder_cs137_tiny, ItemIds.powder_cs137);
        compress(NS, "powder_i131", ItemIds.powder_i131_tiny, ItemIds.powder_i131);
        compress(NS, "powder_lanthanium", ItemIds.powder_lanthanium_tiny, ItemIds.powder_lanthanium);
        compress(NS, "powder_lithium", ItemIds.powder_lithium_tiny, ItemIds.powder_lithium);
        compress(NS, "powder_meteorite", ItemIds.powder_meteorite_tiny, ItemIds.powder_meteorite);
        compress(NS, "powder_neodymium", ItemIds.powder_neodymium_tiny, ItemIds.powder_neodymium);
        compress(NS, "powder_niobium", ItemIds.powder_niobium_tiny, ItemIds.powder_niobium);
        compress(NS, "powder_paleogenite", ItemIds.powder_paleogenite_tiny, ItemIds.powder_paleogenite);
        compress(NS, "powder_sr90", ItemIds.powder_sr90_tiny, ItemIds.powder_sr90);
        compress(NS, "powder_steel", ItemIds.powder_steel_tiny, ItemIds.powder_steel);
        compress(NS, "powder_xe135", ItemIds.powder_xe135_tiny, ItemIds.powder_xe135);
    }

    private static void unpacking() {
        decompress(NS, "dust_tiny", ItemIds.dust, ItemIds.dust_tiny);
        decompress(NS, "fragment_coltan", ItemIds.block_coltan, ItemIds.fragment_coltan);
        decompress(NS, "ingot_actinium_1", ItemIds.block_actinium, ItemIds.ingot_actinium);
        decompress(NS, "ingot_aluminium_3", ItemIds.block_aluminium, ItemIds.ingot_aluminium);
        decompress(NS, "ingot_bakelite_1", ItemIds.block_bakelite, ItemIds.ingot_bakelite);
        decompress(NS, "ingot_bismuth_2", ItemIds.block_bismuth, ItemIds.ingot_bismuth);
        decompress(NS, "ingot_boron_1", ItemIds.block_boron, ItemIds.ingot_boron);
        decompress(NS, "ingot_cadmium_1", ItemIds.block_cadmium, ItemIds.ingot_cadmium);
        decompress(NS, "ingot_dineutronium_2", ItemIds.block_dineutronium, ItemIds.ingot_dineutronium);
        decompress(NS, "ingot_graphite_1", ItemIds.block_graphite, ItemIds.ingot_graphite);
        decompress(NS, "ingot_lanthanium_2", ItemIds.block_lanthanium, ItemIds.ingot_lanthanium);
        decompress(NS, "ingot_niobium_2", ItemIds.block_niobium, ItemIds.ingot_niobium);
        decompress(NS, "ingot_polymer_1", ItemIds.block_polymer, ItemIds.ingot_polymer);
        decompress(NS, "ingot_pu_mix_1", ItemIds.block_pu_mix, ItemIds.ingot_pu_mix);
        decompress(NS, "ingot_ra226_1", ItemIds.block_ra226, ItemIds.ingot_ra226);
        decompress(NS, "ingot_raw_slag", ItemIds.block_slag, ItemIds.ingot_raw_slag);
        decompress(NS, "ingot_rubber", ItemIds.block_rubber, ItemIds.ingot_rubber);
        decompress(NS, "ingot_saturnite_1", ItemIds.block_saturnite, ItemIds.ingot_saturnite);
        decompress(NS, "ingot_schrabidate_1", ItemIds.block_schrabidate, ItemIds.ingot_schrabidate);
        decompress(NS, "ingot_schraranium", ItemIds.block_schraranium, ItemIds.ingot_schraranium);
        decompress(NS, "ingot_tantalium_2", ItemIds.block_tantalium, ItemIds.ingot_tantalium);
        decompress(NS, "ingot_zirconium_2", ItemIds.block_zirconium, ItemIds.ingot_zirconium);
        decompress(NS, "nuclear_waste_vitrified_tiny", ItemIds.nuclear_waste_vitrified, ItemIds.nuclear_waste_vitrified_tiny);
        decompress(NS, "nugget_actinium", ItemIds.ingot_actinium, ItemIds.nugget_actinium);
        decompress(NS, "nugget_am241", ItemIds.ingot_am241, ItemIds.nugget_am241);
        decompress(NS, "nugget_am242", ItemIds.ingot_am242, ItemIds.nugget_am242);
        decompress(NS, "nugget_am_mix", ItemIds.ingot_am_mix, ItemIds.nugget_am_mix);
        decompress(NS, "nugget_americium_fuel", ItemIds.ingot_americium_fuel, ItemIds.nugget_americium_fuel);
        decompress(NS, "nugget_arsenic", ItemIds.ingot_arsenic, ItemIds.nugget_arsenic);
        decompress(NS, "nugget_au198", ItemIds.ingot_au198, ItemIds.nugget_au198);
        decompress(NS, "nugget_bismuth", ItemIds.ingot_bismuth, ItemIds.nugget_bismuth);
        decompress(NS, "nugget_co60", ItemIds.ingot_co60, ItemIds.nugget_co60);
        decompress(NS, "nugget_dineutronium", ItemIds.ingot_dineutronium, ItemIds.nugget_dineutronium);
        decompress(NS, "nugget_gh336", ItemIds.ingot_gh336, ItemIds.nugget_gh336);
        decompress(NS, "nugget_mercury_tiny", ItemIds.nugget_mercury, ItemIds.nugget_mercury_tiny);
        decompress(NS, "nugget_neptunium_fuel", ItemIds.ingot_neptunium_fuel, ItemIds.nugget_neptunium_fuel);
        decompress(NS, "nugget_niobium", ItemIds.ingot_niobium, ItemIds.nugget_niobium);
        decompress(NS, "nugget_osmiridium", ItemIds.ingot_osmiridium, ItemIds.nugget_osmiridium);
        decompress(NS, "nugget_pb209", ItemIds.ingot_pb209, ItemIds.nugget_pb209);
        decompress(NS, "nugget_pu241", ItemIds.ingot_pu241, ItemIds.nugget_pu241);
        decompress(NS, "nugget_pu_mix", ItemIds.ingot_pu_mix, ItemIds.nugget_pu_mix);
        decompress(NS, "nugget_ra226", ItemIds.ingot_ra226, ItemIds.nugget_ra226);
        decompress(NS, "nugget_silicon", ItemIds.ingot_silicon, ItemIds.nugget_silicon);
        decompress(NS, "nugget_sr90", ItemIds.ingot_sr90, ItemIds.nugget_sr90);
        decompress(NS, "nugget_tantalium", ItemIds.ingot_tantalium, ItemIds.nugget_tantalium);
        decompress(NS, "nugget_technetium", ItemIds.ingot_technetium, ItemIds.nugget_technetium);
        decompress(NS, "nugget_zirconium", ItemIds.ingot_zirconium, ItemIds.nugget_zirconium);
        decompress(NS, "powder_boron_tiny", ItemIds.powder_boron, ItemIds.powder_boron_tiny);
        decompress(NS, "powder_coal_tiny", ItemIds.powder_coal, ItemIds.powder_coal_tiny);
        decompress(NS, "powder_cs137_tiny", ItemIds.powder_cs137, ItemIds.powder_cs137_tiny);
        decompress(NS, "powder_i131_tiny", ItemIds.powder_i131, ItemIds.powder_i131_tiny);
        decompress(NS, "powder_paleogenite_tiny", ItemIds.powder_paleogenite, ItemIds.powder_paleogenite_tiny);
        decompress(NS, "powder_sr90_tiny", ItemIds.powder_sr90, ItemIds.powder_sr90_tiny);
        decompress(NS, "powder_xe135_tiny", ItemIds.powder_xe135, ItemIds.powder_xe135_tiny);
    }

    /** 1.7 {@code OreDictManager}: every NUGGET autogen material with an ingot gets a 1-to-9 pair with its raw nugget. */
    private static void rawNuggets() {
        String[][] pairs = {
                {"titanium", ItemIds.ingot_titanium}, {"copper", VanillaIds.copper_ingot},
                {"tungsten", ItemIds.ingot_tungsten}, {"aluminium", ItemIds.ingot_aluminium},
                {"boron", ItemIds.ingot_boron}, {"sodium", "hbm:ingot_raw_sodium"},
                {"strontium", "hbm:ingot_raw_strontium"}, {"lithium", ItemIds.lithium}};
        for (String[] pair : pairs) {
            String nugget = "hbm:nugget_raw_" + pair[0];
            decompress(NS, "nugget_raw_" + pair[0], pair[1], nugget);
            compress(NS, "nugget_raw_" + pair[0] + "_to_ingot", nugget, pair[1]);
        }
    }

    private static void smelting0() {
        smelt(NS, "arc_electrode_burnt_desh", ItemIds.arc_electrode_burnt_desh, ItemIds.ingot_desh, 3.0);
        smelt(NS, "arc_electrode_burnt_graphite", ItemIds.arc_electrode_burnt, ItemIds.ingot_graphite, 3.0);
        smelt(NS, "arc_electrode_burnt_lanthanium", ItemIds.arc_electrode_burnt_lanthanium, ItemIds.ingot_lanthanium, 3.0);
        smelt(NS, "arc_electrode_burnt_saturnite", ItemIds.arc_electrode_burnt_saturnite, ItemIds.ingot_saturnite, 3.0);
        smelt(NS, "basalt_smooth", ItemIds.basalt, ItemIds.basalt_smooth, 0.1);
        smelt(NS, "chunk_ore_cryolite", ItemIds.ore_aluminium, ItemIds.chunk_ore_cryolite, 2.5);
        smelt(NS, "cinnabar", ItemIds.crystal_cinnabar, ItemIds.cinnabar, 2.0, 4);
        smelt(NS, "cobblestone", VanillaIds.gravel, VanillaIds.cobblestone, 0.0);
        smelt(NS, "diamond", ItemIds.gravel_diamond, VanillaIds.diamond, 3.0);
        smelt(NS, "diamond_1", ItemIds.crystal_diamond, VanillaIds.diamond, 2.0, 2);
        smelt(NS, "fluorite", ItemIds.crystal_fluorite, ItemIds.fluorite, 2.0, 6);
        smelt(NS, "glass_ash", ItemIds.ash_digamma, ItemIds.glass_ash, 10.0);
        smelt(NS, "glass_boron", ItemIds.sand_boron, ItemIds.glass_boron, 0.25);
        smelt(NS, "glass_lead", ItemIds.sand_lead, ItemIds.glass_lead, 0.25);
        smelt(NS, "glass_trinitite", ItemIds.waste_trinitite, ItemIds.glass_trinitite, 0.25);
        smelt(NS, "glass_trinitite_1", ItemIds.waste_trinitite_red, ItemIds.glass_trinitite, 0.25);
        smelt(NS, "glyphid_meat_grilled", ItemIds.glyphid_meat, ItemIds.glyphid_meat_grilled, 1.0);
        smelt(NS, "gold_ingot_1", ItemIds.ore_gneiss_gold, VanillaIds.gold_ingot, 5.0);
        smelt(NS, "gold_ingot_2", ItemIds.powder_gold, VanillaIds.gold_ingot, 1.0);
        smelt(NS, "gold_ingot_3", ItemIds.crystal_gold, VanillaIds.gold_ingot, 2.0, 2);
        smelt(NS, "ingot_actinium", ItemIds.powder_actinium, ItemIds.ingot_actinium, 1.0);
        smelt(NS, "ingot_aluminium_1", ItemIds.powder_aluminium, ItemIds.ingot_aluminium, 1.0);
        smelt(NS, "ingot_aluminium_2", ItemIds.crystal_aluminium, ItemIds.ingot_aluminium, 2.0, 2);
        smelt(NS, "ingot_asbestos", ItemIds.powder_asbestos, ItemIds.ingot_asbestos, 1.0);
        smelt(NS, "ingot_au198", ItemIds.powder_au198, ItemIds.ingot_au198, 1.0);
        smelt(NS, "ingot_australium", ItemIds.powder_australium, ItemIds.ingot_australium, 5.0);
        smelt(NS, "ingot_bakelite", ItemIds.powder_bakelite, ItemIds.ingot_bakelite, 1.0);
        smelt(NS, "ingot_beryllium", ItemIds.ore_beryllium, ItemIds.ingot_beryllium, 2.0);
        smelt(NS, "ingot_beryllium_1", ItemIds.powder_beryllium, ItemIds.ingot_beryllium, 1.0);
        smelt(NS, "ingot_beryllium_2", ItemIds.crystal_beryllium, ItemIds.ingot_beryllium, 2.0, 2);
        smelt(NS, "ingot_biorubber", ItemIds.ball_resin, ItemIds.ingot_biorubber, 0.1);
        smelt(NS, "ingot_bismuth", ItemIds.powder_bismuth, ItemIds.ingot_bismuth, 1.0);
        smelt(NS, "ingot_boron", ItemIds.powder_boron, ItemIds.ingot_boron, 1.0);
        smelt(NS, "ingot_cadmium", ItemIds.powder_cadmium, ItemIds.ingot_cadmium, 1.0);
        smelt(NS, "ingot_calcium", ItemIds.powder_calcium, ItemIds.ingot_calcium, 1.0);
        smelt(NS, "ingot_cobalt", ItemIds.ore_cobalt, ItemIds.ingot_cobalt, 2.0);
        smelt(NS, "ingot_cobalt_1", ItemIds.ore_nether_cobalt, ItemIds.ingot_cobalt, 2.0);
        smelt(NS, "ingot_cobalt_2", ItemIds.powder_cobalt, ItemIds.ingot_cobalt, 1.0);
        smelt(NS, "ingot_cobalt_3", ItemIds.crystal_cobalt, ItemIds.ingot_cobalt, 2.0, 2);
        smelt(NS, "ingot_combine_steel", ItemIds.powder_combine_steel, ItemIds.ingot_combine_steel, 1.0);
        smelt(NS, "ingot_combine_steel_1", ItemIds.combine_scrap, ItemIds.ingot_combine_steel, 1.0);
        smelt(NS, "ingot_copper_2", ItemIds.ore_gneiss_copper, VanillaIds.copper_ingot, 5.0);
        smelt(NS, "ingot_copper_3", ItemIds.powder_copper, VanillaIds.copper_ingot, 1.0);
        smelt(NS, "ingot_copper_4", ItemIds.crystal_copper, VanillaIds.copper_ingot, 2.0, 2);
        smelt(NS, "ingot_desh", ItemIds.powder_desh, ItemIds.ingot_desh, 1.0);
        smelt(NS, "ingot_dineutronium", ItemIds.powder_dineutronium, ItemIds.ingot_dineutronium, 5.0);
        smelt(NS, "ingot_dura_steel", ItemIds.powder_dura_steel, ItemIds.ingot_dura_steel, 1.0);
        smelt(NS, "ingot_euphemium", ItemIds.powder_euphemium, ItemIds.ingot_euphemium, 10.0);
        smelt(NS, "ingot_firebrick_1", ItemIds.ball_fireclay, ItemIds.ingot_firebrick, 0.1);
        smelt(NS, "ingot_lanthanium", ItemIds.powder_lanthanium, ItemIds.ingot_lanthanium, 1.0);
        smelt(NS, "ingot_lead", ItemIds.ore_lead, ItemIds.ingot_lead, 3.0);
        smelt(NS, "ingot_lead_1", ItemIds.powder_lead, ItemIds.ingot_lead, 1.0);
        smelt(NS, "ingot_lead_2", ItemIds.crystal_lead, ItemIds.ingot_lead, 2.0, 2);
        smelt(NS, "ingot_magnetized_tungsten_1", ItemIds.powder_magnetized_tungsten, ItemIds.ingot_magnetized_tungsten, 1.0);
        smelt(NS, "ingot_neptunium", ItemIds.powder_neptunium, ItemIds.ingot_neptunium, 1.0);
        smelt(NS, "ingot_niobium", ItemIds.powder_niobium, ItemIds.ingot_niobium, 1.0);
        smelt(NS, "ingot_osmiridium", ItemIds.crystal_osmiridium, ItemIds.ingot_osmiridium, 2.0);
        smelt(NS, "ingot_plutonium", ItemIds.ore_nether_plutonium, ItemIds.ingot_plutonium, 24.0);
        smelt(NS, "ingot_plutonium_1", ItemIds.powder_plutonium, ItemIds.ingot_plutonium, 1.0);
        smelt(NS, "ingot_plutonium_2", ItemIds.crystal_plutonium, ItemIds.ingot_plutonium, 2.0, 2);
        smelt(NS, "ingot_plutonium_3", ItemIds.crystal_trixite, ItemIds.ingot_plutonium, 2.0, 4);
        smelt(NS, "ingot_polonium", ItemIds.powder_polonium, ItemIds.ingot_polonium, 1.0);
        smelt(NS, "ingot_polymer", ItemIds.powder_polymer, ItemIds.ingot_polymer, 1.0);
        smelt(NS, "ingot_ra226", ItemIds.powder_ra226, ItemIds.ingot_ra226, 1.0);
        smelt(NS, "ingot_red_copper_1", ItemIds.powder_red_copper, ItemIds.ingot_red_copper, 1.0);
        smelt(NS, "ingot_schrabidate", ItemIds.powder_schrabidate, ItemIds.ingot_schrabidate, 5.0);
        smelt(NS, "ingot_schrabidium_1", ItemIds.ore_schrabidium, ItemIds.ingot_schrabidium, 128.0);
        smelt(NS, "ingot_schrabidium_2", ItemIds.ore_nether_schrabidium, ItemIds.ingot_schrabidium, 256.0);
        smelt(NS, "ingot_schrabidium_3", ItemIds.ore_gneiss_schrabidium, ItemIds.ingot_schrabidium, 256.0);
        smelt(NS, "ingot_schrabidium_4", ItemIds.powder_schrabidium, ItemIds.ingot_schrabidium, 5.0);
        smelt(NS, "ingot_schrabidium_5", ItemIds.crystal_schrabidium, ItemIds.ingot_schrabidium, 2.0, 2);
        smelt(NS, "ingot_sr90", ItemIds.powder_sr90, ItemIds.ingot_sr90, 1.0);
        smelt(NS, "ingot_starmetal", ItemIds.crystal_starmetal, ItemIds.ingot_starmetal, 2.0, 2);
        smelt(NS, "ingot_steel", ItemIds.powder_steel, ItemIds.ingot_steel, 1.0);
        smelt(NS, "ingot_tantalium", ItemIds.powder_tantalium, ItemIds.ingot_tantalium, 1.0);
        smelt(NS, "ingot_tcalloy", ItemIds.powder_tcalloy, ItemIds.ingot_tcalloy, 1.0);
        smelt(NS, "ingot_th232", ItemIds.ore_thorium, ItemIds.ingot_th232, 3.0);
        smelt(NS, "ingot_th232_1", ItemIds.powder_thorium, ItemIds.ingot_th232, 1.0);
        smelt(NS, "ingot_th232_2", ItemIds.crystal_thorium, ItemIds.ingot_th232, 2.0, 2);
        smelt(NS, "ingot_titanium", ItemIds.ore_titanium, ItemIds.ingot_titanium, 3.0);
        smelt(NS, "ingot_titanium_1", ItemIds.powder_titanium, ItemIds.ingot_titanium, 1.0);
        smelt(NS, "ingot_titanium_2", ItemIds.crystal_titanium, ItemIds.ingot_titanium, 2.0, 2);
        smelt(NS, "ingot_tungsten_1", ItemIds.ore_tungsten, ItemIds.ingot_tungsten, 6.0);
        smelt(NS, "ingot_tungsten_2", ItemIds.ore_nether_tungsten, ItemIds.ingot_tungsten, 12.0);
        smelt(NS, "ingot_tungsten_3", ItemIds.powder_tungsten, ItemIds.ingot_tungsten, 1.0);
        smelt(NS, "ingot_tungsten_4", ItemIds.crystal_tungsten, ItemIds.ingot_tungsten, 2.0, 2);
        smelt(NS, "ingot_uranium", ItemIds.ore_uranium, ItemIds.ingot_uranium, 6.0);
        smelt(NS, "ingot_uranium_1", ItemIds.ore_uranium_scorched, ItemIds.ingot_uranium, 6.0);
        smelt(NS, "ingot_uranium_2", ItemIds.ore_nether_uranium, ItemIds.ingot_uranium, 12.0);
        smelt(NS, "ingot_uranium_3", ItemIds.ore_nether_uranium_scorched, ItemIds.ingot_uranium, 12.0);
        smelt(NS, "ingot_uranium_4", ItemIds.ore_gneiss_uranium, ItemIds.ingot_uranium, 12.0);
        // port: the deepslate twins smelt like their stone ores
        smelt(NS, "chunk_ore_cryolite_deepslate", ItemIds.ore_aluminium_deepslate, ItemIds.chunk_ore_cryolite, 2.5);
        smelt(NS, "ingot_beryllium_deepslate", ItemIds.ore_beryllium_deepslate, ItemIds.ingot_beryllium, 2.0);
        smelt(NS, "ingot_cobalt_deepslate", ItemIds.ore_cobalt_deepslate, ItemIds.ingot_cobalt, 2.0);
        smelt(NS, "ingot_lead_deepslate", ItemIds.ore_lead_deepslate, ItemIds.ingot_lead, 3.0);
        smelt(NS, "ingot_th232_deepslate", ItemIds.ore_thorium_deepslate, ItemIds.ingot_th232, 3.0);
        smelt(NS, "ingot_titanium_deepslate", ItemIds.ore_titanium_deepslate, ItemIds.ingot_titanium, 3.0);
        smelt(NS, "ingot_tungsten_deepslate", ItemIds.ore_tungsten_deepslate, ItemIds.ingot_tungsten, 6.0);
        smelt(NS, "ingot_uranium_deepslate", ItemIds.ore_uranium_deepslate, ItemIds.ingot_uranium, 6.0);
    }

    private static void smelting1() {
        smelt(NS, "ingot_uranium_5", ItemIds.ore_gneiss_uranium_scorched, ItemIds.ingot_uranium, 12.0);
        smelt(NS, "ingot_uranium_6", ItemIds.powder_uranium, ItemIds.ingot_uranium, 1.0);
        smelt(NS, "ingot_uranium_7", ItemIds.crystal_uranium, ItemIds.ingot_uranium, 2.0, 2);
        smelt(NS, "ingot_zirconium", ItemIds.powder_zirconium, ItemIds.ingot_zirconium, 1.0);
        smelt(NS, "iron_ingot", ItemIds.ore_gneiss_iron, VanillaIds.iron_ingot, 5.0);
        smelt(NS, "iron_ingot_1", ItemIds.powder_iron, VanillaIds.iron_ingot, 1.0);
        smelt(NS, "iron_ingot_2", ItemIds.crystal_iron, VanillaIds.iron_ingot, 2.0, 2);
        // no "lignite" smelting: 1.7 SmeltingRecipes has none (lignite and ore_lignite do not smelt); the port had lignite -> lignite
        smelt(NS, "lithium", ItemIds.ore_gneiss_lithium, ItemIds.lithium, 10.0);
        smelt(NS, "lithium_1", ItemIds.powder_lithium, ItemIds.lithium, 1.0);
        smelt(NS, "lithium_2", ItemIds.crystal_lithium, ItemIds.lithium, 2.0, 2);
        smelt(NS, "niter", ItemIds.crystal_niter, ItemIds.niter, 2.0, 6);
        smelt(NS, "nugget_australium", ItemIds.ore_australium, ItemIds.nugget_australium, 2.5);
        smelt(NS, "nugget_schrabidium", ItemIds.ingot_schraranium, ItemIds.nugget_schrabidium, 2.0);
        smelt(NS, "nugget_schrabidium_1", ItemIds.crystal_schraranium, ItemIds.nugget_schrabidium, 2.0, 2);
        smelt(NS, "obsidian", ItemIds.gravel_obsidian, VanillaIds.obsidian, 0.0);
        smelt(NS, "powder_desh_mix", ItemIds.crystal_rare, ItemIds.powder_desh_mix, 2.0);
        smelt(NS, "powder_fire", ItemIds.crystal_phosphorus, ItemIds.powder_fire, 2.0, 6);
        smelt(NS, "redstone", ItemIds.crystal_redstone, VanillaIds.redstone, 2.0, 6);
        smelt(NS, "sulfur", ItemIds.crystal_sulfur, ItemIds.sulfur, 2.0, 6);
    }
}
