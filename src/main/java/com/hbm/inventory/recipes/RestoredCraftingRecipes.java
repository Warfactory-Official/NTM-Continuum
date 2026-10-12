package com.hbm.inventory.recipes;

import com.hbm.items.special.CellVariants;
import com.hbm.lib.ComponentIds;
import com.hbm.lib.ItemIds;
import com.hbm.lib.TagIds;
import com.hbm.lib.VanillaIds;
import dev.golem.core.dejson.Crafting.Ingredient;
import dev.golem.core.dejson.Crafting.Key;
import dev.golem.core.dejson.Crafting.Stack;

import static dev.golem.core.dejson.Crafting.anyOf;
import static dev.golem.core.dejson.Crafting.componentShaped;
import static dev.golem.core.dejson.Crafting.componentShapeless;
import static dev.golem.core.dejson.Crafting.fluid;
import static dev.golem.core.dejson.Crafting.item;
import static dev.golem.core.dejson.Crafting.key;
import static dev.golem.core.dejson.Crafting.requires;
import static dev.golem.core.dejson.Crafting.rows;
import static dev.golem.core.dejson.Crafting.shaped;
import static dev.golem.core.dejson.Crafting.shapeless;
import static dev.golem.core.dejson.Crafting.smelt;
import static dev.golem.core.dejson.Crafting.special;
import static dev.golem.core.dejson.Crafting.stack;
import static dev.golem.core.dejson.Crafting.tag;

/**
 * The original-hbm (1.7.10) crafting-table and furnace recipes the port was missing, restored in fix phase 1B
 * (History §19ej-1b) from the {@code ...Recipes.java} classes in {@code crafting}, {@code main/CraftingManager.java} and
 * {@code SmeltingRecipes.java} at default config. Each block names its 1.7 source lines.
 *
 * <p>Spelling conventions are the rest of the port's: a 1.7 ore-dictionary name is its {@code c:} tag, a metadata
 * variant is the flattened id (History §19es-itemvariant), a 1.7 cell is the port's {@code cell} carrying the fluid
 * and leaving the empty cell, {@code Items.potionitem} is the water bottle consumed whole. A result that carries
 * components goes through the mod's own component recipe types, which read result components on every target.
 * Recipes 1.7 decided in Java ({@code IRecipe} handlers) and the ones whose result is stack state a declaration
 * cannot state are the {@link #handlers()} below.
 */
public final class RestoredCraftingRecipes {

    private static final String NS = "hbm";
    private static boolean declared;

    /** 1.7 fluid ids (declaration order of {@code Fluids}) of the fluids carried by 1.7's cell items. */
    // registry names, never numeric fluid ids (History section 19ej-ids)
    private static final String TRITIUM = "hbm:tritium", UF6 = "hbm:uf6", SAS3 = "hbm:sas3", AMAT = "hbm:amat",
            ASCHRAB = "hbm:aschrab";

    /** {@code GunFactory.EnumModSpecial}, whose ordinal is the {@code weapon_mod_special} variant. */
    private static final String[] MOD_SPECIAL = {"SILENCER", "SCOPE", "SAW", "GREASEGUN", "SLOWDOWN", "SPEEDUP",
            "CHOKE", "SPEEDLOADER", "FURNITURE_GREEN", "FURNITURE_BLACK", "BAYONET", "STACK_MAG", "SKIN_SATURNITE",
            "LAS_SHOTGUN", "LAS_CAPACITOR", "LAS_AUTO", "NICKEL", "DOUBLOONS", "DRILL_HSS", "DRILL_WEAPONSTEEL",
            "DRILL_TCALLOY", "DRILL_SATURNITE", "ENGINE_DIESEL", "ENGINE_AVIATION", "ENGINE_ELECTRIC", "ENGINE_TURBO",
            "MAGNET", "SIFTER", "CANISTERS"};
    /** {@code GunFactory.EnumModGeneric}, whose ordinal is the {@code weapon_mod_generic} variant. */
    private static final String[] MOD_GENERIC = {"IRON_DAMAGE", "IRON_DURA", "STEEL_DAMAGE", "STEEL_DURA",
            "DURA_DAMAGE", "DURA_DURA", "DESH_DAMAGE", "DESH_DURA", "WSTEEL_DAMAGE", "WSTEEL_DURA", "FERRO_DAMAGE",
            "FERRO_DURA", "TCALLOY_DAMAGE", "TCALLOY_DURA", "BIGMT_DAMAGE", "BIGMT_DURA", "BRONZE_DAMAGE",
            "BRONZE_DURA"};

    private RestoredCraftingRecipes() {}

    public static void declare() {
        if (declared) return;
        declared = true;
        consumables();
        crafting();
        minerals();
        powders();
        weapons();
        weaponMods();
        tools();
        slabs();
        smelting();
        handlers();
    }

    // ---- helpers -------------------------------------------------------------------------------------------------

    // 1.7's weapon-mod metadata, one item per constant here (ntm-next ids; History §19es-itemvariant)
    private static String modId(String base, String[] values, String name) {
        if (!java.util.Arrays.asList(values).contains(name)) throw new IllegalArgumentException(name);
        return "hbm:" + base + "_" + name.toLowerCase(java.util.Locale.ROOT);
    }

    private static Stack specialStack(String name) {
        return stack(modId("weapon_mod_special", MOD_SPECIAL, name));
    }

    private static Ingredient specialMod(String name) {
        return item(modId("weapon_mod_special", MOD_SPECIAL, name));
    }

