package com.hbm.inventory.material;

import dev.golem.core.dejson.Tags;

import static com.hbm.inventory.material.MaterialShapes.*;

/**
 * Every {@code c:} convention tag hbm's materials belong to, declared through Golem Core's
 * {@code dejson} vocabulary instead of written as JSON.
 *
 * <p>The {@value #FILES_REPLACED} tag files these declarations stand in for have been deleted.
 * 578 of them said the only thing a material tag usually says — that {@code c:ingots/iron} contains
 * {@code hbm:ingot_iron} — which is not a fact about iron so much as a restatement of
 * {@link MaterialShapes}, one file at a time. Those are now a single line per material listing the
 * shapes it comes in, and the item name is derived from the shape exactly as the item's own
 * registration derives it.
 *
 * <p>The rest are not derivable and are spelled out: an item whose name does not follow its shape
 * ({@code c:ingots/carbon} is {@code hbm:ingot_graphite}), a tag that also admits a vanilla item or
 * a vanilla tag, and the {@code any_*} aggregates, which are tags of tags rather than tags of
 * items.
 *
 * <p>{@link com.hbm.gametest.MaterialTagGameTests} is the other half: it asks the live item
 * registry which {@code <prefix>_<material>} items actually exist and fails if one of them is in no
 * tag, which is the failure this file cannot see on its own — a silently untagged item makes a
 * recipe reject a valid ingredient and nothing anywhere reports it.
 */
public final class ModMaterialTags {

    private static final String NS = "c";

    /** How many tag files the declarations below stand in for; quoted in this class's own javadoc. */
    static final int FILES_REPLACED = 787;

    private static boolean declared;

    private ModMaterialTags() {}

    public static void declare() {
        if (declared) {
            return;
        }
        declared = true;

        shapes0();
        shapes1();
        shapes2();
        named0();
        named1();
        named2();
    }

    /** The derivable case: this material comes in these shapes, under the names the shapes imply. */
    private static void shapes(String material, MaterialShapes... shapes) {
        for (MaterialShapes shape : shapes) {
            Tags.item(NS, shape.tagPlural + "/" + material, "hbm:" + shape.itemPrefix + "_" + material);
        }
    }

    private static void tag(String path, String... values) {
        Tags.item(NS, path, values);
    }

    private static void blockTag(String path, String... values) {
        Tags.block(NS, path, values);
    }

