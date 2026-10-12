package com.hbm.inventory.recipes;

import com.hbm.lib.ComponentIds;
import com.hbm.lib.ItemIds;
import com.hbm.lib.TagIds;
import com.hbm.lib.VanillaIds;

import static dev.golem.core.dejson.Crafting.componentShapeless;
import static dev.golem.core.dejson.Crafting.item;
import static dev.golem.core.dejson.Crafting.requires;
import static dev.golem.core.dejson.Crafting.rows;
import static dev.golem.core.dejson.Crafting.shaped;
import static dev.golem.core.dejson.Crafting.shapeless;
import static dev.golem.core.dejson.Crafting.stack;
import static dev.golem.core.dejson.Crafting.tag;

/**
 * The crafting recipes 1.7 registered only under a config branch: {@code GeneralConfig.enableLBSM &&
 * enableLBSMSimple*} (Less Bullshit Mode, off by default) and {@code enable528} (off by default). Each file carries
 * {@code "golem:requires"} (HBM's {@code RecipeConditions} flags as Golem Core recipe flags), so it is in the game only
 * in that mode; the default side of each branch (1.7's {@code else} / {@code if(!enable528)}) is declared where the
 * recipe always was, with the negated flag. In 1.7 order; each block names its source lines. Ids are 1.7-less (1.7
 * recipes had none): the result's name and {@code _lbsm} / {@code _528}.
 */
public final class ConfigCraftingRecipes {

    private static final String NS = "hbm";
    private static boolean declared;

    /** 1.7 {@code GeneralConfig.enableLBSM && enableLBSMSimpleCrafting} (CraftingManager, PowderRecipes). */
    private static final String CRAFTING = "lbsm_simple_crafting";
    /** 1.7 {@code GeneralConfig.enableLBSM && enableLBSMSimpleArmorRecipes} (ArmorRecipes). */
    private static final String ARMOR = "lbsm_simple_armor";
    /** 1.7 {@code GeneralConfig.enableLBSM && enableLBSMSimpleToolRecipes} (ToolRecipes). */
    private static final String TOOL = "lbsm_simple_tool";
    /** 1.7 {@code GeneralConfig.enableLBSM && enableLBSMSimpleMedicineRecipes} (ConsumableRecipes). */
    private static final String MEDICINE = "lbsm_simple_medicine";
    /** 1.7 {@code GeneralConfig.enable528}. */
    private static final String M528 = "enable_528";

    private ConfigCraftingRecipes() {}

    public static void declare() {
        if (declared) return;
        declared = true;
        craftingManager528();
        craftingManagerLbsm();
        armor();
        consumables();
        powders();
        tools();
    }

    // 1.7 CraftingManager:758-768, the else side of if(!GeneralConfig.enable528) (the default side is rbmk_control,
    // rbmk_control_mod and rbmk_control_auto in ModShapedRecipes)
    private static void craftingManager528() {
        requires(shaped(NS, "rbmk_control_reasim_528", ItemIds.rbmk_control_reasim, rows(" B ", "GRG", " B "),
                "G", TagIds.ingots_graphite, "B", ItemIds.motor, "R", ItemIds.rbmk_absorber), M528);
        // 1.7 as written: the ReaSim auto rod takes the plain rbmk_control, which has no recipe in 528 mode
        requires(shaped(NS, "rbmk_control_reasim_auto_528", ItemIds.rbmk_control_reasim_auto, rows("C", "R", "D"),
                "C", ItemIds.circuit_advanced, "R", ItemIds.rbmk_control, "D", ItemIds.crt_display), M528);
    }

