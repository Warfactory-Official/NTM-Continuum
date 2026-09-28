package com.hbm.items;

import com.hbm.lib.ItemIds;
import com.hbm.lib.TagIds;
import com.hbm.lib.VanillaIds;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Tags;
import dev.golem.core.dejson.Tints;

import static dev.golem.core.dejson.Crafting.rows;
import static dev.golem.core.dejson.Crafting.shaped;
import static dev.golem.core.dejson.Crafting.shapeless;
import static dev.golem.core.dejson.Crafting.stack;

/**
 * Dejson declarations for {@link ModMeleeKitItems}: the item models (1.7 texture names; everything 1.7 drew
 * {@code setFull3D} is handheld), the family's repair and sword tags, and the crafting recipes of 1.7
 * {@code CraftingManager}, {@code ToolRecipes}, {@code WeaponRecipes} and {@code ConsumableRecipes}, including the
 * missile assembly station's, which waited on the wrench. The Book of Boxcars recipes are
 * {@code MagicRecipes}, not data. Touches no Minecraft class.
 */
public final class MeleeKitDeclarations {

    private static final String NS = "hbm";
    private static boolean declared;

    private MeleeKitDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        // --- handheld (ItemSword, ItemTooling, setFull3D) ---
        ItemRendererDeclarations.heldModel("big_sword", "big_sword", "big_sword"); // 1.7 ItemRenderBigSword
        handheld("schrabidium_hammer", "schrabidium_hammer");
        ItemRendererDeclarations.heldModel("shimmer_sledge", "shimmer_sledge_original", "shim"); // 1.7 ItemRenderShim
        ItemRendererDeclarations.heldModel("shimmer_axe", "shimmer_axe", "shim");
        handheld("wrench_flipped", "wrench_flipped");
        handheld("memespoon", "memespoon");
        ItemRendererDeclarations.heldModel("wood_gavel", "wood_gavel", "gavel"); // 1.7 ItemRenderGavel
        ItemRendererDeclarations.heldModel("lead_gavel", "lead_gavel", "gavel");
        ItemRendererDeclarations.heldModel("diamond_gavel", "diamond_gavel", "gavel");
        // 1.7's lead-pipe weapon icon is textures/items/pipe_lead.png, but hbm:items/pipe_lead is the computed
        // autogen sprite of the lead pipe material item; a file there would replace that sprite, so the
        // weapon's copy of the original art is shipped under its own item name.
        handheld("weapon_pipe_lead", "weapon_pipe_lead");
        handheld("reer_graar", "reer_graar_hd");
        ItemRendererDeclarations.heldModel("stopsign", "stopsign", "shim");
        ItemRendererDeclarations.heldModel("sopsign", "sopsign", "shim");
        ItemRendererDeclarations.heldModel("chernobylsign", "chernobylsign", "shim");
        handheld("ullapool_caber", "ullapool_caber");
        handheld("wrench", "wrench");
        handheld("wrench_archineer", "wrench_archineer_hd");
        handheld("screwdriver_desh", "screwdriver_desh");
        handheld("hand_drill_desh", "hand_drill_desh");
        handheld("acetylene_torch", "acetylene_torch");
        handheld("rod_of_discord", "rod_of_discord");
        handheld("mysteryshovel", "cursed_shovel");

        // --- flat ---
        flat("weaponized_starblaster_cell", "gun_b92_ammo_weaponized");
        flat("stick_dynamite_fishing", "stick_dynamite_fishing");
        flat("book_of_", "book_of_");
        for (String kit : new String[]{"stealth_boy", "nuke_starter_kit", "nuke_advanced_kit", "nuke_commercially_kit",
                "nuke_electric_kit", "gadget_kit", "boy_kit", "man_kit", "mike_kit", "tsar_kit", "multi_kit", "custom_kit",
                "fleija_kit", "prototype_kit", "missile_kit", "solinium_kit", "hazmat_kit", "hazmat_red_kit",
                "hazmat_grey_kit", "loot_10", "loot_15", "loot_misc", "bobmazon"}) {
            flat(kit, kit);
        }
        flat("euphemium_kit", "code");
        flat("bobmazon_hidden", "bobmazon_special");

        // 1.7 ItemKitCustom: the kit, then kit_1 and kit_2 tinted by the stack's color1 and color2 (black if unset)
        Dejson.item(NS, "kit_custom").models(Models.generated("hbm:items/kit", "hbm:items/kit_1", "hbm:items/kit_2"))
                .tints(Tints.constant(-1), Tints.customModelData(0, 0), Tints.customModelData(1, 0)).register();

        // 1.7 ItemToolBox: pass 1 is kit_toolbox, or its empty lid kit_toolbox_empty while open (item_variant 1)
        Dejson.item(NS, "toolbox").models(Models.group()
                        .and("", Models.generated("hbm:items/kit_toolbox"))
                        .and("_open", Models.generated("hbm:items/kit_toolbox", "hbm:items/kit_toolbox_empty")))
                .selects(NS + ":item_variant").register();