    private static void shapes0() {
        shapes("actinium", NUGGET, FRAGMENT, BILLET, INGOT, DUST, BLOCK);
        shapes("actinium_tiny", DUST);
        shapes("alexandrite", GEM);
        shapes("aluminium", INGOT, CRYSTAL, DUST, PLATE, CASTPLATE, WELDEDPLATE, SHELL, PIPE, BLOCK, ORE);
        shapes("am241", NUGGET, BILLET, INGOT);
        shapes("am242", NUGGET, BILLET, INGOT);
        shapes("am_mix", NUGGET, BILLET, INGOT);
        shapes("americium_fuel", NUGGET, BILLET, INGOT);
        shapes("armor_ajr", PLATE);
        shapes("armor_dnt", PLATE);
        shapes("armor_fau", PLATE);
        shapes("armor_hev", PLATE);
        shapes("armor_lunar", PLATE);
        shapes("armor_titanium", PLATE);
        shapes("arsenic", NUGGET, INGOT);
        shapes("arsenic_bronze", INGOT, CASTPLATE, LIGHTBARREL, LIGHTRECEIVER, HEAVYRECEIVER);
        shapes("asbestos", INGOT, CRYSTAL, DUST, BLOCK);
        shapes("astatine", INGOT, DUST);
        shapes("at209", DUST);
        shapes("au198", NUGGET, BILLET, INGOT, DUST, BLOCK);
        shapes("australium", NUGGET, BILLET, INGOT, DUST, ORE);
        shapes("australium_greater", NUGGET, BILLET);
        shapes("australium_lesser", NUGGET, BILLET);
        shapes("bakelite", INGOT, DUST, STOCK, GRIP, BLOCK);
        shapes("balefire", DUST);
        shapes("balefire_gold", BILLET);
        shapes("beryllium", NUGGET, BILLET, INGOT, CRYSTAL, DUST, BLOCK, ORE);
        shapes("biorubber", INGOT);
        shapes("bismuth", NUGGET, BILLET, INGOT, DUST, PLATE, BLOCK);
        shapes("bismuth_bronze", INGOT, CASTPLATE, LIGHTBARREL, LIGHTRECEIVER, HEAVYRECEIVER);
        shapes("bone", GRIP);
        shapes("borax", DUST);
        shapes("boron", INGOT, DUST, BLOCK);
        shapes("boron_tiny", DUST);
        shapes("bromine", INGOT, DUST);
        shapes("bscco", INGOT, DENSEWIRE);
        shapes("c4", INGOT);
        shapes("cadmium", NUGGET, INGOT, DUST, BLOCK);
        shapes("caesium", INGOT, DUST);
        shapes("calcium", INGOT, DUST);
        shapes("cdalloy", 
                INGOT, DUST, CASTPLATE, WELDEDPLATE, LIGHTBARREL, HEAVYBARREL, LIGHTRECEIVER, HEAVYRECEIVER, BLOCK);
        shapes("cerium", FRAGMENT, INGOT, DUST);
        shapes("cft", INGOT);
        shapes("charred", CRYSTAL);
        shapes("chlorocalcite", DUST);
        shapes("chlorophyte", DUST);
        shapes("cinnabar", CRYSTAL);
        shapes("cloud", DUST);
        shapes("co60", NUGGET, BILLET, INGOT, DUST);
        shapes("coal", CRYSTAL, DUST);
        shapes("cobalt", BILLET, INGOT, CRYSTAL, DUST, BLOCK);
        shapes("coltan", FRAGMENT, BLOCK, ORE);
        shapes("coltan_ore", DUST);
        shapes("combine_steel", INGOT, DUST, PLATE, CASTPLATE, WELDEDPLATE, BLOCK);
        shapes("copper", CRYSTAL, DUST, DENSEWIRE, PLATE, CASTPLATE, WELDEDPLATE, SHELL, PIPE);
        shapes("cs137", DUST);
        shapes("daffergon", NUGGET, INGOT, DUST);
        shapes("dalekanium", PLATE);
        shapes("desh", 
                NUGGET, INGOT, DUST, PLATE, CASTPLATE, LIGHTBARREL, HEAVYBARREL, LIGHTRECEIVER, STOCK, GRIP, BLOCK);
        shapes("desh_mix", DUST);
        shapes("desh_ready", DUST);
        shapes("diamond", CRYSTAL, DUST);
        shapes("dineutronium", NUGGET, INGOT, DUST, DENSEWIRE, PLATE, BLOCK);
        shapes("dura_steel", 
                BOLT, INGOT, DUST, PLATE, CASTPLATE, PIPE, LIGHTBARREL, HEAVYBARREL, LIGHTRECEIVER, HEAVYRECEIVER,
                GRIP, BLOCK);
        shapes("emerald", DUST);
        shapes("energy", CRYSTAL);
        shapes("euphemium", NUGGET, INGOT, DUST, PLATE, BLOCK);
        shapes("ferrouranium", INGOT, CASTPLATE, HEAVYBARREL, HEAVYRECEIVER);
        shapes("fiberglass", INGOT);
        shapes("fire", DUST);
        shapes("firebrick", INGOT);
        shapes("flashlead", BILLET);
        shapes("fluorite", CRYSTAL, BLOCK, ORE);
        shapes("flux", DUST);
        shapes("gh336", NUGGET, BILLET, INGOT);
        shapes("gold", CRYSTAL, DUST, DENSEWIRE, PLATE, CASTPLATE);
        shapes("graphite", INGOT, BLOCK);
        shapes("gunmetal", 
                INGOT, PLATE, LIGHTBARREL, HEAVYBARREL, LIGHTRECEIVER, HEAVYRECEIVER, MECHANISM, STOCK, GRIP);
        shapes("hes", NUGGET, BILLET, INGOT);
        shapes("horn", CRYSTAL);
    }