    // 1.7 CraftingManager:945-975, if(GeneralConfig.enableLBSM && GeneralConfig.enableLBSMSimpleCrafting)
    private static void craftingManagerLbsm() {
        // 1.7 Blocks.wool at OreDictionary.WILDCARD_VALUE: any wool
        requires(shapeless(NS, "cordite_lbsm", ItemIds.cordite, 3,
                ItemIds.ballistite, VanillaIds.gunpowder, TagIds.minecraft_wool), CRAFTING);
        requires(shapeless(NS, "ingot_semtex_lbsm", ItemIds.ingot_semtex, 3,
                VanillaIds.slime_ball, VanillaIds.tnt, TagIds.dusts_niter), CRAFTING);
        // 1.7 canister_full metadata = fluid id; the port's canister carries the fluid by name
        requires(componentShapeless(NS, "canister_full_diesel_lbsm", stack(ItemIds.canister_full)
                        .with(ComponentIds.fluid, "hbm:diesel").with(ComponentIds.fluid_amount, 1_000),
                item(ItemIds.canister_full).with(ComponentIds.fluid, "hbm:oil"), tag(TagIds.dusts_redstone),
                item(ItemIds.canister_empty)), CRAFTING);

        requires(shapeless(NS, "ore_uranium_lbsm", ItemIds.ore_uranium,
                ItemIds.ore_uranium_scorched, VanillaIds.water_bucket), CRAFTING);
        requires(shaped(NS, "ore_uranium_8_lbsm", ItemIds.ore_uranium, 8, rows("OOO", "OBO", "OOO"),
                "O", ItemIds.ore_uranium_scorched, "B", VanillaIds.water_bucket), CRAFTING);
        requires(shapeless(NS, "ore_nether_uranium_lbsm", ItemIds.ore_nether_uranium,
                ItemIds.ore_nether_uranium_scorched, VanillaIds.water_bucket), CRAFTING);
        requires(shaped(NS, "ore_nether_uranium_8_lbsm", ItemIds.ore_nether_uranium, 8, rows("OOO", "OBO", "OOO"),
                "O", ItemIds.ore_nether_uranium_scorched, "B", VanillaIds.water_bucket), CRAFTING);
        requires(shapeless(NS, "ore_gneiss_uranium_lbsm", ItemIds.ore_gneiss_uranium,
                ItemIds.ore_gneiss_uranium_scorched, VanillaIds.water_bucket), CRAFTING);
        requires(shaped(NS, "ore_gneiss_uranium_8_lbsm", ItemIds.ore_gneiss_uranium, 8, rows("OOO", "OBO", "OOO"),
                "O", ItemIds.ore_gneiss_uranium_scorched, "B", VanillaIds.water_bucket), CRAFTING);
        // 1.7: scorched Sellafield uranium washes into the ordinary uranium ore
        requires(shapeless(NS, "ore_uranium_from_sellafield_lbsm", ItemIds.ore_uranium,
                ItemIds.ore_sellafield_uranium_scorched, VanillaIds.water_bucket), CRAFTING);
        requires(shaped(NS, "ore_uranium_8_from_sellafield_lbsm", ItemIds.ore_uranium, 8, rows("OOO", "OBO", "OOO"),
                "O", ItemIds.ore_sellafield_uranium_scorched, "B", VanillaIds.water_bucket), CRAFTING);

        plate("plate_iron", ItemIds.plate_iron, TagIds.ingots_iron);
        plate("plate_gold", ItemIds.plate_gold, TagIds.ingots_gold);
        plate("plate_aluminium", ItemIds.plate_aluminium, TagIds.ingots_aluminium);
        plate("plate_titanium", ItemIds.plate_titanium, TagIds.ingots_titanium);
        plate("plate_copper", ItemIds.plate_copper, TagIds.ingots_copper);
        plate("plate_lead", ItemIds.plate_lead, TagIds.ingots_lead);
        plate("plate_steel", ItemIds.plate_steel, TagIds.ingots_steel);
        plate("plate_schrabidium", ItemIds.plate_schrabidium, TagIds.ingots_schrabidium);
        plate("plate_saturnite", ItemIds.plate_saturnite, TagIds.ingots_saturnite);
        plate("plate_combine_steel", ItemIds.plate_combine_steel, TagIds.ingots_combine_steel);
        plate("neutron_reflector", ItemIds.neutron_reflector, TagIds.ingots_tungsten);

        // 1.7 for(NTMMaterial mat : Mats.orderedList) if(mat.autogen.contains(WIRE)) for(name : mat.names):
        // wire_fine x24 from "###" of ingot<name>. Mats declaration order; one recipe per material (every 1.7 name of
        // a material is the same c: tag here).
        wire("wire_fine_carbon", ItemIds.wire_fine_carbon, TagIds.ingots_carbon);
        wire("wire_fine_gold", ItemIds.wire_fine_gold, TagIds.ingots_gold);
        wire("wire_fine_schrabidium", ItemIds.wire_fine_schrabidium, TagIds.ingots_schrabidium);
        wire("wire_fine_copper", ItemIds.wire_fine_copper, TagIds.ingots_copper);
        wire("wire_fine_tungsten", ItemIds.wire_fine_tungsten, TagIds.ingots_tungsten);
        wire("wire_fine_aluminium", ItemIds.wire_fine_aluminium, TagIds.ingots_aluminium);
        wire("wire_fine_lead", ItemIds.wire_fine_lead, TagIds.ingots_lead);
        wire("wire_fine_zirconium", ItemIds.wire_fine_zirconium, TagIds.ingots_zirconium);
        wire("wire_fine_steel", ItemIds.wire_fine_steel, TagIds.ingots_steel);
        wire("wire_fine_red_copper", ItemIds.wire_fine_red_copper, TagIds.ingots_red_copper);
        wire("wire_fine_magnetized_tungsten", ItemIds.wire_fine_magnetized_tungsten, TagIds.ingots_magnetized_tungsten);

        requires(shaped(NS, "book_of__lbsm", ItemIds.book_of_, rows("BGB", "GAG", "BGB"),
                "B", ItemIds.egg_balefire_shard, "G", TagIds.ingots_gold, "A", VanillaIds.book), CRAFTING);
    }