    private static Stack generic(String name) {
        return stack(modId("weapon_mod_generic", MOD_GENERIC, name));
    }

    /** Items.potionitem meta 0 - the water bottle - consumed whole (1.7's potion had no container item). */
    private static Ingredient waterBottle() {
        return item("minecraft:potion").potion("minecraft:water").remainder("minecraft:air");
    }

    /** A 1.7 {@code cell_*} item: the port's cell full of that fluid, leaving the empty cell (its 1.7 container item). */
    private static Ingredient cell(String fluidId) {
        return item(ItemIds.cell).with(ComponentIds.fluid, fluidId).with(ComponentIds.fluid_amount, 1000)
                .remainder(ItemIds.cell);
    }

    /** {@code ItemAmmoArty.ArtilleryShellType} in 1.7 metadata order, by the shells' (ntm-next) item ids. */
    private static final String[] ARTY = {"ammo_arty", "ammo_arty_classic", "ammo_arty_he", "ammo_arty_mini_nuke",
            "ammo_arty_nuke", "ammo_arty_phosphorus", "ammo_arty_mini_nuke_multi", "ammo_arty_phosphorus_multi",
            "ammo_arty_cargo", "ammo_arty_chlorine", "ammo_arty_phosgene", "ammo_arty_mustard_gas"};

    private static Ingredient arty(int variant) {
        return item("hbm:" + ARTY[variant]);
    }

    private static Key k(String symbol, String idOrTag) {
        return key(symbol, idOrTag.startsWith("#") ? tag(idOrTag) : item(idOrTag));
    }

    private static Ingredient i(String idOrTag) {
        return idOrTag.startsWith("#") ? tag(idOrTag) : item(idOrTag);
    }

    // ---- ConsumableRecipes ---------------------------------------------------------------------------------------

    private static void consumables() {
        // :33-36 airstrike callers (bomb_caller metadata 0, 1, 2, 4 = carpet, napalm, chlorine, abomb)
        shaped(NS, "bomb_caller_carpet", "hbm:bomb_caller_carpet", rows("TTT", "TRT", "TTT"),
                "T", VanillaIds.tnt, "R", ItemIds.rangefinder);
        shaped(NS, "bomb_caller_napalm", "hbm:bomb_caller_napalm", rows("TTT", "TRT", "TTT"),
                "T", ItemIds.grenade_filling_inc, "R", ItemIds.rangefinder);
        shaped(NS, "bomb_caller_chlorine", "hbm:bomb_caller_chlorine", rows("TTT", "TRT", "TTT"),
                "T", ItemIds.pellet_gas, "R", ItemIds.rangefinder);
        shaped(NS, "bomb_caller_abomb", "hbm:bomb_caller_abomb", rows("TRC"),
                "T", ItemIds.ammo_standard_nuke_high, "R", ItemIds.rangefinder, "C", ItemIds.circuit_controller);

        // :62 coffee, :74-79 sodas, :87-89 bottles, :93-94 bottle2, :115 syringe_taint - all on Items.potionitem
        componentShapeless(NS, "coffee", stack(ItemIds.coffee), tag(TagIds.dusts_coal), item(VanillaIds.milk_bucket),
                waterBottle(), item(VanillaIds.sugar));
        soda("can_smart", ItemIds.can_smart, ItemIds.can_empty, TagIds.dusts_niter);
        soda("can_redbomb", ItemIds.can_redbomb, ItemIds.can_empty, ItemIds.pellet_cluster);
        soda("can_mrsugar", ItemIds.can_mrsugar, ItemIds.can_empty, TagIds.dusts_fluorite);
        soda("can_overcharge", ItemIds.can_overcharge, ItemIds.can_empty, TagIds.dusts_sulfur);
        soda("can_luna", ItemIds.can_luna, ItemIds.can_empty, ItemIds.powder_meteorite_tiny);
        soda("bottle_nuka", ItemIds.bottle_nuka, ItemIds.bottle_empty, TagIds.dusts_coal);
        soda("bottle_cherry", ItemIds.bottle_cherry, ItemIds.bottle_empty, TagIds.dusts_redstone);
        soda("bottle_quantum", ItemIds.bottle_quantum, ItemIds.bottle_empty, ItemIds.trinitite);
        soda("bottle2_korl", ItemIds.bottle2_korl, ItemIds.bottle2_empty, TagIds.dusts_copper);
        soda("bottle2_fritz", ItemIds.bottle2_fritz, ItemIds.bottle2_empty, TagIds.dusts_tungsten);
        componentShapeless(NS, "syringe_taint", stack(ItemIds.syringe_taint), item(ItemIds.bottle2_empty),
                item(ItemIds.syringe_metal_empty), item(ItemIds.ducttape), item(ItemIds.powder_magic),
                tag(TagIds.nuggets_schrabidium), waterBottle());

        // :118 iodine pills, :130-131 the default (non-LBSM) siox and xanax, :149 radaway flush
        shaped(NS, "pill_iodine", ItemIds.pill_iodine, 8, rows("IF"), "I", TagIds.dusts_iodine, "F", TagIds.dusts_fluorite);
        requires(shapeless(NS, "siox", ItemIds.siox, 8, TagIds.dusts_coal, TagIds.dusts_asbestos, ItemIds.nugget_bismuth),
                "!lbsm_simple_medicine");
        requires(shapeless(NS, "xanax", ItemIds.xanax, TagIds.dusts_coal, TagIds.dusts_niter, TagIds.dusts_bromine),
                "!lbsm_simple_medicine");
        shapeless(NS, "radaway_flush", ItemIds.radaway_flush, ItemIds.radaway_strong, TagIds.dusts_iodine);

        // :135 the first of the six med bags (leather, stimpaks, antidote)
        shaped(NS, "med_bag_antidote", ItemIds.med_bag, rows("LLL", "SIS", "LLL"),
                "L", VanillaIds.leather, "S", ItemIds.syringe_metal_stimpak, "I", ItemIds.syringe_antidote);
    }