    private static void shapes1() {
        shapes("i131", INGOT, DUST);
        shapes("ice", DUST);
        shapes("impure_osmiridium", DUST);
        shapes("iodine", INGOT, DUST);
        shapes("iron", CRYSTAL, DUST, PLATE, CASTPLATE, WELDEDPLATE, PIPE);
        shapes("kevlar", PLATE);
        shapes("lanthanium", INGOT, DUST, BLOCK);
        shapes("lanthanium_tiny", DUST);
        shapes("lapis", CRYSTAL, DUST);
        shapes("lead", NUGGET, BOLT, INGOT, CRYSTAL, DUST, PLATE, CASTPLATE, PIPE, BLOCK, ORE);
        shapes("les", NUGGET, BILLET, INGOT);
        shapes("lignite", DUST, ORE);
        shapes("limestone", DUST);
        shapes("lithium", CRYSTAL, DUST, BLOCK);
        shapes("magic", DUST);
        shapes("magnetized_tungsten", INGOT, DUST, DENSEWIRE, BLOCK);
        shapes("mercury", NUGGET);
        shapes("mercury_tiny", NUGGET);
        shapes("meteorite", FRAGMENT, DUST);
        shapes("meteorite_tiny", DUST);
        shapes("mixed", PLATE);
        shapes("molysite", DUST);
        shapes("mox_fuel", NUGGET, BILLET, INGOT);
        shapes("mud", INGOT);
        shapes("neodymium", DUST, DENSEWIRE);
        shapes("neptunium", NUGGET, BILLET, INGOT, DUST, BLOCK);
        shapes("neptunium_fuel", NUGGET, BILLET, INGOT);
        shapes("niobium", INGOT, DUST, DENSEWIRE, BLOCK);
        shapes("nitan_mix", DUST);
        shapes("niter", CRYSTAL, BLOCK, ORE);
        shapes("nuclear_waste", BILLET);
        shapes("oil_sand", ORE);
        shapes("osmiridium", NUGGET, INGOT, CRYSTAL, DUST, CASTPLATE, WELDEDPLATE);
        shapes("paa", PLATE);
        shapes("paleogenite", DUST);
        shapes("pb209", NUGGET, BILLET, INGOT, DUST);
        shapes("pc", INGOT, STOCK, GRIP);
        shapes("phosphorus", INGOT, CRYSTAL);
        shapes("plutonium", NUGGET, BILLET, INGOT, CRYSTAL, DUST, BLOCK);
        shapes("plutonium_fuel", NUGGET, BILLET, INGOT);
        shapes("po210be", BILLET);
        shapes("poison", DUST);
        shapes("polonium", NUGGET, BILLET, INGOT, DUST, BLOCK);
        shapes("polymer", INGOT, DUST, PLATE, STOCK, GRIP, BLOCK);
        shapes("power", DUST);
        shapes("pu238", NUGGET, BILLET, INGOT, BLOCK);
        shapes("pu238be", BILLET);
        shapes("pu239", NUGGET, BILLET, INGOT, BLOCK);
        shapes("pu240", NUGGET, BILLET, INGOT, BLOCK);
        shapes("pu241", NUGGET, BILLET, INGOT);
        shapes("pu_mix", NUGGET, BILLET, INGOT, BLOCK);
        shapes("pvc", INGOT, STOCK, GRIP);
        shapes("quartz", DUST);
        shapes("ra226", NUGGET, BILLET, INGOT, DUST, BLOCK);
        shapes("ra226be", BILLET);
        shapes("rad", GEM);
        shapes("rare", CRYSTAL);
        shapes("red_copper", INGOT, DUST, DENSEWIRE, BLOCK);
        shapes("red_phosphorus", BLOCK);
        shapes("redstone", CRYSTAL);
        shapes("reiium", NUGGET, INGOT, DUST);
        shapes("rubber", INGOT, PIPE, GRIP, BLOCK);
        shapes("saturnite", 
                INGOT, PLATE, CASTPLATE, SHELL, LIGHTBARREL, HEAVYBARREL, LIGHTRECEIVER, HEAVYRECEIVER, MECHANISM,
                STOCK, GRIP, BLOCK);
        shapes("sawdust", DUST);
        shapes("schrabidate", INGOT, DUST, DENSEWIRE, CASTPLATE, BLOCK);
        shapes("schrabidium", NUGGET, BILLET, INGOT, CRYSTAL, DUST, DENSEWIRE, PLATE, CASTPLATE, BLOCK);
        shapes("schrabidium_fuel", NUGGET, BILLET, INGOT);
        shapes("schraranium", INGOT, CRYSTAL, BLOCK);
        shapes("semtex_mix", DUST);
        shapes("silicon", NUGGET, BILLET, INGOT);
        shapes("slag", BLOCK);
        shapes("sodalite", GEM);
        shapes("sodium", DUST);
        shapes("solinium", NUGGET, BILLET, INGOT, BLOCK);
        shapes("spark_mix", DUST);
        shapes("sr90", NUGGET, BILLET, INGOT, DUST);
        shapes("starmetal", INGOT, CRYSTAL, DENSEWIRE, CASTPLATE, BLOCK);
        shapes("steel", 
                BOLT, INGOT, DUST, PLATE, CASTPLATE, WELDEDPLATE, SHELL, PIPE, LIGHTBARREL, HEAVYBARREL,
                LIGHTRECEIVER, GRIP, BLOCK);
        shapes("steel_tiny", DUST);
        shapes("strontium", NUGGET, DUST);
    }