    // 1.7 ArmorRecipes:169-177, if(GeneralConfig.enableLBSM && GeneralConfig.enableLBSMSimpleArmorRecipes); the else
    // side (the cobalt/starmetal upgrade recipes) is in ModShapedRecipes with !lbsm_simple_armor
    private static void armor() {
        armorSet("starmetal", TagIds.ingots_starmetal);
        armorSet("schrabidium", TagIds.ingots_schrabidium);
    }

    // 1.7 ConsumableRecipes:126-129, if(GeneralConfig.enableLBSM && GeneralConfig.enableLBSMSimpleMedicineRecipes); the
    // else side is in RestoredCraftingRecipes with !lbsm_simple_medicine
    private static void consumables() {
        requires(shapeless(NS, "siox_lbsm", ItemIds.siox, 8,
                TagIds.dusts_coal, TagIds.dusts_asbestos, TagIds.nuggets_gold), MEDICINE);
        // 1.7 NETHERQUARTZ.dust(): the port's quartz dust tag, as in its other 1.7 recipes
        requires(shapeless(NS, "xanax_lbsm", ItemIds.xanax,
                TagIds.dusts_coal, TagIds.dusts_niter, TagIds.dusts_quartz), MEDICINE);
    }

    // 1.7 PowderRecipes:78-85, if(GeneralConfig.enableLBSM && GeneralConfig.enableLBSMSimpleCrafting)
    private static void powders() {
        requires(shapeless(NS, "powder_red_copper_lbsm", ItemIds.powder_red_copper, 2,
                TagIds.dusts_redstone, TagIds.dusts_copper), CRAFTING);
        requires(shapeless(NS, "powder_dura_steel_lbsm", ItemIds.powder_dura_steel, 2,
                TagIds.dusts_steel, TagIds.dusts_tungsten), CRAFTING);
        requires(shapeless(NS, "powder_dura_steel_1_lbsm", ItemIds.powder_dura_steel, 2,
                TagIds.dusts_steel, TagIds.dusts_cobalt), CRAFTING);
        requires(shapeless(NS, "powder_dura_steel_2_lbsm", ItemIds.powder_dura_steel, 4,
                TagIds.dusts_iron, TagIds.dusts_coal, TagIds.dusts_tungsten, TagIds.dusts_tungsten), CRAFTING);
        requires(shapeless(NS, "powder_dura_steel_3_lbsm", ItemIds.powder_dura_steel, 4,
                TagIds.dusts_iron, TagIds.dusts_coal, TagIds.dusts_cobalt, TagIds.dusts_cobalt), CRAFTING);
        // 1.7 Items.brick and Items.netherbrick (the items)
        requires(shaped(NS, "ingot_firebrick_lbsm", ItemIds.ingot_firebrick, 4, rows("BN", "NB"),
                "B", VanillaIds.brick, "N", VanillaIds.nether_brick), CRAFTING);
    }