    private static void soda(String id, String result, String container, String flavour) {
        componentShapeless(NS, id, stack(result), item(container), waterBottle(), item(VanillaIds.sugar), i(flavour));
    }

    // ---- CraftingManager -----------------------------------------------------------------------------------------

    private static void crafting() {
        // :103 asbestos cloth (Blocks.wool is any colour: 1.7 recipes read a Block ingredient as wildcard metadata)
        shaped(NS, "asbestos_cloth", ItemIds.asbestos_cloth, 8, rows("SCS", "CPC", "SCS"),
                "S", VanillaIds.string, "P", TagIds.dusts_bromine, "C", TagIds.minecraft_wool);
        // :107, :111 the fiberglass and nether-brick polymer plates
        shaped(NS, "plate_polymer_fiberglass", ItemIds.plate_polymer, 16, rows("DD"), "D", TagIds.ingots_fiberglass);
        // no :111 plate_polymer from "ingotNetherBrick": nothing in 1.7 registers that ore name (Forge 1.7 names the
        // nether brick "ingotBrickNether", OreDictManager.KEY_NETHERBRICK), so the 1.7 recipe can never be crafted
        // :209 memespoon
        shaped(NS, "memespoon", ItemIds.memespoon, rows("CGC", "PSP", "IAI"),
                "C", ItemIds.powder_yellowcake, "G", TagIds.storage_blocks_thorium, "P", ItemIds.photo_panel,
                "S", ItemIds.steel_shovel, "I", ItemIds.plate_polymer, "A", TagIds.ingots_australium);
        // :337-338 re-sharpening worn blades (the blade's damage is not checked)
        shaped(NS, "blades_steel_sharpen", ItemIds.blades_steel, rows("PIP"),
                "P", TagIds.plates_steel, "I", ItemIds.blades_steel);
        shaped(NS, "blades_titanium_sharpen", ItemIds.blades_titanium, rows("PIP"),
                "P", TagIds.plates_titanium, "I", ItemIds.blades_titanium);
        // :392-393 any coloured concrete back to smooth concrete
        // (1.7 wildcard metadata: every colour, one item each here; History §19es-itemvariant)
        componentShapeless(NS, "concrete_smooth_from_colored", stack(ItemIds.concrete_smooth), anyOf("hbm:concrete_white", "hbm:concrete_orange", "hbm:concrete_magenta", "hbm:concrete_light_blue", "hbm:concrete_yellow", "hbm:concrete_lime", "hbm:concrete_pink", "hbm:concrete_gray", "hbm:concrete_silver", "hbm:concrete_cyan", "hbm:concrete_purple", "hbm:concrete_blue", "hbm:concrete_brown", "hbm:concrete_green", "hbm:concrete_red", "hbm:concrete_black"));
        componentShapeless(NS, "concrete_smooth_from_colored_ext", stack(ItemIds.concrete_smooth), anyOf("hbm:concrete_ext_machine", "hbm:concrete_ext_machine_stripe", "hbm:concrete_ext_indigo", "hbm:concrete_ext_purple", "hbm:concrete_ext_pink", "hbm:concrete_ext_hazard", "hbm:concrete_ext_sand", "hbm:concrete_ext_bronze"));
        // :429 compound bricks
        shaped(NS, "brick_compound_from_reinforced_brick", ItemIds.brick_compound, 4, rows("FBF", "BTB", "FBF"),
                "F", TagIds.bolts_steel, "B", ItemIds.reinforced_brick, "T", TagIds.tars);
        // :471-473 dyed steel scaffolds (variants 1-3; the scaffold ingredient is any variant)
        scaffold(1, "red", TagIds.dyes_red);
        scaffold(2, "white", TagIds.dyes_white);
        scaffold(3, "yellow", TagIds.dyes_yellow);
        // :564-567 crystals
        shapeless(NS, "crystal_horn", ItemIds.crystal_horn, TagIds.dusts_neptunium, TagIds.dusts_iodine,
                TagIds.dusts_thorium, TagIds.dusts_astatine, TagIds.dusts_neodymium, TagIds.dusts_caesium,
                ItemIds.block_meteor, ItemIds.gravel_obsidian, VanillaIds.water_bucket);
        shapeless(NS, "crystal_charred", ItemIds.crystal_charred, TagIds.dusts_strontium, TagIds.dusts_cobalt,
                TagIds.dusts_bromine, TagIds.dusts_niobium, TagIds.dusts_tennessine, TagIds.dusts_cerium,
                ItemIds.block_meteor, TagIds.storage_blocks_aluminium, VanillaIds.water_bucket);
        componentShaped(NS, "crystal_pulsar", stack(ItemIds.crystal_pulsar, 32), rows("STS", "THT", "STS"),
                key("S", cell(UF6)), k("T", TagIds.dusts_aluminium), k("H", ItemIds.crystal_charred));
        // :586-588 empty tanks and barrels (the port's fluid_tank/fluid_barrel without contents)
        shaped(NS, "fluid_tank_empty", ItemIds.fluid_tank, 8, rows("121", "1G1", "121"),
                "1", TagIds.plates_aluminium, "2", TagIds.plates_iron, "G", TagIds.glass_panes);
        componentShaped(NS, "fluid_tank_lead_empty", stack(ItemIds.fluid_tank_lead_empty, 4), rows("LUL", "LTL", "LUL"),
                k("L", TagIds.plates_lead), k("U", TagIds.billets_u238),
                key("T", item(ItemIds.fluid_tank).without(ComponentIds.fluid, ComponentIds.fluid_amount)));
        shaped(NS, "fluid_barrel_empty", ItemIds.fluid_barrel, 2, rows("121", "1G1", "121"),
                "1", TagIds.plates_steel, "2", TagIds.plates_aluminium, "G", TagIds.glass_panes);
        // :663 a typed duct back to the universal duct
        shapeless(NS, "fluid_duct_neo_from_fluid_duct", ItemIds.fluid_duct_neo, ItemIds.fluid_duct);
        // :666 coke torches, :835 the tar name tag
        shaped(NS, "torch_coke", VanillaIds.torch, 8, rows("L", "S"), "L", TagIds.cokes, "S", VanillaIds.stick);
        shaped(NS, "name_tag_tar", VanillaIds.name_tag, rows("SB ", "BPB", " BP"),
                "S", VanillaIds.string, "B", TagIds.tars, "P", VanillaIds.paper);
        // :669-670 launch pad structure parts (fluid_duct_neo wildcard = all three duct skins)
        shaped(NS, "struct_launcher", ItemIds.struct_launcher, 8, rows("PPP", "SDS", "CCC"),
                "P", TagIds.plates_steel, "S", "#hbm:steel_scaffolds", "D", TagIds.pipes_steel, "C", TagIds.concretes);
        componentShaped(NS, "struct_scaffold", stack(ItemIds.struct_scaffold, 8), rows("SSS", "DCD", "SSS"),
                k("S", "#hbm:steel_scaffolds"),
                key("D", anyOf(ItemIds.fluid_duct_neo, ItemIds.fluid_duct_neo_silver, ItemIds.fluid_duct_neo_colored)),
                k("C", ItemIds.red_cable));
        // :697, :707-708 AMS catalysts
        shaped(NS, "ams_catalyst_blank", ItemIds.ams_catalyst_blank, rows("TET", "ETE", "TET"),
                "T", TagIds.dusts_tennessine, "E", TagIds.ingots_euphemium);
        shapeless(NS, "ams_catalyst_cerium", ItemIds.ams_catalyst_cerium, ItemIds.ams_catalyst_blank,
                ItemIds.rune_hagalaz, ItemIds.rune_hagalaz, ItemIds.rune_jera, ItemIds.rune_thurisaz,
                TagIds.dusts_cerium, TagIds.dusts_cerium, TagIds.dusts_cerium, TagIds.dusts_cerium);
        shapeless(NS, "ams_catalyst_caesium", ItemIds.ams_catalyst_caesium, ItemIds.ams_catalyst_blank,
                ItemIds.rune_isa, ItemIds.rune_isa, ItemIds.rune_thurisaz, ItemIds.rune_thurisaz,
                TagIds.dusts_caesium, TagIds.dusts_caesium, TagIds.dusts_caesium, TagIds.dusts_caesium);
        // :766-767 ReaSim rods (not enable528-gated), :990 the default (!enable528) moderated rod
        shaped(NS, "rbmk_fuel_rod_reasim", ItemIds.rbmk_fuel_rod_reasim, rows("ZCZ", "ZRZ", "ZCZ"),
                "C", TagIds.shells_steel, "R", ItemIds.rbmk_blank, "Z", TagIds.ingots_zirconium);
        shaped(NS, "rbmk_fuel_rod_reasim_moderated", ItemIds.rbmk_fuel_rod_reasim_moderated, rows("BGB", "GRG", "BGB"),
                "G", TagIds.storage_blocks_graphite, "R", ItemIds.rbmk_fuel_rod_reasim,
                "B", TagIds.ingots_any_resistant_alloy);
        requires(shaped(NS, "rbmk_fuel_rod_moderated", ItemIds.rbmk_fuel_rod_moderated, rows("BGB", "GRG", "BGB"),
                "G", TagIds.storage_blocks_graphite, "R", ItemIds.rbmk_fuel_rod, "B", ItemIds.nugget_bismuth), "!enable_528");
    }