    private static void shapes2() {
        shapes("sulfur", CRYSTAL, BLOCK);
        shapes("tantalium", NUGGET, INGOT, GEM, DUST, BLOCK);
        shapes("tcalloy", 
                INGOT, DUST, CASTPLATE, WELDEDPLATE, LIGHTBARREL, HEAVYBARREL, LIGHTRECEIVER, HEAVYRECEIVER, BLOCK);
        shapes("technetium", NUGGET, BILLET, INGOT);
        shapes("tektite", DUST);
        shapes("tennessine", INGOT, DUST);
        shapes("th232", NUGGET, BILLET, INGOT);
        shapes("thermite", DUST);
        shapes("thorium", CRYSTAL, DUST, BLOCK, ORE);
        shapes("thorium_fuel", NUGGET, BILLET, INGOT);
        shapes("tikite", ORE);
        shapes("titanium", INGOT, CRYSTAL, DUST, DENSEWIRE, PLATE, CASTPLATE, WELDEDPLATE, SHELL, BLOCK, ORE);
        shapes("trixite", CRYSTAL);
        shapes("tungsten", BOLT, INGOT, CRYSTAL, DUST, DENSEWIRE, CASTPLATE, WELDEDPLATE, BLOCK);
        shapes("tungsten_carbide", INGOT);
        shapes("u233", NUGGET, BILLET, INGOT, BLOCK);
        shapes("u235", NUGGET, BILLET, INGOT, BLOCK);
        shapes("u238", NUGGET, BILLET, INGOT, BLOCK);
        shapes("unobtainium", NUGGET, BILLET, INGOT, DUST);
        shapes("unobtainium_greater", NUGGET);
        shapes("unobtainium_lesser", NUGGET);
        shapes("uranium", NUGGET, BILLET, INGOT, CRYSTAL, DUST, BLOCK);
        shapes("uranium_fuel", NUGGET, BILLET, INGOT);
        shapes("uzh", BILLET);
        shapes("verticium", NUGGET, INGOT, DUST);
        shapes("volcanic", GEM);
        shapes("weaponsteel", 
                INGOT, PLATE, CASTPLATE, SHELL, LIGHTBARREL, HEAVYBARREL, LIGHTRECEIVER, HEAVYRECEIVER, MECHANISM,
                STOCK, GRIP);
        shapes("weidanium", NUGGET, INGOT, DUST);
        shapes("white_phosphorus", BLOCK);
        shapes("wood", STOCK, GRIP);
        shapes("xe135", DUST);
        shapes("yellowcake", DUST);
        shapes("yharonite", BILLET);
        shapes("zfb_am_mix", BILLET);
        shapes("zfb_bismuth", BILLET);
        shapes("zfb_pu241", BILLET);
        shapes("zirconium", NUGGET, BILLET, INGOT, DUST, CASTPLATE, WELDEDPLATE, BLOCK);
    }

