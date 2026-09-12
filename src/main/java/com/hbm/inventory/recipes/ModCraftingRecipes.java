package com.hbm.inventory.recipes;

import static dev.golem.core.dejson.Crafting.compress;
import static dev.golem.core.dejson.Crafting.decompress;
import static dev.golem.core.dejson.Crafting.smelt;

/**
 * The hbm recipes whose shape carries no decision, declared through Golem Core's {@code dejson}
 * vocabulary instead of written as JSON.
 *
 * <p>{@value #FILES_REPLACED} recipe files have been deleted for these: 183 that pack nine of
 * something into a block, 54 that unpack it again, and 121 furnace recipes. Every one was a fixed
 * arrangement around two ids and a number — a full three-by-three grid of one ingredient, a single
 * shapeless ingredient making nine, two hundred ticks and an experience value — so the file was
 * seven lines of punctuation around the two facts that mattered.
 *
 * <p>The other {@value #FILES_KEPT} recipes are still files, and should stay that way. A pattern
 * somebody chose and ingredients somebody balanced <em>are</em> the recipe; there is no boilerplate
 * in them to remove, and moving them into Java would only make them harder to read.
 *
 * <p>Recipe ids are carried through unchanged rather than derived from the output. They are visible
 * — the recipe book remembers them, JEI bookmarks them — so renaming {@code ingot_actinium_1} to
 * something tidier would reset progress for no gain.
 */
public final class ModCraftingRecipes {

    private static final String NS = "hbm";

    /** How many recipe files the declarations below stand in for; quoted in this class's own javadoc. */
    static final int FILES_REPLACED = 358;