    private static void scaffold(int variant, String colour, String dye) {
        componentShaped(NS, "steel_scaffold_" + colour, stack("hbm:steel_scaffold_" + colour, 8),
                rows("SSS", "SDS", "SSS"), k("S", "#hbm:steel_scaffolds"), k("D", dye));
    }

    // ---- MineralRecipes ------------------------------------------------------------------------------------------

    private static void minerals() {
        // :95-104 add1To9PairSameMeta for every waste class of the long/short storage-drum waste; each class is an item
        // of its own ({@code <family>_<class>}, History §19es-itemvariant)
        // 1.7's metadata order (ItemWasteLong/ItemWasteShort.WasteClass), as ItemWasteStorage's enums
        String[] longClasses = {"uranium235", "uranium233", "neptunium", "thorium", "schrabidium"};
        String[] shortClasses = {"uranium235", "uranium233", "neptunium", "plutonium239", "plutonium240", "plutonium241",
                "americium242", "schrabidium"};
        String[][] waste = {
                {"nuclear_waste_long", "nuclear_waste_long_tiny"},
                {"nuclear_waste_long_depleted", "nuclear_waste_long_depleted_tiny"},
                {"nuclear_waste_short", "nuclear_waste_short_tiny"},
                {"nuclear_waste_short_depleted", "nuclear_waste_short_depleted_tiny"}};
        for (String[] pair : waste) {
            String[] classes = pair[0].contains("_long") ? longClasses : shortClasses;
            for (int c = 0; c < classes.length; c++) wastePair(pair[0], pair[1], c, classes[c]);
        }
        // :425 the alternative ammo container
        componentShaped(NS, "ammo_container_1", stack("hbm:ammo_container_alt"), rows("##", "##"),
                k("#", ItemIds.nitra));
        // :448 PU mix from tiny nuggets (:447's "nuggetPluonium240" sibling is a 1.7 typo that never matched)
        shapeless(NS, "ingot_pu_mix_from_nuggets", ItemIds.ingot_pu_mix, TagIds.nuggets_pu239, TagIds.nuggets_pu239,
                TagIds.nuggets_pu239, TagIds.nuggets_pu239, TagIds.nuggets_pu239, TagIds.nuggets_pu239,
                TagIds.nuggets_pu240, TagIds.nuggets_pu240, TagIds.nuggets_pu240);
    }