    private static void named0() {
        blockTag("ores/uranium", 
                "hbm:ore_uranium", "hbm:ore_uranium_scorched", "hbm:ore_gneiss_uranium",
                "hbm:ore_gneiss_uranium_scorched", "hbm:ore_nether_uranium", "hbm:ore_nether_uranium_scorched");
        tag("barrels_heavy/any_resistant_alloy", "#c:barrels_heavy/cdalloy", "#c:barrels_heavy/tcalloy");
        tag("barrels_light/any_resistant_alloy", "#c:barrels_light/cdalloy", "#c:barrels_light/tcalloy");
        tag("billets/thorium", "hbm:billet_th232");
        tag("cobblestones", "minecraft:cobblestone");
        tag("cokes", "hbm:coke_coal", "hbm:coke_lignite", "hbm:coke_petroleum");
        tag("concretes", 
                "hbm:concrete", "hbm:concrete_smooth", "hbm:concrete_asbestos", "hbm:ducrete", "hbm:ducrete_smooth",
                "hbm:concrete_colored", "hbm:concrete_colored_ext");
        tag("crystals/cryolite", "hbm:chunk_ore_cryolite");
        tag("crystals/red_phosphorus", "hbm:crystal_phosphorus");
        tag("dusts/any_plastic", "#c:dusts/bakelite", "#c:dusts/polymer");
        tag("dusts/any_propellant", "hbm:ballistite", "hbm:cordite", "minecraft:gunpowder");
        tag("dusts/any_resistant_alloy", "#c:dusts/cdalloy", "#c:dusts/tcalloy");
        tag("dusts/any_smokeless", "hbm:ballistite", "hbm:cordite");
        tag("dusts/ash", 
                "hbm:powder_ash_wood", "hbm:powder_ash_coal", "hbm:powder_ash_misc", "hbm:powder_ash_fly",
                "hbm:powder_ash_soot");
        tag("dusts/coltan", "hbm:powder_coltan", "hbm:powder_coltan_ore");
        tag("dusts/fluorite", "hbm:fluorite");
        tag("dusts/glowstone", "minecraft:glowstone_dust");
        tag("dusts/niter", "hbm:niter");
        tag("dusts/red_phosphorus", "hbm:powder_fire");
        tag("dusts/redstone", "minecraft:redstone");
        tag("dusts/sulfur", "hbm:sulfur");
        tag("dusts_tiny/actinium", "hbm:powder_actinium_tiny");
        tag("dusts_tiny/at209", "hbm:powder_at209_tiny");
        tag("dusts_tiny/au198", "hbm:powder_au198_tiny");
        tag("dusts_tiny/boron", "hbm:powder_boron_tiny");
        tag("dusts_tiny/cerium", "hbm:powder_cerium_tiny");
        tag("dusts_tiny/co60", "hbm:powder_co60_tiny");
        tag("dusts_tiny/coal", "hbm:powder_coal_tiny");
        tag("dusts_tiny/cobalt", "hbm:powder_cobalt_tiny");
        tag("dusts_tiny/cs137", "hbm:powder_cs137_tiny");
        tag("dusts_tiny/i131", "hbm:powder_i131_tiny");
        tag("dusts_tiny/iodine", "hbm:powder_iodine_tiny");
        tag("dusts_tiny/lanthanium", "hbm:powder_lanthanium_tiny");
        tag("dusts_tiny/lithium", "hbm:powder_lithium_tiny");
        tag("dusts_tiny/meteorite", "hbm:powder_meteorite_tiny");
        tag("dusts_tiny/neodymium", "hbm:powder_neodymium_tiny");
        tag("dusts_tiny/niobium", "hbm:powder_niobium_tiny");
        tag("dusts_tiny/paleogenite", "hbm:powder_paleogenite_tiny");
        tag("dusts_tiny/pb209", "hbm:powder_pb209_tiny");
        tag("dusts_tiny/sr90", "hbm:powder_sr90_tiny");
        tag("dusts_tiny/steel", "hbm:powder_steel_tiny");
        tag("dusts_tiny/xe135", "hbm:powder_xe135_tiny");
        tag("dyes/black", 
                "hbm:chemical_dye_black", "hbm:crayon_black", "hbm:powder_ash_coal", "hbm:powder_ash_soot",
                "hbm:powder_coal", "hbm:oil_tar_crack", "hbm:oil_tar_crude", "minecraft:black_dye");
        tag("dyes/blue", "hbm:chemical_dye_blue", "hbm:crayon_blue", "hbm:powder_lapis", "minecraft:blue_dye");
        tag("dyes/brown", 
                "hbm:chemical_dye_brown", "hbm:crayon_brown", "hbm:powder_ash_fly", "hbm:powder_lignite",
                "minecraft:brown_dye");
        tag("dyes/cyan", "hbm:chemical_dye_cyan", "hbm:crayon_cyan", "hbm:oil_tar_wax", "minecraft:cyan_dye");
        tag("dyes/gray", 
                "hbm:chemical_dye_gray", "hbm:crayon_gray", "hbm:oil_tar_coal", "hbm:powder_ash_misc",
                "minecraft:gray_dye");
        tag("dyes/green", "hbm:chemical_dye_green", "hbm:crayon_green", "minecraft:green_dye");
        tag("dyes/light_blue", "hbm:chemical_dye_lightblue", "hbm:crayon_lightblue", "minecraft:light_blue_dye");
        tag("dyes/light_gray", 
                "hbm:chemical_dye_silver", "hbm:crayon_silver", "hbm:powder_ash_wood", "hbm:powder_titanium",
                "minecraft:light_gray_dye");
        tag("dyes/lime", "hbm:chemical_dye_lime", "hbm:crayon_lime", "minecraft:lime_dye");
        tag("dyes/magenta", 
                "hbm:chemical_dye_magenta", "hbm:crayon_magenta", "hbm:powder_ash_fullerene",
                "minecraft:magenta_dye");
        tag("dyes/orange", 
                "hbm:chemical_dye_orange", "hbm:crayon_orange", "hbm:powder_cadmium", "minecraft:orange_dye");
        tag("dyes/pink", "hbm:chemical_dye_pink", "hbm:crayon_pink", "minecraft:pink_dye");
        tag("dyes/purple", "hbm:chemical_dye_purple", "hbm:crayon_purple", "minecraft:purple_dye");
        tag("dyes/red", "hbm:chemical_dye_red", "hbm:crayon_red", "hbm:cinnabar", "minecraft:red_dye");
        tag("dyes/white", 
                "hbm:chemical_dye_white", "hbm:crayon_white", "hbm:fluorite", "hbm:oil_tar_paraffin",
                "minecraft:white_dye");
        tag("dyes/yellow", "hbm:chemical_dye_yellow", "hbm:crayon_yellow", "hbm:sulfur", "minecraft:yellow_dye");
        tag("fragments/aluminium", "hbm:bedrock_ore_fragment_aluminium");
        tag("fragments/asbestos", "hbm:bedrock_ore_fragment_asbestos");
        tag("fragments/bauxite", "hbm:bedrock_ore_fragment_bauxite");
        tag("fragments/beryllium", "hbm:bedrock_ore_fragment_beryllium");
        tag("fragments/bismuth", "hbm:bedrock_ore_fragment_bismuth");
        tag("fragments/borax", "hbm:bedrock_ore_fragment_borax");
        tag("fragments/boron", "hbm:bedrock_ore_fragment_boron", "hbm:fragment_boron");
        tag("fragments/chlorocalcite", "hbm:bedrock_ore_fragment_chlorocalcite");
        tag("fragments/cinnabar", "hbm:bedrock_ore_fragment_cinnabar");
        tag("fragments/coal", "hbm:bedrock_ore_fragment_coal");
        tag("fragments/cobalt", "hbm:bedrock_ore_fragment_cobalt", "hbm:fragment_cobalt");
        tag("fragments/copper", "hbm:bedrock_ore_fragment_copper");
    }