    /** How many recipes remain hand-written, for the reason given above. */
    static final int FILES_KEPT = 1132;

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
        smelting0();
        smelting1();
    }

    private static void packing0() {
        compress(NS, "bedrock_ore_fragment_aluminium_compression", "#c:fragments/aluminium", "hbm:powder_aluminium");
        compress(NS, "bedrock_ore_fragment_asbestos_compression", "#c:fragments/asbestos", "hbm:powder_asbestos");
        compress(NS, "bedrock_ore_fragment_bauxite_compression", "#c:fragments/bauxite", "hbm:stone_resource_bauxite");
        compress(NS, "bedrock_ore_fragment_beryllium_compression", "#c:fragments/beryllium", "hbm:powder_beryllium");
        compress(NS, "bedrock_ore_fragment_bismuth_compression", "#c:fragments/bismuth", "hbm:powder_bismuth");
        compress(NS, "bedrock_ore_fragment_borax_compression", "#c:fragments/borax", "hbm:powder_borax");
        compress(NS, "bedrock_ore_fragment_boron_compression", "#c:fragments/boron", "hbm:powder_boron");
        compress(NS, "bedrock_ore_fragment_chlorocalcite_compression", "#c:fragments/chlorocalcite", "hbm:powder_chlorocalcite");
        compress(NS, "bedrock_ore_fragment_cinnabar_compression", "#c:fragments/cinnabar", "hbm:cinnabar");
        compress(NS, "bedrock_ore_fragment_coal_compression", "#c:fragments/coal", "hbm:powder_coal");
        compress(NS, "bedrock_ore_fragment_cobalt_compression", "#c:fragments/cobalt", "hbm:powder_cobalt");
        compress(NS, "bedrock_ore_fragment_copper_compression", "#c:fragments/copper", "hbm:powder_copper");
        compress(NS, "bedrock_ore_fragment_cryolite_compression", "#c:fragments/cryolite", "hbm:chunk_ore_cryolite");
        compress(NS, "bedrock_ore_fragment_diamond_compression", "#c:fragments/diamond", "hbm:powder_diamond");
        compress(NS, "bedrock_ore_fragment_emerald_compression", "#c:fragments/emerald", "hbm:powder_emerald");
        compress(NS, "bedrock_ore_fragment_fluorite_compression", "#c:fragments/fluorite", "hbm:fluorite");
        compress(NS, "bedrock_ore_fragment_gold_compression", "#c:fragments/gold", "hbm:powder_gold");
        compress(NS, "bedrock_ore_fragment_iron_compression", "#c:fragments/iron", "hbm:powder_iron");
        compress(NS, "bedrock_ore_fragment_lanthanium_compression", "#c:fragments/lanthanium", "hbm:powder_lanthanium");
        compress(NS, "bedrock_ore_fragment_lead_compression", "#c:fragments/lead", "hbm:powder_lead");
        compress(NS, "bedrock_ore_fragment_lignite_compression", "#c:fragments/lignite", "hbm:powder_lignite");
        compress(NS, "bedrock_ore_fragment_lithium_compression", "#c:fragments/lithium", "hbm:powder_lithium");
        compress(NS, "bedrock_ore_fragment_molysite_compression", "#c:fragments/molysite", "hbm:powder_molysite");
        compress(NS, "bedrock_ore_fragment_neodymium_compression", "#c:fragments/neodymium", "hbm:powder_neodymium");
        compress(NS, "bedrock_ore_fragment_niobium_compression", "#c:fragments/niobium", "hbm:powder_niobium");
        compress(NS, "bedrock_ore_fragment_niter_compression", "#c:fragments/niter", "hbm:niter");
        compress(NS, "bedrock_ore_fragment_polonium_compression", "#c:fragments/polonium", "hbm:powder_polonium");
        compress(NS, "bedrock_ore_fragment_ra226_compression", "#c:fragments/ra226", "hbm:powder_ra226");
        compress(NS, "bedrock_ore_fragment_rare_compression", "#c:fragments/rare", "hbm:chunk_ore_rare");
        compress(NS, "bedrock_ore_fragment_red_phosphorus_compression", "#c:fragments/red_phosphorus", "hbm:powder_fire");
        compress(NS, "bedrock_ore_fragment_redstone_compression", "#c:fragments/redstone", "minecraft:redstone");
        compress(NS, "bedrock_ore_fragment_silicon_compression", "#c:fragments/silicon", "hbm:ingot_silicon");
        compress(NS, "bedrock_ore_fragment_sodalite_compression", "#c:fragments/sodalite", "hbm:gem_sodalite");
        compress(NS, "bedrock_ore_fragment_sodium_compression", "#c:fragments/sodium", "hbm:powder_sodium");
        compress(NS, "bedrock_ore_fragment_strontium_compression", "#c:fragments/strontium", "hbm:powder_strontium");
        compress(NS, "bedrock_ore_fragment_sulfur_compression", "#c:fragments/sulfur", "hbm:sulfur");
        compress(NS, "bedrock_ore_fragment_tantalium_compression", "#c:fragments/tantalium", "hbm:powder_tantalium");
        compress(NS, "bedrock_ore_fragment_technetium_compression", "#c:fragments/technetium", "hbm:ingot_technetium");
        compress(NS, "bedrock_ore_fragment_thorium_compression", "#c:fragments/thorium", "hbm:powder_thorium");
        compress(NS, "bedrock_ore_fragment_titanium_compression", "#c:fragments/titanium", "hbm:powder_titanium");
        compress(NS, "bedrock_ore_fragment_tungsten_compression", "#c:fragments/tungsten", "hbm:powder_tungsten");
        compress(NS, "bedrock_ore_fragment_u238_compression", "#c:fragments/u238", "hbm:ingot_u238");
        compress(NS, "bedrock_ore_fragment_uranium_compression", "#c:fragments/uranium", "hbm:powder_uranium");
        compress(NS, "bedrock_ore_fragment_zirconium_compression", "#c:fragments/zirconium", "hbm:powder_zirconium");
        compress(NS, "block_actinium", "hbm:ingot_actinium", "hbm:block_actinium");
        compress(NS, "block_aluminium", "hbm:ingot_aluminium", "hbm:block_aluminium");
        compress(NS, "block_asbestos", "hbm:ingot_asbestos", "hbm:block_asbestos");
        compress(NS, "block_bakelite", "hbm:ingot_bakelite", "hbm:block_bakelite");
        compress(NS, "block_beryllium", "hbm:ingot_beryllium", "hbm:block_beryllium");
        compress(NS, "block_bismuth", "hbm:ingot_bismuth", "hbm:block_bismuth");
        compress(NS, "block_boron", "hbm:ingot_boron", "hbm:block_boron");
        compress(NS, "block_cadmium", "hbm:ingot_cadmium", "hbm:block_cadmium");
        compress(NS, "block_cobalt", "hbm:ingot_cobalt", "hbm:block_cobalt");
        compress(NS, "block_coltan", "hbm:fragment_coltan", "hbm:block_coltan");
        compress(NS, "block_combine_steel", "hbm:ingot_combine_steel", "hbm:block_combine_steel");
        compress(NS, "block_copper", "hbm:ingot_copper", "hbm:block_copper");
        compress(NS, "block_desh", "hbm:ingot_desh", "hbm:block_desh");
        compress(NS, "block_dineutronium", "hbm:ingot_dineutronium", "hbm:block_dineutronium");
        compress(NS, "block_dura_steel", "hbm:ingot_dura_steel", "hbm:block_dura_steel");
        compress(NS, "block_euphemium", "hbm:ingot_euphemium", "hbm:block_euphemium");
        compress(NS, "block_fluorite", "hbm:fluorite", "hbm:block_fluorite");
        compress(NS, "block_graphite", "hbm:ingot_graphite", "hbm:block_graphite");
        compress(NS, "block_lanthanium", "hbm:ingot_lanthanium", "hbm:block_lanthanium");
        compress(NS, "block_lead", "hbm:ingot_lead", "hbm:block_lead");
        compress(NS, "block_magnetized_tungsten", "hbm:ingot_magnetized_tungsten", "hbm:block_magnetized_tungsten");
        compress(NS, "block_meteor_broken", "hbm:fragment_meteorite", "hbm:block_meteor_broken");
        compress(NS, "block_mox_fuel", "hbm:ingot_mox_fuel", "hbm:block_mox_fuel");
        compress(NS, "block_neptunium", "hbm:ingot_neptunium", "hbm:block_neptunium");
        compress(NS, "block_niobium", "hbm:ingot_niobium", "hbm:block_niobium");
        compress(NS, "block_niter", "hbm:niter", "hbm:block_niter");
        compress(NS, "block_plutonium", "hbm:ingot_plutonium", "hbm:block_plutonium");
        compress(NS, "block_plutonium_fuel", "hbm:ingot_plutonium_fuel", "hbm:block_plutonium_fuel");
        compress(NS, "block_polonium", "hbm:ingot_polonium", "hbm:block_polonium");
        compress(NS, "block_polymer", "hbm:ingot_polymer", "hbm:block_polymer");
        compress(NS, "block_pu238", "hbm:ingot_pu238", "hbm:block_pu238");
        compress(NS, "block_pu239", "hbm:ingot_pu239", "hbm:block_pu239");
        compress(NS, "block_pu240", "hbm:ingot_pu240", "hbm:block_pu240");
        compress(NS, "block_pu_mix", "hbm:ingot_pu_mix", "hbm:block_pu_mix");
        compress(NS, "block_ra226", "hbm:ingot_ra226", "hbm:block_ra226");
        compress(NS, "block_red_copper", "hbm:ingot_red_copper", "hbm:block_red_copper");
        compress(NS, "block_red_phosphorus", "hbm:powder_fire", "hbm:block_red_phosphorus");
        compress(NS, "block_rubber", "hbm:ingot_rubber", "hbm:block_rubber");
        compress(NS, "block_saturnite", "hbm:ingot_saturnite", "hbm:block_saturnite");
        compress(NS, "block_schrabidate", "hbm:ingot_schrabidate", "hbm:block_schrabidate");
        compress(NS, "block_schrabidium", "hbm:ingot_schrabidium", "hbm:block_schrabidium");
        compress(NS, "block_schrabidium_fuel", "hbm:ingot_schrabidium_fuel", "hbm:block_schrabidium_fuel");
        compress(NS, "block_schraranium", "hbm:ingot_schraranium", "hbm:block_schraranium");
        compress(NS, "block_scrap_1", "hbm:dust", "hbm:block_scrap");
        compress(NS, "block_slag", "hbm:ingot_raw_slag", "hbm:block_slag");
        compress(NS, "block_solinium", "hbm:ingot_solinium", "hbm:block_solinium");
        compress(NS, "block_starmetal", "hbm:ingot_starmetal", "hbm:block_starmetal");
        compress(NS, "block_steel", "hbm:ingot_steel", "hbm:block_steel");
        compress(NS, "block_sulfur", "hbm:sulfur", "hbm:block_sulfur");
        compress(NS, "block_tantalium", "hbm:ingot_tantalium", "hbm:block_tantalium");
        compress(NS, "block_thorium", "hbm:ingot_th232", "hbm:block_thorium");
    }

    private static void packing1() {
        compress(NS, "block_thorium_fuel", "hbm:ingot_thorium_fuel", "hbm:block_thorium_fuel");
        compress(NS, "block_titanium", "hbm:ingot_titanium", "hbm:block_titanium");
        compress(NS, "block_trinitite", "hbm:trinitite", "hbm:block_trinitite");
        compress(NS, "block_tungsten", "hbm:ingot_tungsten", "hbm:block_tungsten");
        compress(NS, "block_u233", "hbm:ingot_u233", "hbm:block_u233");
        compress(NS, "block_u235", "hbm:ingot_u235", "hbm:block_u235");
        compress(NS, "block_u238", "hbm:ingot_u238", "hbm:block_u238");
        compress(NS, "block_uranium", "hbm:ingot_uranium", "hbm:block_uranium");
        compress(NS, "block_uranium_fuel", "hbm:ingot_uranium_fuel", "hbm:block_uranium_fuel");
        compress(NS, "block_white_phosphorus", "hbm:ingot_phosphorus", "hbm:block_white_phosphorus");
        compress(NS, "block_zirconium", "hbm:ingot_zirconium", "hbm:block_zirconium");
        compress(NS, "dust", "hbm:dust_tiny", "hbm:dust");
        compress(NS, "egg_balefire", "hbm:egg_balefire_shard", "hbm:egg_balefire");
        compress(NS, "egg_balefire_shard_2", "hbm:cell_balefire", "hbm:egg_balefire_shard");
        compress(NS, "gold_ingot", "#c:wires/gold", "minecraft:gold_ingot");
        compress(NS, "ingot_actinium_2", "hbm:nugget_actinium", "hbm:ingot_actinium");
        compress(NS, "ingot_aluminium", "#c:wires/aluminium", "hbm:ingot_aluminium");
        compress(NS, "ingot_am241", "hbm:nugget_am241", "hbm:ingot_am241");
        compress(NS, "ingot_am242", "hbm:nugget_am242", "hbm:ingot_am242");
        compress(NS, "ingot_am_mix", "hbm:nugget_am_mix", "hbm:ingot_am_mix");
        compress(NS, "ingot_americium_fuel", "hbm:nugget_americium_fuel", "hbm:ingot_americium_fuel");
        compress(NS, "ingot_arsenic", "hbm:nugget_arsenic", "hbm:ingot_arsenic");
        compress(NS, "ingot_au198_1", "hbm:nugget_au198", "hbm:ingot_au198");
        compress(NS, "ingot_australium_2", "hbm:nugget_australium", "hbm:ingot_australium");
        compress(NS, "ingot_beryllium_5", "hbm:nugget_beryllium", "hbm:ingot_beryllium");
        compress(NS, "ingot_bismuth_1", "hbm:nugget_bismuth", "hbm:ingot_bismuth");
        compress(NS, "ingot_co60", "hbm:nugget_co60", "hbm:ingot_co60");
        compress(NS, "ingot_copper", "#c:wires/copper", "hbm:ingot_copper");
        compress(NS, "ingot_dineutronium_1", "hbm:nugget_dineutronium", "hbm:ingot_dineutronium");
        compress(NS, "ingot_euphemium_2", "hbm:nugget_euphemium", "hbm:ingot_euphemium");
        compress(NS, "ingot_gh336", "hbm:nugget_gh336", "hbm:ingot_gh336");
        compress(NS, "ingot_graphite", "#c:wires/carbon", "hbm:ingot_graphite");
        compress(NS, "ingot_hes_1", "hbm:nugget_hes", "hbm:ingot_hes");
        compress(NS, "ingot_lead_4", "hbm:nugget_lead", "hbm:ingot_lead");
        compress(NS, "ingot_les_1", "hbm:nugget_les", "hbm:ingot_les");
        compress(NS, "ingot_magnetized_tungsten", "#c:wires/magnetized_tungsten", "hbm:ingot_magnetized_tungsten");
        compress(NS, "ingot_mox_fuel_2", "hbm:nugget_mox_fuel", "hbm:ingot_mox_fuel");
        compress(NS, "ingot_neptunium_3", "hbm:nugget_neptunium", "hbm:ingot_neptunium");
        compress(NS, "ingot_neptunium_fuel", "hbm:nugget_neptunium_fuel", "hbm:ingot_neptunium_fuel");
        compress(NS, "ingot_niobium_1", "hbm:nugget_niobium", "hbm:ingot_niobium");
        compress(NS, "ingot_osmiridium_1", "hbm:nugget_osmiridium", "hbm:ingot_osmiridium");
        compress(NS, "ingot_pb209", "hbm:nugget_pb209", "hbm:ingot_pb209");
        compress(NS, "ingot_plutonium_6", "hbm:nugget_plutonium", "hbm:ingot_plutonium");
        compress(NS, "ingot_plutonium_fuel_2", "hbm:nugget_plutonium_fuel", "hbm:ingot_plutonium_fuel");
        compress(NS, "ingot_polonium_3", "hbm:nugget_polonium", "hbm:ingot_polonium");
        compress(NS, "ingot_pu238_2", "hbm:nugget_pu238", "hbm:ingot_pu238");
        compress(NS, "ingot_pu239_2", "hbm:nugget_pu239", "hbm:ingot_pu239");
        compress(NS, "ingot_pu240_2", "hbm:nugget_pu240", "hbm:ingot_pu240");
        compress(NS, "ingot_pu241", "hbm:nugget_pu241", "hbm:ingot_pu241");
        compress(NS, "ingot_pu_mix", "hbm:nugget_pu_mix", "hbm:ingot_pu_mix");
        compress(NS, "ingot_ra226_2", "hbm:nugget_ra226", "hbm:ingot_ra226");
        compress(NS, "ingot_red_copper", "#c:wires/red_copper", "hbm:ingot_red_copper");
        compress(NS, "ingot_schrabidium", "#c:wires/schrabidium", "hbm:ingot_schrabidium");
        compress(NS, "ingot_schrabidium_8", "hbm:nugget_schrabidium", "hbm:ingot_schrabidium");
        compress(NS, "ingot_schrabidium_fuel_2", "hbm:nugget_schrabidium_fuel", "hbm:ingot_schrabidium_fuel");
        compress(NS, "ingot_silicon", "hbm:nugget_silicon", "hbm:ingot_silicon");
        compress(NS, "ingot_solinium_2", "hbm:nugget_solinium", "hbm:ingot_solinium");
        compress(NS, "ingot_sr90_1", "hbm:nugget_sr90", "hbm:ingot_sr90");
        compress(NS, "ingot_tantalium_1", "hbm:nugget_tantalium", "hbm:ingot_tantalium");
        compress(NS, "ingot_technetium", "hbm:nugget_technetium", "hbm:ingot_technetium");
        compress(NS, "ingot_th232_5", "hbm:nugget_th232", "hbm:ingot_th232");
        compress(NS, "ingot_thorium_fuel_2", "hbm:nugget_thorium_fuel", "hbm:ingot_thorium_fuel");
        compress(NS, "ingot_tungsten", "#c:wires/tungsten", "hbm:ingot_tungsten");
        compress(NS, "ingot_u233_2", "hbm:nugget_u233", "hbm:ingot_u233");
        compress(NS, "ingot_u235_2", "hbm:nugget_u235", "hbm:ingot_u235");
        compress(NS, "ingot_u238_2", "hbm:nugget_u238", "hbm:ingot_u238");
        compress(NS, "ingot_uranium_10", "hbm:nugget_uranium", "hbm:ingot_uranium");
        compress(NS, "ingot_uranium_fuel_2", "hbm:nugget_uranium_fuel", "hbm:ingot_uranium_fuel");
        compress(NS, "ingot_zirconium_1", "hbm:nugget_zirconium", "hbm:ingot_zirconium");
        compress(NS, "nuclear_waste_1", "hbm:nuclear_waste_tiny", "hbm:nuclear_waste");
        compress(NS, "nuclear_waste_vitrified", "hbm:nuclear_waste_vitrified_tiny", "hbm:nuclear_waste_vitrified");
        compress(NS, "nugget_mercury", "hbm:nugget_mercury_tiny", "hbm:nugget_mercury");
        compress(NS, "powder_actinium", "hbm:powder_actinium_tiny", "hbm:powder_actinium");
        compress(NS, "powder_boron", "hbm:powder_boron_tiny", "hbm:powder_boron");
        compress(NS, "powder_cerium", "hbm:powder_cerium_tiny", "hbm:powder_cerium");
        compress(NS, "powder_coal", "hbm:powder_coal_tiny", "hbm:powder_coal");
        compress(NS, "powder_cobalt", "hbm:powder_cobalt_tiny", "hbm:powder_cobalt");
        compress(NS, "powder_cs137", "hbm:powder_cs137_tiny", "hbm:powder_cs137");
        compress(NS, "powder_i131", "hbm:powder_i131_tiny", "hbm:powder_i131");
        compress(NS, "powder_lanthanium", "hbm:powder_lanthanium_tiny", "hbm:powder_lanthanium");
        compress(NS, "powder_lithium", "hbm:powder_lithium_tiny", "hbm:powder_lithium");
        compress(NS, "powder_meteorite", "hbm:powder_meteorite_tiny", "hbm:powder_meteorite");
        compress(NS, "powder_neodymium", "hbm:powder_neodymium_tiny", "hbm:powder_neodymium");
        compress(NS, "powder_niobium", "hbm:powder_niobium_tiny", "hbm:powder_niobium");
        compress(NS, "powder_paleogenite", "hbm:powder_paleogenite_tiny", "hbm:powder_paleogenite");
        compress(NS, "powder_sr90", "hbm:powder_sr90_tiny", "hbm:powder_sr90");
        compress(NS, "powder_steel", "hbm:powder_steel_tiny", "hbm:powder_steel");
        compress(NS, "powder_xe135", "hbm:powder_xe135_tiny", "hbm:powder_xe135");
    }

    private static void unpacking() {
        decompress(NS, "dust_tiny", "hbm:dust", "hbm:dust_tiny");
        decompress(NS, "fragment_coltan", "hbm:block_coltan", "hbm:fragment_coltan");
        decompress(NS, "ingot_actinium_1", "hbm:block_actinium", "hbm:ingot_actinium");
        decompress(NS, "ingot_aluminium_3", "hbm:block_aluminium", "hbm:ingot_aluminium");
        decompress(NS, "ingot_bakelite_1", "hbm:block_bakelite", "hbm:ingot_bakelite");
        decompress(NS, "ingot_bismuth_2", "hbm:block_bismuth", "hbm:ingot_bismuth");
        decompress(NS, "ingot_boron_1", "hbm:block_boron", "hbm:ingot_boron");
        decompress(NS, "ingot_cadmium_1", "hbm:block_cadmium", "hbm:ingot_cadmium");
        decompress(NS, "ingot_dineutronium_2", "hbm:block_dineutronium", "hbm:ingot_dineutronium");
        decompress(NS, "ingot_graphite_1", "hbm:block_graphite", "hbm:ingot_graphite");
        decompress(NS, "ingot_lanthanium_2", "hbm:block_lanthanium", "hbm:ingot_lanthanium");
        decompress(NS, "ingot_niobium_2", "hbm:block_niobium", "hbm:ingot_niobium");
        decompress(NS, "ingot_polymer_1", "hbm:block_polymer", "hbm:ingot_polymer");
        decompress(NS, "ingot_pu_mix_1", "hbm:block_pu_mix", "hbm:ingot_pu_mix");
        decompress(NS, "ingot_ra226_1", "hbm:block_ra226", "hbm:ingot_ra226");
        decompress(NS, "ingot_raw_slag", "hbm:block_slag", "hbm:ingot_raw_slag");
        decompress(NS, "ingot_rubber", "hbm:block_rubber", "hbm:ingot_rubber");
        decompress(NS, "ingot_saturnite_1", "hbm:block_saturnite", "hbm:ingot_saturnite");
        decompress(NS, "ingot_schrabidate_1", "hbm:block_schrabidate", "hbm:ingot_schrabidate");
        decompress(NS, "ingot_schraranium", "hbm:block_schraranium", "hbm:ingot_schraranium");
        decompress(NS, "ingot_tantalium_2", "hbm:block_tantalium", "hbm:ingot_tantalium");
        decompress(NS, "ingot_zirconium_2", "hbm:block_zirconium", "hbm:ingot_zirconium");
        decompress(NS, "nuclear_waste_vitrified_tiny", "hbm:nuclear_waste_vitrified", "hbm:nuclear_waste_vitrified_tiny");
        decompress(NS, "nugget_actinium", "hbm:ingot_actinium", "hbm:nugget_actinium");
        decompress(NS, "nugget_am241", "hbm:ingot_am241", "hbm:nugget_am241");
        decompress(NS, "nugget_am242", "hbm:ingot_am242", "hbm:nugget_am242");
        decompress(NS, "nugget_am_mix", "hbm:ingot_am_mix", "hbm:nugget_am_mix");
        decompress(NS, "nugget_americium_fuel", "hbm:ingot_americium_fuel", "hbm:nugget_americium_fuel");
        decompress(NS, "nugget_arsenic", "hbm:ingot_arsenic", "hbm:nugget_arsenic");
        decompress(NS, "nugget_au198", "hbm:ingot_au198", "hbm:nugget_au198");
        decompress(NS, "nugget_bismuth", "hbm:ingot_bismuth", "hbm:nugget_bismuth");
        decompress(NS, "nugget_co60", "hbm:ingot_co60", "hbm:nugget_co60");
        decompress(NS, "nugget_dineutronium", "hbm:ingot_dineutronium", "hbm:nugget_dineutronium");
        decompress(NS, "nugget_gh336", "hbm:ingot_gh336", "hbm:nugget_gh336");
        decompress(NS, "nugget_mercury_tiny", "hbm:nugget_mercury", "hbm:nugget_mercury_tiny");
        decompress(NS, "nugget_neptunium_fuel", "hbm:ingot_neptunium_fuel", "hbm:nugget_neptunium_fuel");
        decompress(NS, "nugget_niobium", "hbm:ingot_niobium", "hbm:nugget_niobium");
        decompress(NS, "nugget_osmiridium", "hbm:ingot_osmiridium", "hbm:nugget_osmiridium");
        decompress(NS, "nugget_pb209", "hbm:ingot_pb209", "hbm:nugget_pb209");
        decompress(NS, "nugget_pu241", "hbm:ingot_pu241", "hbm:nugget_pu241");
        decompress(NS, "nugget_pu_mix", "hbm:ingot_pu_mix", "hbm:nugget_pu_mix");
        decompress(NS, "nugget_ra226", "hbm:ingot_ra226", "hbm:nugget_ra226");
        decompress(NS, "nugget_silicon", "hbm:ingot_silicon", "hbm:nugget_silicon");
        decompress(NS, "nugget_sr90", "hbm:ingot_sr90", "hbm:nugget_sr90");
        decompress(NS, "nugget_tantalium", "hbm:ingot_tantalium", "hbm:nugget_tantalium");
        decompress(NS, "nugget_technetium", "hbm:ingot_technetium", "hbm:nugget_technetium");
        decompress(NS, "nugget_zirconium", "hbm:ingot_zirconium", "hbm:nugget_zirconium");
        decompress(NS, "powder_boron_tiny", "hbm:powder_boron", "hbm:powder_boron_tiny");
        decompress(NS, "powder_coal_tiny", "hbm:powder_coal", "hbm:powder_coal_tiny");
        decompress(NS, "powder_cs137_tiny", "hbm:powder_cs137", "hbm:powder_cs137_tiny");
        decompress(NS, "powder_i131_tiny", "hbm:powder_i131", "hbm:powder_i131_tiny");
        decompress(NS, "powder_paleogenite_tiny", "hbm:powder_paleogenite", "hbm:powder_paleogenite_tiny");
        decompress(NS, "powder_sr90_tiny", "hbm:powder_sr90", "hbm:powder_sr90_tiny");
        decompress(NS, "powder_xe135_tiny", "hbm:powder_xe135", "hbm:powder_xe135_tiny");
    }

    private static void smelting0() {
        smelt(NS, "arc_electrode_burnt_desh", "hbm:arc_electrode_burnt_desh", "hbm:ingot_desh", 3.0);
        smelt(NS, "arc_electrode_burnt_graphite", "hbm:arc_electrode_burnt", "hbm:ingot_graphite", 3.0);
        smelt(NS, "arc_electrode_burnt_lanthanium", "hbm:arc_electrode_burnt_lanthanium", "hbm:ingot_lanthanium", 3.0);
        smelt(NS, "arc_electrode_burnt_saturnite", "hbm:arc_electrode_burnt_saturnite", "hbm:ingot_saturnite", 3.0);
        smelt(NS, "basalt_smooth", "hbm:basalt", "hbm:basalt_smooth", 0.1);
        smelt(NS, "chunk_ore_cryolite", "hbm:ore_aluminium", "hbm:chunk_ore_cryolite", 2.5);
        smelt(NS, "cinnabar", "hbm:crystal_cinnabar", "hbm:cinnabar", 2.0, 4);
        smelt(NS, "cobblestone", "minecraft:gravel", "minecraft:cobblestone", 0.0);
        smelt(NS, "diamond", "hbm:gravel_diamond", "minecraft:diamond", 3.0);
        smelt(NS, "diamond_1", "hbm:crystal_diamond", "minecraft:diamond", 2.0, 2);
        smelt(NS, "fluorite", "hbm:crystal_fluorite", "hbm:fluorite", 2.0, 6);
        smelt(NS, "glass_ash", "hbm:ash_digamma", "hbm:glass_ash", 10.0);
        smelt(NS, "glass_boron", "hbm:sand_boron", "hbm:glass_boron", 0.25);
        smelt(NS, "glass_lead", "hbm:sand_lead", "hbm:glass_lead", 0.25);
        smelt(NS, "glass_trinitite", "hbm:waste_trinitite", "hbm:glass_trinitite", 0.25);
        smelt(NS, "glass_trinitite_1", "hbm:waste_trinitite_red", "hbm:glass_trinitite", 0.25);
        smelt(NS, "glyphid_meat_grilled", "hbm:glyphid_meat", "hbm:glyphid_meat_grilled", 1.0);
        smelt(NS, "gold_ingot_1", "hbm:ore_gneiss_gold", "minecraft:gold_ingot", 5.0);
        smelt(NS, "gold_ingot_2", "hbm:powder_gold", "minecraft:gold_ingot", 1.0);
        smelt(NS, "gold_ingot_3", "hbm:crystal_gold", "minecraft:gold_ingot", 2.0, 2);
        smelt(NS, "ingot_actinium", "hbm:powder_actinium", "hbm:ingot_actinium", 1.0);
        smelt(NS, "ingot_aluminium_1", "hbm:powder_aluminium", "hbm:ingot_aluminium", 1.0);
        smelt(NS, "ingot_aluminium_2", "hbm:crystal_aluminium", "hbm:ingot_aluminium", 2.0, 2);
        smelt(NS, "ingot_asbestos", "hbm:powder_asbestos", "hbm:ingot_asbestos", 1.0);
        smelt(NS, "ingot_au198", "hbm:powder_au198", "hbm:ingot_au198", 1.0);
        smelt(NS, "ingot_australium", "hbm:powder_australium", "hbm:ingot_australium", 5.0);
        smelt(NS, "ingot_bakelite", "hbm:powder_bakelite", "hbm:ingot_bakelite", 1.0);
        smelt(NS, "ingot_beryllium", "hbm:ore_beryllium", "hbm:ingot_beryllium", 2.0);
        smelt(NS, "ingot_beryllium_1", "hbm:powder_beryllium", "hbm:ingot_beryllium", 1.0);
        smelt(NS, "ingot_beryllium_2", "hbm:crystal_beryllium", "hbm:ingot_beryllium", 2.0, 2);
        smelt(NS, "ingot_biorubber", "hbm:ball_resin", "hbm:ingot_biorubber", 0.1);
        smelt(NS, "ingot_bismuth", "hbm:powder_bismuth", "hbm:ingot_bismuth", 1.0);
        smelt(NS, "ingot_boron", "hbm:powder_boron", "hbm:ingot_boron", 1.0);
        smelt(NS, "ingot_cadmium", "hbm:powder_cadmium", "hbm:ingot_cadmium", 1.0);
        smelt(NS, "ingot_calcium", "hbm:powder_calcium", "hbm:ingot_calcium", 1.0);
        smelt(NS, "ingot_cobalt", "hbm:ore_cobalt", "hbm:ingot_cobalt", 2.0);
        smelt(NS, "ingot_cobalt_1", "hbm:ore_nether_cobalt", "hbm:ingot_cobalt", 2.0);
        smelt(NS, "ingot_cobalt_2", "hbm:powder_cobalt", "hbm:ingot_cobalt", 1.0);
        smelt(NS, "ingot_cobalt_3", "hbm:crystal_cobalt", "hbm:ingot_cobalt", 2.0, 2);
        smelt(NS, "ingot_combine_steel", "hbm:powder_combine_steel", "hbm:ingot_combine_steel", 1.0);
        smelt(NS, "ingot_combine_steel_1", "hbm:combine_scrap", "hbm:ingot_combine_steel", 1.0);
        smelt(NS, "ingot_copper_1", "hbm:ore_copper", "hbm:ingot_copper", 2.5);
        smelt(NS, "ingot_copper_2", "hbm:ore_gneiss_copper", "hbm:ingot_copper", 5.0);
        smelt(NS, "ingot_copper_3", "hbm:powder_copper", "hbm:ingot_copper", 1.0);
        smelt(NS, "ingot_copper_4", "hbm:crystal_copper", "hbm:ingot_copper", 2.0, 2);
        smelt(NS, "ingot_desh", "hbm:powder_desh", "hbm:ingot_desh", 1.0);
        smelt(NS, "ingot_desh_1", "hbm:arc_electrode_burnt_desh", "hbm:ingot_desh", 3.0);
        smelt(NS, "ingot_dineutronium", "hbm:powder_dineutronium", "hbm:ingot_dineutronium", 5.0);
        smelt(NS, "ingot_dura_steel", "hbm:powder_dura_steel", "hbm:ingot_dura_steel", 1.0);
        smelt(NS, "ingot_euphemium", "hbm:powder_euphemium", "hbm:ingot_euphemium", 10.0);
        smelt(NS, "ingot_firebrick_1", "hbm:ball_fireclay", "hbm:ingot_firebrick", 0.1);
        smelt(NS, "ingot_lanthanium", "hbm:powder_lanthanium", "hbm:ingot_lanthanium", 1.0);
        smelt(NS, "ingot_lanthanium_1", "hbm:arc_electrode_burnt_lanthanium", "hbm:ingot_lanthanium", 3.0);
        smelt(NS, "ingot_lead", "hbm:ore_lead", "hbm:ingot_lead", 3.0);
        smelt(NS, "ingot_lead_1", "hbm:powder_lead", "hbm:ingot_lead", 1.0);
        smelt(NS, "ingot_lead_2", "hbm:crystal_lead", "hbm:ingot_lead", 2.0, 2);
        smelt(NS, "ingot_magnetized_tungsten_1", "hbm:powder_magnetized_tungsten", "hbm:ingot_magnetized_tungsten", 1.0);
        smelt(NS, "ingot_neptunium", "hbm:powder_neptunium", "hbm:ingot_neptunium", 1.0);
        smelt(NS, "ingot_niobium", "hbm:powder_niobium", "hbm:ingot_niobium", 1.0);
        smelt(NS, "ingot_osmiridium", "hbm:crystal_osmiridium", "hbm:ingot_osmiridium", 2.0);
        smelt(NS, "ingot_plutonium", "hbm:ore_nether_plutonium", "hbm:ingot_plutonium", 24.0);
        smelt(NS, "ingot_plutonium_1", "hbm:powder_plutonium", "hbm:ingot_plutonium", 1.0);
        smelt(NS, "ingot_plutonium_2", "hbm:crystal_plutonium", "hbm:ingot_plutonium", 2.0, 2);
        smelt(NS, "ingot_plutonium_3", "hbm:crystal_trixite", "hbm:ingot_plutonium", 2.0, 4);
        smelt(NS, "ingot_polonium", "hbm:powder_polonium", "hbm:ingot_polonium", 1.0);
        smelt(NS, "ingot_polymer", "hbm:powder_polymer", "hbm:ingot_polymer", 1.0);
        smelt(NS, "ingot_ra226", "hbm:powder_ra226", "hbm:ingot_ra226", 1.0);
        smelt(NS, "ingot_red_copper_1", "hbm:powder_red_copper", "hbm:ingot_red_copper", 1.0);
        smelt(NS, "ingot_saturnite", "hbm:arc_electrode_burnt_saturnite", "hbm:ingot_saturnite", 3.0);
        smelt(NS, "ingot_schrabidate", "hbm:powder_schrabidate", "hbm:ingot_schrabidate", 5.0);
        smelt(NS, "ingot_schrabidium_1", "hbm:ore_schrabidium", "hbm:ingot_schrabidium", 128.0);
        smelt(NS, "ingot_schrabidium_2", "hbm:ore_nether_schrabidium", "hbm:ingot_schrabidium", 256.0);
        smelt(NS, "ingot_schrabidium_3", "hbm:ore_gneiss_schrabidium", "hbm:ingot_schrabidium", 256.0);
        smelt(NS, "ingot_schrabidium_4", "hbm:powder_schrabidium", "hbm:ingot_schrabidium", 5.0);
        smelt(NS, "ingot_schrabidium_5", "hbm:crystal_schrabidium", "hbm:ingot_schrabidium", 2.0, 2);
        smelt(NS, "ingot_sr90", "hbm:powder_sr90", "hbm:ingot_sr90", 1.0);
        smelt(NS, "ingot_starmetal", "hbm:crystal_starmetal", "hbm:ingot_starmetal", 2.0, 2);
        smelt(NS, "ingot_steel", "hbm:powder_steel", "hbm:ingot_steel", 1.0);
        smelt(NS, "ingot_tantalium", "hbm:powder_tantalium", "hbm:ingot_tantalium", 1.0);
        smelt(NS, "ingot_tcalloy", "hbm:powder_tcalloy", "hbm:ingot_tcalloy", 1.0);
        smelt(NS, "ingot_th232", "hbm:ore_thorium", "hbm:ingot_th232", 3.0);
        smelt(NS, "ingot_th232_1", "hbm:powder_thorium", "hbm:ingot_th232", 1.0);
        smelt(NS, "ingot_th232_2", "hbm:crystal_thorium", "hbm:ingot_th232", 2.0, 2);
        smelt(NS, "ingot_titanium", "hbm:ore_titanium", "hbm:ingot_titanium", 3.0);
        smelt(NS, "ingot_titanium_1", "hbm:powder_titanium", "hbm:ingot_titanium", 1.0);
        smelt(NS, "ingot_titanium_2", "hbm:crystal_titanium", "hbm:ingot_titanium", 2.0, 2);
        smelt(NS, "ingot_tungsten_1", "hbm:ore_tungsten", "hbm:ingot_tungsten", 6.0);
        smelt(NS, "ingot_tungsten_2", "hbm:ore_nether_tungsten", "hbm:ingot_tungsten", 12.0);
        smelt(NS, "ingot_tungsten_3", "hbm:powder_tungsten", "hbm:ingot_tungsten", 1.0);
        smelt(NS, "ingot_tungsten_4", "hbm:crystal_tungsten", "hbm:ingot_tungsten", 2.0, 2);
        smelt(NS, "ingot_uranium", "hbm:ore_uranium", "hbm:ingot_uranium", 6.0);
        smelt(NS, "ingot_uranium_1", "hbm:ore_uranium_scorched", "hbm:ingot_uranium", 6.0);
        smelt(NS, "ingot_uranium_2", "hbm:ore_nether_uranium", "hbm:ingot_uranium", 12.0);
        smelt(NS, "ingot_uranium_3", "hbm:ore_nether_uranium_scorched", "hbm:ingot_uranium", 12.0);
        smelt(NS, "ingot_uranium_4", "hbm:ore_gneiss_uranium", "hbm:ingot_uranium", 12.0);
    }

    private static void smelting1() {
        smelt(NS, "ingot_uranium_5", "hbm:ore_gneiss_uranium_scorched", "hbm:ingot_uranium", 12.0);
        smelt(NS, "ingot_uranium_6", "hbm:powder_uranium", "hbm:ingot_uranium", 1.0);
        smelt(NS, "ingot_uranium_7", "hbm:crystal_uranium", "hbm:ingot_uranium", 2.0, 2);
        smelt(NS, "ingot_zirconium", "hbm:powder_zirconium", "hbm:ingot_zirconium", 1.0);
        smelt(NS, "iron_ingot", "hbm:ore_gneiss_iron", "minecraft:iron_ingot", 5.0);
        smelt(NS, "iron_ingot_1", "hbm:powder_iron", "minecraft:iron_ingot", 1.0);
        smelt(NS, "iron_ingot_2", "hbm:crystal_iron", "minecraft:iron_ingot", 2.0, 2);
        smelt(NS, "lignite", "hbm:lignite", "hbm:lignite", 1.0);
        smelt(NS, "lithium", "hbm:ore_gneiss_lithium", "hbm:lithium", 10.0);
        smelt(NS, "lithium_1", "hbm:powder_lithium", "hbm:lithium", 1.0);
        smelt(NS, "lithium_2", "hbm:crystal_lithium", "hbm:lithium", 2.0, 2);
        smelt(NS, "niter", "hbm:crystal_niter", "hbm:niter", 2.0, 6);
        smelt(NS, "nugget_australium", "hbm:ore_australium", "hbm:nugget_australium", 2.5);
        smelt(NS, "nugget_schrabidium", "hbm:ingot_schraranium", "hbm:nugget_schrabidium", 2.0);
        smelt(NS, "nugget_schrabidium_1", "hbm:crystal_schraranium", "hbm:nugget_schrabidium", 2.0, 2);
        smelt(NS, "obsidian", "hbm:gravel_obsidian", "minecraft:obsidian", 0.0);
        smelt(NS, "ore_gneiss_uranium_scorched_smelting", "hbm:ore_gneiss_uranium_scorched", "hbm:ingot_uranium", 0.7);
        smelt(NS, "ore_gneiss_uranium_smelting", "hbm:ore_gneiss_uranium", "hbm:ingot_uranium", 0.7);
        smelt(NS, "ore_nether_uranium_scorched_smelting", "hbm:ore_nether_uranium_scorched", "hbm:ingot_uranium", 0.7);
        smelt(NS, "ore_nether_uranium_smelting", "hbm:ore_nether_uranium", "hbm:ingot_uranium", 0.7);
        smelt(NS, "ore_uranium_scorched_smelting", "hbm:ore_uranium_scorched", "hbm:ingot_uranium", 0.7);
        smelt(NS, "ore_uranium_smelting", "hbm:ore_uranium", "hbm:ingot_uranium", 0.7);
        smelt(NS, "powder_desh_mix", "hbm:crystal_rare", "hbm:powder_desh_mix", 2.0);
        smelt(NS, "powder_fire", "hbm:crystal_phosphorus", "hbm:powder_fire", 2.0, 6);
        smelt(NS, "redstone", "hbm:crystal_redstone", "minecraft:redstone", 2.0, 6);
        smelt(NS, "sulfur", "hbm:crystal_sulfur", "hbm:sulfur", 2.0, 6);
    }
}