    /** The 1.7 pair for waste class {@code c}; recipe names keep 1.7's class number. */
    private static void wastePair(String a, String b, int c, String waste) {
        String one = "hbm:" + a + "_" + waste, nine = "hbm:" + b + "_" + waste;
        shapeless(NS, a + "_to_" + b + "_" + c, nine, 9, one);
        shaped(NS, b + "_to_" + a + "_" + c, one, rows("###", "###", "###"), "#", nine);
    }

    // ---- PowderRecipes -------------------------------------------------------------------------------------------

    private static void powders() {
        // :50-51 nitan mix, :56-57 desh mix
        shapeless(NS, "powder_nitan_mix", ItemIds.powder_nitan_mix, 6, TagIds.dusts_neptunium, TagIds.dusts_iodine,
                TagIds.dusts_thorium, TagIds.dusts_astatine, TagIds.dusts_neodymium, TagIds.dusts_caesium);
        shapeless(NS, "powder_nitan_mix_1", ItemIds.powder_nitan_mix, 6, TagIds.dusts_strontium, TagIds.dusts_cobalt,
                TagIds.dusts_bromine, TagIds.dusts_tennessine, TagIds.dusts_niobium, TagIds.dusts_cerium);
        shapeless(NS, "powder_desh_mix_from_tiny_dusts", ItemIds.powder_desh_mix, TagIds.dusts_tiny_boron, TagIds.dusts_tiny_boron,
                TagIds.dusts_tiny_lanthanium, TagIds.dusts_tiny_lanthanium, TagIds.dusts_tiny_cerium,
                TagIds.dusts_tiny_cobalt, TagIds.dusts_tiny_lithium, TagIds.dusts_tiny_neodymium,
                TagIds.dusts_tiny_niobium);
        shapeless(NS, "powder_desh_mix_from_dusts", ItemIds.powder_desh_mix, 9, TagIds.dusts_boron, TagIds.dusts_boron,
                TagIds.dusts_lanthanium, TagIds.dusts_lanthanium, TagIds.dusts_cerium, TagIds.dusts_cobalt,
                TagIds.dusts_lithium, TagIds.dusts_neodymium, TagIds.dusts_niobium);
    }

    // ---- WeaponRecipes -------------------------------------------------------------------------------------------