    private static void named1() {
        tag("fragments/cryolite", "hbm:bedrock_ore_fragment_cryolite");
        tag("fragments/diamond", "hbm:bedrock_ore_fragment_diamond");
        tag("fragments/emerald", "hbm:bedrock_ore_fragment_emerald");
        tag("fragments/fluorite", "hbm:bedrock_ore_fragment_fluorite");
        tag("fragments/gold", "hbm:bedrock_ore_fragment_gold");
        tag("fragments/iron", "hbm:bedrock_ore_fragment_iron");
        tag("fragments/lanthanium", "hbm:bedrock_ore_fragment_lanthanium", "hbm:fragment_lanthanium");
        tag("fragments/lead", "hbm:bedrock_ore_fragment_lead");
        tag("fragments/lignite", "hbm:bedrock_ore_fragment_lignite");
        tag("fragments/lithium", "hbm:bedrock_ore_fragment_lithium");
        tag("fragments/molysite", "hbm:bedrock_ore_fragment_molysite");
        tag("fragments/neodymium", "hbm:bedrock_ore_fragment_neodymium", "hbm:fragment_neodymium");
        tag("fragments/niobium", "hbm:bedrock_ore_fragment_niobium", "hbm:fragment_niobium");
        tag("fragments/niter", "hbm:bedrock_ore_fragment_niter");
        tag("fragments/polonium", "hbm:bedrock_ore_fragment_polonium");
        tag("fragments/ra226", "hbm:bedrock_ore_fragment_ra226");
        tag("fragments/rare", "hbm:bedrock_ore_fragment_rare");
        tag("fragments/red_phosphorus", "hbm:bedrock_ore_fragment_red_phosphorus");
        tag("fragments/redstone", "hbm:bedrock_ore_fragment_redstone");
        tag("fragments/silicon", "hbm:bedrock_ore_fragment_silicon");
        tag("fragments/sodalite", "hbm:bedrock_ore_fragment_sodalite");
        tag("fragments/sodium", "hbm:bedrock_ore_fragment_sodium");
        tag("fragments/strontium", "hbm:bedrock_ore_fragment_strontium");
        tag("fragments/sulfur", "hbm:bedrock_ore_fragment_sulfur");
        tag("fragments/tantalium", "hbm:bedrock_ore_fragment_tantalium");
        tag("fragments/technetium", "hbm:bedrock_ore_fragment_technetium");
        tag("fragments/thorium", "hbm:bedrock_ore_fragment_thorium");
        tag("fragments/titanium", "hbm:bedrock_ore_fragment_titanium");
        tag("fragments/tungsten", "hbm:bedrock_ore_fragment_tungsten");
        tag("fragments/u238", "hbm:bedrock_ore_fragment_u238");
        tag("fragments/uranium", "hbm:bedrock_ore_fragment_uranium");
        tag("fragments/zirconium", "hbm:bedrock_ore_fragment_zirconium");
        tag("gems/bauxite", "hbm:stone_resource_bauxite");
        tag("gems/cinnabar", "hbm:cinnabar");
        tag("gems/coal", "minecraft:coal");
        tag("gems/coal_coke", "hbm:coke_coal");
        tag("gems/diamond", "minecraft:diamond");
        tag("gems/emerald", "minecraft:emerald");
        tag("gems/lapis", "minecraft:lapis_lazuli");
        tag("gems/latex", "hbm:ball_resin");
        tag("gems/lignite", "hbm:lignite");
        tag("gems/lignite_coke", "hbm:coke_lignite");
        tag("gems/pet_coke", "hbm:coke_petroleum");
        tag("gems/quartz", "minecraft:quartz");
        tag("glass_blocks", "hbm:glass_boron", "hbm:glass_lead");
        tag("glass_panes", 
                "minecraft:glass_pane", "minecraft:white_stained_glass_pane", "minecraft:orange_stained_glass_pane",
                "minecraft:magenta_stained_glass_pane", "minecraft:light_blue_stained_glass_pane",
                "minecraft:yellow_stained_glass_pane", "minecraft:lime_stained_glass_pane",
                "minecraft:pink_stained_glass_pane", "minecraft:gray_stained_glass_pane",
                "minecraft:light_gray_stained_glass_pane", "minecraft:cyan_stained_glass_pane",
                "minecraft:purple_stained_glass_pane", "minecraft:blue_stained_glass_pane",
                "minecraft:brown_stained_glass_pane", "minecraft:green_stained_glass_pane",
                "minecraft:red_stained_glass_pane", "minecraft:black_stained_glass_pane");
        tag("grips/any_hardplastic", "#c:grips/pc", "#c:grips/pvc");
        tag("grips/any_plastic", "#c:grips/bakelite", "#c:grips/polymer");
        tag("ingots/any_bismoid", "#c:ingots/arsenic", "#c:ingots/bismuth");
        tag("ingots/any_hardplastic", "#c:ingots/pc", "#c:ingots/pvc");
        tag("ingots/any_highexplosive", "hbm:ball_tatb", "hbm:ball_tnt");
        tag("ingots/any_plastic", "#c:ingots/bakelite", "#c:ingots/polymer");
        tag("ingots/any_plasticexplosive", "hbm:ingot_c4", "hbm:ingot_semtex");
        tag("ingots/any_resistant_alloy", "#c:ingots/cdalloy", "#c:ingots/tcalloy");
        tag("ingots/any_rubber", "#c:ingots/rubber");
        tag("ingots/borax", "hbm:ingot_raw_borax");
        tag("ingots/carbon", "hbm:ingot_graphite");
        tag("ingots/coltan", "hbm:fragment_coltan");
        tag("ingots/copper", "hbm:ingot_copper", "minecraft:copper_ingot");
        tag("ingots/gold", "minecraft:gold_ingot");
        tag("ingots/iron", "minecraft:iron_ingot");
        tag("ingots/latex", "hbm:ingot_biorubber");
        tag("ingots/lithium", "hbm:lithium");
        tag("ingots/malachite", "hbm:chunk_ore_malachite");
        tag("ingots/neodymium", "hbm:ingot_raw_neodymium");
        tag("ingots/rare", "hbm:chunk_ore_rare");
        tag("ingots/redstone", "hbm:ingot_raw_redstone");
        tag("ingots/slag", "hbm:ingot_raw_slag");
        tag("ingots/sodium", "hbm:ingot_raw_sodium");
        tag("ingots/strontium", "hbm:ingot_raw_strontium");
    }