        // --- tags ---
        Tags.item(NS, "repairs/diamond", VanillaIds.diamond);
        Tags.item(NS, "repairs/block_schrabidium", ItemIds.block_schrabidium);
        // nested into minecraft:swords by AbilityToolDeclarations, which owns that file
        Tags.item(NS, "melee_kit_swords", "hbm:big_sword", "hbm:schrabidium_hammer", "hbm:shimmer_sledge",
                "hbm:shimmer_axe", "hbm:wrench_flipped", "hbm:memespoon", "hbm:wood_gavel", "hbm:lead_gavel",
                "hbm:diamond_gavel", "hbm:weapon_pipe_lead", "hbm:reer_graar", "hbm:stopsign", "hbm:sopsign",
                "hbm:chernobylsign", "hbm:ullapool_caber", "hbm:wrench");

        recipes();
    }

    private static void handheld(String id, String texture) {
        Dejson.item(NS, id).models(Models.handheld("hbm:items/" + texture)).register();
    }

    private static void flat(String id, String texture) {
        Dejson.item(NS, id).models(Models.generated("hbm:items/" + texture)).register();
    }

    private static void recipes() {
        String stick = VanillaIds.stick;
        String plastic = TagIds.ingots_any_plastic;

        // 1.7 CraftingManager
        shaped(NS, "big_sword", "hbm:big_sword", rows("QIQ", "QIQ", "GSG"),
                "G", VanillaIds.gold_ingot, "S", stick, "I", VanillaIds.iron_ingot, "Q", VanillaIds.quartz);
        shaped(NS, "wrench", "hbm:wrench", rows(" S ", " IS", "I  "),
                "S", TagIds.ingots_steel, "I", TagIds.ingots_iron);
        shaped(NS, "wrench_flipped", "hbm:wrench_flipped", rows("S", "D", "W"),
                "S", VanillaIds.iron_sword, "D", ItemIds.ducttape, "W", "hbm:wrench");
        shaped(NS, "machine_missile_assembly", "hbm:machine_missile_assembly", rows("PWP", "SSS", "CCC"),
                "P", ItemIds.pedestal_steel, "W", "hbm:wrench", "S", TagIds.plates_steel, "C", ItemIds.steel_scaffold);

        // 1.7 ToolRecipes
        shaped(NS, "wood_gavel", "hbm:wood_gavel", rows("SWS", " R ", " R "),
                "S", TagIds.minecraft_wooden_slabs, "W", TagIds.minecraft_logs, "R", stick);
        shaped(NS, "lead_gavel", "hbm:lead_gavel", rows("PIP", "IGI", "PIP"),
                "P", ItemIds.pellet_buckshot, "I", TagIds.ingots_lead, "G", "hbm:wood_gavel");
        shaped(NS, "weapon_pipe_lead", "hbm:weapon_pipe_lead", rows("II", " I", " I"), "I", TagIds.pipes_lead);
        shaped(NS, "ullapool_caber", "hbm:ullapool_caber", rows("ITI", " S ", " S "),
                "I", TagIds.plates_iron, "T", VanillaIds.tnt, "S", stick);
        shaped(NS, "toolbox", "hbm:toolbox", rows("CCC", "CIC"), "C", TagIds.plates_copper, "I", TagIds.ingots_iron);
        shaped(NS, "screwdriver_desh", "hbm:screwdriver_desh", rows("  I", " I ", "S  "),
                "S", plastic, "I", TagIds.ingots_desh);
        shaped(NS, "hand_drill_desh", "hbm:hand_drill_desh", rows(" D", "S ", " S"),
                "D", TagIds.ingots_desh, "S", plastic);
        // ItemBlowtorch.getEmptyTool: the torch is crafted with both tanks written empty
        shaped(NS, "acetylene_torch", stack("hbm:acetylene_torch").with("hbm:torch_unsaturateds", 0)
                        .with("hbm:torch_oxygen", 0), rows("SS ", " PS", " T "),
                "S", TagIds.plates_steel, "P", plastic, "T", ItemIds.tank_steel);
        shapeless(NS, "bobmazon", "hbm:bobmazon", VanillaIds.book, VanillaIds.gold_nugget, VanillaIds.string,
                TagIds.dyes_blue);

        // 1.7 WeaponRecipes: the chemthrower waited on the wrench too
        shaped(NS, "gun_chemthrower", "hbm:gun_chemthrower", rows("MHW", "PSS"),
                "M", TagIds.mechanisms_weaponsteel, "H", TagIds.pipes_rubber, "W", "hbm:wrench",
                "P", TagIds.barrels_heavy_weaponsteel, "S", TagIds.shells_weaponsteel);
        // 1.7 WeaponRecipes: three sticks of dynamite, paper and any tar
        shapeless(NS, "stick_dynamite_fishing", "hbm:stick_dynamite_fishing", ItemIds.stick_dynamite,
                ItemIds.stick_dynamite, ItemIds.stick_dynamite, VanillaIds.paper, TagIds.tars);

        // 1.7 ConsumableRecipes
        shaped(NS, "stealth_boy", "hbm:stealth_boy", rows(" B", "LI", "LC"),
                "B", VanillaIds.stone_button, "L", VanillaIds.leather, "I", TagIds.ingots_steel,
                "C", ItemIds.circuit_basic);
    }
}