    private static void weapons() {
        // :89, :100, :103 the three guns with a scope
        componentShaped(NS, "gun_amat", stack(ItemIds.gun_amat), rows(" C ", "BRS", " MG"),
                k("G", TagIds.grips_wood), k("B", TagIds.barrels_heavy_ferrouranium),
                k("R", TagIds.receivers_heavy_ferrouranium), k("M", TagIds.mechanisms_weaponsteel),
                key("C", specialMod("SCOPE")), k("S", TagIds.stocks_wood));
        componentShaped(NS, "gun_stg77", stack(ItemIds.gun_stg77), rows(" D ", "BRS", "GGM"),
                key("D", specialMod("SCOPE")), k("B", TagIds.barrels_light_saturnite),
                k("R", TagIds.receivers_light_saturnite), k("S", TagIds.stocks_any_hardplastic),
                k("G", TagIds.grips_any_hardplastic), k("M", TagIds.mechanisms_saturnite));
        componentShaped(NS, "gun_lasrifle", stack(ItemIds.gun_lasrifle), rows("DLC", "BRS", "MG "),
                k("D", ItemIds.crystal_redstone), key("L", specialMod("SCOPE")), k("C", ItemIds.circuit_bismoid),
                k("B", TagIds.barrels_light_any_bismoid_bronze), k("R", TagIds.receivers_light_any_bismoid_bronze),
                k("S", TagIds.stocks_any_hardplastic), k("M", TagIds.mechanisms_saturnite),
                k("G", TagIds.grips_any_hardplastic));

        // :175 schrabidium missile
        componentShapeless(NS, "missile_schrabidium", stack(ItemIds.missile_schrabidium), item(ItemIds.missile_assembly),
                item(ItemIds.ducttape), cell(ASCHRAB), item(ItemIds.circuit_quantum));

        // :246 the mini-nuke shell (variant 4); :242-245 (variants 5-7) are in CraftingRecipesGenerated
        componentShapeless(NS, "ammo_arty_4_mini_nuke", stack("hbm:ammo_arty_nuke"), arty(2),
                item(ItemIds.boy_bullet), item(ItemIds.boy_target), item(ItemIds.boy_shielding),
                item(ItemIds.circuit_controller), item(ItemIds.ducttape));

        // :256 fire extinguisher sand charge (sand_mix BORON)
        shaped(NS, "ammo_fireext_sand", ItemIds.ammo_fireext_sand, rows("NNN", "NFN", "NNN"),
                "N", ItemIds.sand_boron, "F", ItemIds.ammo_fireext);

        // :277, :281 grenade fillings
        componentShaped(NS, "grenade_filling_plasma", stack(ItemIds.grenade_filling_plasma, 4), rows(" C ", "KWK"),
                k("C", ItemIds.circuit_capacitor_board), key("K", cell(TRITIUM)), k("W", TagIds.plates_weaponsteel));
        componentShaped(NS, "grenade_filling_schrab", stack(ItemIds.grenade_filling_schrab), rows("BCB", "TST", "BCB"),
                k("B", TagIds.plates_cast_combine_steel), k("C", ItemIds.circuit_controller), k("T", ItemIds.ball_tatb),
                key("S", cell(SAS3)));

        // :315-316 custom missile warhead fillings
        componentShaped(NS, "custom_hydro", stack(ItemIds.custom_hydro), rows(" C ", "LTL", "LIL"),
                k("C", TagIds.plates_copper), k("L", TagIds.plates_lead), k("I", TagIds.plates_iron),
                key("T", cell(TRITIUM)));
        componentShaped(NS, "custom_amat", stack(ItemIds.custom_amat), rows(" C ", "MMM", "AAA"),
                k("C", TagIds.plates_copper), k("A", TagIds.plates_aluminium), key("M", cell(AMAT)));
    }