    // 1.7 ToolRecipes:172-187, if(GeneralConfig.enableLBSM && GeneralConfig.enableLBSMSimpleToolRecipes); the else side
    // (the upgrade recipes) is in AbilityToolDeclarations with !lbsm_simple_tool
    private static void tools() {
        toolSet("cobalt_decorated", TagIds.storage_blocks_cobalt);
        toolSet("starmetal", TagIds.ingots_starmetal);
        toolSet("schrabidium", TagIds.ingots_schrabidium);
    }

    // ---- 1.7 helpers -----------------------------------------------------------------------------------------------

    /** 1.7 {@code addRecipeAuto(new ItemStack(plate, 4), "##", "##", '#', ingot)}. */
    private static void plate(String name, String result, String ingot) {
        requires(shaped(NS, name + "_lbsm", result, 4, rows("##", "##"), "#", ingot), CRAFTING);
    }

    private static void wire(String name, String result, String ingot) {
        requires(shaped(NS, name + "_lbsm", result, 24, rows("###"), "#", ingot), CRAFTING);
    }

    /** 1.7 {@code ArmorRecipes.addHelmet/addChest/addLegs/addBoots}: X the material. */
    private static void armorSet(String set, String ingot) {
        requires(shaped(NS, set + "_helmet_lbsm", "hbm:" + set + "_helmet", rows("XXX", "X X"), "X", ingot), ARMOR);
        requires(shaped(NS, set + "_plate_lbsm", "hbm:" + set + "_plate", rows("X X", "XXX", "XXX"), "X", ingot), ARMOR);
        requires(shaped(NS, set + "_legs_lbsm", "hbm:" + set + "_legs", rows("XXX", "X X", "X X"), "X", ingot), ARMOR);
        requires(shaped(NS, set + "_boots_lbsm", "hbm:" + set + "_boots", rows("X X", "X X"), "X", ingot), ARMOR);
    }

    /** 1.7 {@code ToolRecipes.addSword/addPickaxe/addAxe/addShovel/addHoe}: X the material, # a stick. */
    private static void toolSet(String set, String ingot) {
        String stick = VanillaIds.stick;
        requires(shaped(NS, set + "_sword_lbsm", "hbm:" + set + "_sword", rows("X", "X", "#"),
                "X", ingot, "#", stick), TOOL);
        requires(shaped(NS, set + "_pickaxe_lbsm", "hbm:" + set + "_pickaxe", rows("XXX", " # ", " # "),
                "X", ingot, "#", stick), TOOL);
        requires(shaped(NS, set + "_axe_lbsm", "hbm:" + set + "_axe", rows("XX", "X#", " #"),
                "X", ingot, "#", stick), TOOL);
        requires(shaped(NS, set + "_shovel_lbsm", "hbm:" + set + "_shovel", rows("X", "#", "#"),
                "X", ingot, "#", stick), TOOL);
        requires(shaped(NS, set + "_hoe_lbsm", "hbm:" + set + "_hoe", rows("XX", " #", " #"),
                "X", ingot, "#", stick), TOOL);
    }
}