    private static void named2() {
        tag("ingots/thorium", "hbm:ingot_th232");
        tag("nuggets/any_bismoid", "#c:nuggets/arsenic", "#c:nuggets/bismuth");
        tag("nuggets/boron", "hbm:fragment_boron");
        tag("nuggets/cerium", "hbm:fragment_cerium");
        tag("nuggets/cobalt", "hbm:fragment_cobalt", "hbm:nugget_cobalt");
        tag("nuggets/gold", "minecraft:gold_nugget");
        tag("nuggets/lanthanium", "hbm:fragment_lanthanium");
        tag("nuggets/neodymium", "hbm:fragment_neodymium");
        tag("nuggets/niobium", "hbm:fragment_niobium", "hbm:nugget_niobium");
        tag("nuggets/thorium", "hbm:nugget_th232");
        tag("ores/asbestos", "hbm:ore_asbestos", "hbm:ore_gneiss_asbestos", "hbm:stone_resource_asbestos");
        tag("ores/borax", "hbm:ore_depth_borax");
        tag("ores/cinnabar", "hbm:ore_cinnabar", "hbm:ore_depth_cinnabar");
        tag("ores/coal", "#minecraft:coal_ores", "hbm:ore_nether_coal");
        tag("ores/cobalt", "hbm:ore_cobalt", "hbm:ore_nether_cobalt");
        tag("ores/copper", "#minecraft:copper_ores", "hbm:ore_copper", "hbm:ore_gneiss_copper");
        tag("ores/diamond", "#minecraft:diamond_ores", "hbm:gravel_diamond", "hbm:ore_sellafield_diamond");
        tag("ores/emerald", "#minecraft:emerald_ores", "hbm:ore_sellafield_emerald");
        tag("ores/gold", "#minecraft:gold_ores", "hbm:ore_gneiss_gold");
        tag("ores/hematite", "hbm:stone_resource_hematite");
        tag("ores/iron", "#minecraft:iron_ores", "hbm:ore_gneiss_iron");
        tag("ores/lapis", "#minecraft:lapis_ores");
        tag("ores/limestone", "hbm:stone_resource_limestone");
        tag("ores/lithium", "hbm:ore_gneiss_lithium");
        tag("ores/malachite", "hbm:stone_resource_malachite");
        tag("ores/neodymium", "hbm:ore_depth_nether_neodymium");
        tag("ores/nitan", "hbm:ore_depth_nether_nitan");
        tag("ores/osmiridium", "hbm:ore_tektite_osmiridium");
        tag("ores/plutonium", "hbm:ore_nether_plutonium");
        tag("ores/rare", "hbm:ore_gneiss_rare", "hbm:ore_rare");
        tag("ores/redstone", "#minecraft:redstone_ores");
        tag("ores/schrabidium", 
                "hbm:ore_gneiss_schrabidium", "hbm:ore_nether_schrabidium", "hbm:ore_schrabidium",
                "hbm:ore_sellafield_schrabidium");
        tag("ores/smoldering", "hbm:ore_nether_smoldering");
        tag("ores/sulfur", "hbm:ore_nether_sulfur", "hbm:ore_sulfur", "hbm:stone_resource_sulfur");
        tag("ores/tungsten", "hbm:ore_nether_tungsten", "hbm:ore_tungsten");
        tag("ores/uranium", 
                "hbm:ore_gneiss_uranium", "hbm:ore_gneiss_uranium_scorched", "hbm:ore_nether_uranium",
                "hbm:ore_nether_uranium_scorched", "hbm:ore_sellafield_uranium_scorched", "hbm:ore_uranium",
                "hbm:ore_uranium_scorched");
        tag("ores/zirconium", "hbm:ore_depth_zirconium");
        tag("raw_materials/copper", "minecraft:raw_copper");
        tag("raw_materials/gold", "minecraft:raw_gold");
        tag("raw_materials/iron", "minecraft:raw_iron");
        tag("receivers_heavy/any_resistant_alloy", "#c:receivers_heavy/cdalloy", "#c:receivers_heavy/tcalloy");
        tag("receivers_light/any_resistant_alloy", "#c:receivers_light/cdalloy", "#c:receivers_light/tcalloy");
        tag("rods/wooden", "minecraft:stick");
        tag("stocks/any_hardplastic", "#c:stocks/pc", "#c:stocks/pvc");
        tag("stocks/any_plastic", "#c:stocks/bakelite", "#c:stocks/polymer");
        tag("storage_blocks/any_bismoid", "#c:storage_blocks/bismuth");
        tag("storage_blocks/any_plastic", "#c:storage_blocks/bakelite", "#c:storage_blocks/polymer");
        tag("storage_blocks/carbon", "hbm:block_graphite");
        tag("storage_blocks/coal", "minecraft:coal_block");
        tag("storage_blocks/copper", "hbm:block_copper", "minecraft:copper_block");
        tag("storage_blocks/diamond", "minecraft:diamond_block");
        tag("storage_blocks/emerald", "minecraft:emerald_block");
        tag("storage_blocks/gold", "minecraft:gold_block");
        tag("storage_blocks/iron", "minecraft:iron_block");
        tag("storage_blocks/lapis", "minecraft:lapis_block");
        tag("storage_blocks/phosphorus", "hbm:block_white_phosphorus");
        tag("storage_blocks/redstone", "minecraft:redstone_block");
        tag("tars", "hbm:oil_tar_crude", "hbm:oil_tar_coal", "hbm:oil_tar_crack", "hbm:oil_tar_wood");
        tag("wires/aluminium", "hbm:wire_fine_aluminium");
        tag("wires/carbon", "hbm:wire_fine_carbon");
        tag("wires/copper", "hbm:wire_fine_copper");
        tag("wires/gold", "hbm:wire_fine_gold");
        tag("wires/lead", "hbm:wire_fine_lead");
        tag("wires/magnetized_tungsten", "hbm:wire_fine_magnetized_tungsten");
        tag("wires/red_copper", "hbm:wire_fine_red_copper");
        tag("wires/schrabidium", "hbm:wire_fine_schrabidium");
        tag("wires/steel", "hbm:wire_fine_steel");
        tag("wires/tungsten", "hbm:wire_fine_tungsten");
        tag("wires/zirconium", "hbm:wire_fine_zirconium");
    }
}