    /** WeaponRecipes:120-164, the SEDNA weapon modifications. */
    private static void weaponMods() {
        genericMod("IRON_DAMAGE", TagIds.ingots_gunmetal, TagIds.ingots_iron, 3);
        genericMod("IRON_DURA", TagIds.ingots_gunmetal, TagIds.ingots_iron, 1);
        genericMod("STEEL_DAMAGE", TagIds.mechanisms_gunmetal, TagIds.plates_cast_steel, 3);
        genericMod("STEEL_DURA", TagIds.plates_gunmetal, TagIds.plates_cast_steel, 1);
        genericMod("DURA_DAMAGE", TagIds.mechanisms_gunmetal, TagIds.plates_cast_dura_steel, 3);
        genericMod("DURA_DURA", TagIds.plates_gunmetal, TagIds.plates_cast_dura_steel, 1);
        genericMod("DESH_DAMAGE", TagIds.mechanisms_gunmetal, TagIds.plates_cast_desh, 3);
        genericMod("DESH_DURA", TagIds.plates_gunmetal, TagIds.plates_cast_desh, 1);
        genericMod("WSTEEL_DAMAGE", TagIds.mechanisms_weaponsteel, TagIds.plates_cast_weaponsteel, 3);
        genericMod("WSTEEL_DURA", TagIds.plates_weaponsteel, TagIds.plates_cast_weaponsteel, 1);
        genericMod("FERRO_DAMAGE", TagIds.mechanisms_weaponsteel, TagIds.plates_cast_ferrouranium, 3);
        genericMod("FERRO_DURA", TagIds.plates_weaponsteel, TagIds.plates_cast_ferrouranium, 1);
        genericMod("TCALLOY_DAMAGE", TagIds.mechanisms_weaponsteel, TagIds.plates_cast_any_resistant_alloy, 3);
        genericMod("TCALLOY_DURA", TagIds.plates_weaponsteel, TagIds.plates_cast_any_resistant_alloy, 1);
        genericMod("BIGMT_DAMAGE", TagIds.mechanisms_saturnite, TagIds.plates_cast_saturnite, 3);
        genericMod("BIGMT_DURA", TagIds.plates_saturnite, TagIds.plates_cast_saturnite, 1);
        genericMod("BRONZE_DAMAGE", TagIds.mechanisms_saturnite, TagIds.plates_cast_any_bismoid_bronze, 3);
        genericMod("BRONZE_DURA", TagIds.plates_saturnite, TagIds.plates_cast_any_bismoid_bronze, 1);

        specialMod("SILENCER", rows("P", "B", "P"), k("P", TagIds.ingots_any_plastic), k("B", TagIds.barrels_light_steel));
        specialMod("SCOPE", rows("SPS", "G G", "SPS"), k("P", TagIds.ingots_any_plastic), k("S", TagIds.plates_steel),
                k("G", TagIds.glass_panes));
        specialMod("SAW", rows("BBS", "BHS"), k("B", TagIds.bolts_steel), k("S", VanillaIds.stick),
                k("H", TagIds.plates_dura_steel));
        specialMod("SPEEDLOADER", rows(" B ", "BSB", " B "), k("B", TagIds.bolts_steel), k("S", TagIds.plates_weaponsteel));
        specialMod("SLOWDOWN", rows(" I ", " M ", "I I"), k("I", TagIds.ingots_weaponsteel),
                k("M", TagIds.mechanisms_weaponsteel));
        specialMod("SPEEDUP", rows("PIP", "WWW", "PIP"), k("P", TagIds.plates_weaponsteel), k("I", TagIds.ingots_gunmetal),
                k("W", TagIds.wires_dense_gold));
        specialMod("GREASEGUN", rows("BRM", "P G"), k("B", TagIds.barrels_light_weaponsteel),
                k("R", TagIds.receivers_light_weaponsteel), k("M", TagIds.mechanisms_weaponsteel),
                k("P", TagIds.plates_dura_steel), k("G", TagIds.grips_any_plastic));
        specialMod("CHOKE", rows("P", "B", "P"), k("P", TagIds.plates_weaponsteel), k("B", TagIds.barrels_light_dura_steel));
        specialMod("FURNITURE_GREEN", rows("PDS", "  G"), k("P", TagIds.ingots_any_plastic), k("D", TagIds.dyes_green),
                k("S", TagIds.stocks_any_plastic), k("G", TagIds.grips_any_plastic));
        specialMod("FURNITURE_BLACK", rows("PDS", "  G"), k("P", TagIds.ingots_any_plastic), k("D", TagIds.dyes_black),
                k("S", TagIds.stocks_any_plastic), k("G", TagIds.grips_any_plastic));
        specialMod("SKIN_SATURNITE", rows("BRM", " P "), k("B", TagIds.barrels_light_saturnite),
                k("R", TagIds.receivers_light_saturnite), k("M", TagIds.mechanisms_saturnite),
                k("P", TagIds.plates_saturnite));
        specialMod("STACK_MAG", rows("P P", "P P", "PMP"), k("P", TagIds.plates_weaponsteel),
                k("M", TagIds.mechanisms_saturnite));
        specialMod("BAYONET", rows("  P", "BBB"), k("P", TagIds.plates_steel), k("B", TagIds.bolts_steel));
        specialMod("LAS_SHOTGUN", rows("PPP", "RCR", "PPP"), k("P", TagIds.ingots_any_hardplastic),
                k("R", ItemIds.crystal_redstone), k("C", ItemIds.circuit_advanced));
        specialMod("LAS_CAPACITOR", rows("CCC", "PIP"), k("C", ItemIds.circuit_capacitor_tantalium),
                k("P", TagIds.ingots_any_hardplastic), k("I", ItemIds.circuit_chip_bismoid));
        specialMod("LAS_AUTO", rows(" C ", "RFR", " C "), k("C", ItemIds.circuit_chip_bismoid),
                k("R", ItemIds.crystal_redstone), k("F", TagIds.receivers_heavy_any_bismoid_bronze));
        specialMod("DRILL_HSS", rows(" IP", "IIM", " IP"), k("I", TagIds.ingots_dura_steel),
                k("P", TagIds.ingots_any_plastic), k("M", TagIds.mechanisms_gunmetal));
        specialMod("DRILL_WEAPONSTEEL", rows(" IP", "IIM", " IP"), k("I", TagIds.ingots_weaponsteel),
                k("P", TagIds.ingots_rubber), k("M", TagIds.mechanisms_gunmetal));
        specialMod("DRILL_TCALLOY", rows(" IP", "IIM", " IP"), k("I", TagIds.ingots_any_resistant_alloy),
                k("P", TagIds.ingots_rubber), k("M", TagIds.mechanisms_weaponsteel));
        specialMod("DRILL_SATURNITE", rows(" IP", "IIM", " IP"), k("I", TagIds.ingots_saturnite),
                k("P", TagIds.ingots_any_hardplastic), k("M", TagIds.mechanisms_weaponsteel));
        specialMod("ENGINE_DIESEL", rows("DSD", "PPP", "DSD"), k("D", TagIds.plates_dura_steel),
                k("P", ItemIds.piston_selenium), k("S", TagIds.pipes_steel));
        specialMod("ENGINE_AVIATION", rows("DSD", "PPP", "DSD"), k("D", TagIds.plates_cast_dura_steel),
                k("P", ItemIds.piston_selenium), k("S", TagIds.mechanisms_gunmetal));
        // battery_pack CAPACITOR_GOLD is the port's capacitor_gold item
        specialMod("ENGINE_ELECTRIC", rows("DSD", "PPP", "DSD"), k("D", TagIds.ingots_any_plastic),
                k("P", TagIds.wires_dense_gold), k("S", ItemIds.capacitor_gold));
        specialMod("ENGINE_TURBO", rows("DSD", "PPP", "DSD"), k("D", TagIds.plates_cast_any_bismoid_bronze),
                k("P", ItemIds.piston_selenium), k("S", TagIds.mechanisms_weaponsteel));
        specialMod("MAGNET", rows("RGR", "GBG", "RGR"), k("R", TagIds.ingots_rubber), k("G", TagIds.wires_dense_gold),
                k("B", TagIds.storage_blocks_niobium));
        specialMod("SIFTER", rows("IGI", "IGI"), k("I", TagIds.ingots_dura_steel), k("G", ItemIds.steel_grate));
        specialMod("CANISTERS", rows(" R ", "CCC", "SSS"), k("R", TagIds.pipes_rubber), k("C", ItemIds.canister_empty),
                k("S", TagIds.plates_steel));
    }

    /** {@code GUN.x(), MAT.y() x n, ducttape}: n = 3 for the damage mods, 1 for the durability mods. */
    private static void genericMod(String name, String core, String material, int count) {
        Ingredient[] ingredients = new Ingredient[count + 2];
        ingredients[0] = i(core);
        for (int n = 1; n <= count; n++) ingredients[n] = i(material);
        ingredients[count + 1] = item(ItemIds.ducttape);
        componentShapeless(NS, "weapon_mod_generic_" + name.toLowerCase(java.util.Locale.ROOT), generic(name),
                ingredients);
    }

    private static void specialMod(String name, String[] pattern, Key... keys) {
        componentShaped(NS, "weapon_mod_special_" + name.toLowerCase(java.util.Locale.ROOT), specialStack(name), pattern,
                keys);
    }

    // ---- ToolRecipes ---------------------------------------------------------------------------------------------

    private static void tools() {
        // :145 blowtorch (1.7 getEmptyTool: an empty torch, the port's default stack)
        // (an absent fill reads as full on the port's torch, so the empty torch carries an explicit 0)
        componentShaped(NS, "blowtorch", stack(ItemIds.blowtorch).with(ComponentIds.fluid_amount, 0),
                rows("CC ", " I ", "CCC"), k("C", TagIds.plates_copper), k("I", TagIds.ingots_iron));

        // :158-169 minecarts. 1.7's cart ingredients are ItemStack(cart, EMPTY) whose chassis lives in NBT, and 1.7
        // recipes never compare NBT: every recipe below takes an empty cart of ANY chassis. The per-chassis loop
        // therefore registered identical grids and the first one wins - destroyer carts always come out steel,
        // powder and semtex carts always wood. Ported as 1.7 behaves (port the odd thing).
        componentShaped(NS, "cart_empty_wood", stack("hbm:cart_empty").with(ComponentIds.cart_base, "wood"),
                rows("P P", "WPW"), k("P", TagIds.minecraft_wooden_slabs), k("W", TagIds.minecraft_planks));
        componentShaped(NS, "cart_empty_steel", stack("hbm:cart_empty").with(ComponentIds.cart_base, "steel"),
                rows("P P", "IPI"), k("P", TagIds.plates_steel), k("I", TagIds.ingots_steel));
        componentShapeless(NS, "cart_empty_painted", stack("hbm:cart_empty").with(ComponentIds.cart_base, "painted"),
                item("hbm:cart_empty"), tag(TagIds.dyes_red));
        componentShaped(NS, "cart_destroyer", stack("hbm:cart_destroyer").with(ComponentIds.cart_base, "steel"),
                rows("S S", "BLB", "SCS"), k("S", TagIds.ingots_steel), k("B", ItemIds.blades_steel),
                key("L", fluid("LAVA", 1000)), k("C", "hbm:cart_empty"));
        componentShaped(NS, "cart_powder", stack("hbm:cart_powder").with(ComponentIds.cart_base, "wood"),
                rows("PPP", "PCP", "PPP"), k("P", VanillaIds.gunpowder), k("C", "hbm:cart_empty"));
        componentShaped(NS, "cart_semtex", stack("hbm:cart_semtex").with(ComponentIds.cart_base, "wood"),
                rows("S", "C"), k("S", ItemIds.semtex), k("C", "hbm:cart_empty"));
        // :169 the crate cart sits on a vanilla minecart and has no chassis tag (VANILLA)
        shaped(NS, "cart_crate", "hbm:cart_crate", rows("C", "S"), "C", ItemIds.crate_steel, "S", VanillaIds.minecart);
    }

    // ---- CraftingManager:90-91, the BlockMultiSlab loop: the two slabs the port lacked ----------------------------

    private static void slabs() {
        componentShaped(NS, "lightstone_tile_slab", stack(ItemIds.lightstone_tile_slab, 6), rows("###"),
                key("#", item("hbm:lightstone_tile")));
        componentShaped(NS, "lightstone_bricks_slab", stack(ItemIds.lightstone_bricks_slab, 6), rows("###"),
                key("#", item("hbm:lightstone_bricks")));
        shaped(NS, "brick_ducrete_slab", ItemIds.brick_ducrete_slab, 6, rows("###"), "#", ItemIds.brick_ducrete);
    }

    // ---- SmeltingRecipes -----------------------------------------------------------------------------------------

    private static void smelting() {
        smelt(NS, "glass_uranium", ItemIds.sand_uranium, ItemIds.glass_uranium, 0.25);              // :128
        smelt(NS, "glass_polonium", ItemIds.sand_polonium, ItemIds.glass_polonium, 0.75);           // :129
        smelt(NS, "ingot_polymer_from_scrap_plastic", "#hbm:scrap_plastic", ItemIds.ingot_polymer, 0.1); // :173, any meta
    }

    // ---- 1.7 IRecipe handlers, and recipes whose result is stack state -------------------------------------------

    private static void handlers() {
        special(NS, "grenade_universal", "hbm:grenade_universal");        // GrenadeCraftingHandler
        special(NS, "ammo_arty_cargo", "hbm:ammo_arty_cargo");            // CargoShellCraftingHandler
        special(NS, "scraps_split", "hbm:scraps_split");                  // ScrapsCraftingHandler
        special(NS, "syringe_mkunicorn", "hbm:syringe_mkunicorn");        // MKUCraftingHandler
        special(NS, "fluid_duct_retype", "hbm:fluid_duct_retype");        // CraftingManager:643-661
        special(NS, "scraps_from_powders", "hbm:scraps_from_powders");    // PowderRecipes:61-65
        special(NS, "zirnox_waste", "hbm:zirnox_waste");                  // RodRecipes:39-47
    }
}
