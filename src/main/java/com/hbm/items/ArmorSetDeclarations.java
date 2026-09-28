package com.hbm.items;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.lib.ItemIds;
import com.hbm.lib.TagIds;
import com.hbm.lib.VanillaIds;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Tags;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.golem.core.dejson.Crafting.rows;
import static dev.golem.core.dejson.Crafting.shaped;
import static dev.golem.core.dejson.Crafting.shapeless;

/**
 * Dejson declarations for {@link ModArmorSetItems}: item models, repair tags and 1.7 {@code ArmorRecipes}.
 * Touches no Minecraft class.
 *
 * <p>The plain sets draw their 1.7 sprite. The six modelled sets drew their inventory icon through
 * {@code ArmorFSB.renderStandard} over the worn OBJ (an {@code IItemRendererProvider}); those are
 * {@code hbm:machine_obj} models on the {@code armor_<set>_<slot>} path of {@code MachineItemModel}, which
 * replays {@code setupRenderInv}/{@code setupRenderNonInv} and the per-slot scale, with each class's own
 * pre-transform keyed on the set name. Part lists and texture bindings are each class's
 * {@code renderStandard} arguments; legs and boots are drawn with culling off, as renderStandard did.
 */
public final class ArmorSetDeclarations {

    private static final String NS = "hbm";
    private static boolean declared;

    private ArmorSetDeclarations() {}

    static final String[] SPRITES = {
            "alloy_helmet", "alloy_plate", "alloy_legs", "alloy_boots",
            "cmb_helmet", "cmb_plate", "cmb_legs", "cmb_boots",
            "dnt_helmet", "dnt_plate", "dnt_legs", "dnt_boots",
            "euphemium_helmet", "euphemium_plate", "euphemium_legs", "euphemium_boots",
            "zirconium_legs",
    };

    public static void declare() {
        if (declared) return;
        declared = true;

        for (String id : SPRITES) Dejson.item(NS, id).models(Models.generated("hbm:items/" + id)).register();

        // renderStandard(model, armorType, helmetTex, chestTex, armTex, legTex, helmet, chest, lArm, rArm, lLeg, rLeg, lBoot, rBoot)
        modelled("ajr", "models/armor/ajr.obj", "ajr_helmet", "ajr_chest", "ajr_arm", "ajr_leg",
                List.of("Head"), List.of("Body"), "LeftBoot", "RightBoot", false);
        modelled("ajro", "models/armor/ajr.obj", "ajro_helmet", "ajro_chest", "ajro_arm", "ajro_leg",
                List.of("Head"), List.of("Body"), "LeftBoot", "RightBoot", false);
        modelled("hev", "models/armor/hev.obj", "hev_helmet", "hev_chest", "hev_arm", "hev_leg",
                List.of("Head"), List.of("Body"), "LeftFoot", "RightFoot", false);
        modelled("fau", "models/armor/fau.obj", "fau_helmet", "fau_chest", "fau_arm", "fau_leg",
                List.of("Head"), List.of("Body", "Cassette"), "LeftBoot", "RightBoot", false);
        // ArmorBismuth binds the one bismuth texture for every slot and disables culling around renderStandard.
        modelled("bismuth", "models/armor/bismuth.obj", "bismuth", "bismuth", "bismuth", "bismuth",
                List.of("Head"), List.of("Body"), "LeftFoot", "RightFoot", true);
        modelled("trenchmaster", "models/armor/trenchmaster.obj", "trenchmaster_helmet", "trenchmaster_chest",
                "trenchmaster_arm", "trenchmaster_leg", List.of("Helmet", "Light"), List.of("Chest"),
                "LeftBoot", "RightBoot", false);

        // 1.7 customCraftingMaterial of each material.
        // aMatAlloy never set a customCraftingMaterial: nothing repairs it, so repairs/alloy stays undeclared
        // (Dejson rejects an empty tag, and an absent tag already matches nothing).
        // repairs/cmb (combine steel) is already declared with the CMB tools.
        Tags.item(NS, "repairs/bismuth_armor", ItemIds.plate_bismuth);
        Tags.item(NS, "repairs/euphemium", ItemIds.plate_euphemium);
        Tags.item(NS, "repairs/zirconium", ItemIds.ingot_zirconium);
        Tags.item(NS, "repairs/dnt", ItemIds.ingot_dineutronium);
        Tags.item(NS, "repairs/ajr", ItemIds.plate_armor_ajr);
        Tags.item(NS, "repairs/hev", ItemIds.plate_armor_hev);
        Tags.item(NS, "repairs/fau", ItemIds.plate_armor_fau);

        recipes();
    }

    /** One set's four icons over its worn OBJ. */
    private static void modelled(String set, String obj, String helmetTex, String chestTex, String armTex,
                                 String legTex, List<String> helmet, List<String> chest, String leftBoot,
                                 String rightBoot, boolean noCullAll) {
        String objId = NS + ":" + obj;
        List<String> all = switch (set) {
            case "ajr", "ajro" -> List.of("Head", "Body", "LeftArm", "RightArm", "LeftLeg", "RightLeg",
                    "LeftBoot", "RightBoot", "RocketBox");
            case "fau" -> List.of("Head", "Body", "Cassette", "LeftArm", "RightArm", "LeftLeg", "RightLeg",
                    "LeftBoot", "RightBoot");
            case "trenchmaster" -> List.of("Helmet", "Light", "Chest", "LeftArm", "RightArm", "LeftLeg",
                    "RightLeg", "LeftBoot", "RightBoot");
            default -> List.of("Head", "Body", "LeftArm", "RightArm", "LeftLeg", "RightLeg", "LeftFoot",
                    "RightFoot");
        };

        Map<String, String> head = new LinkedHashMap<>();
        for (String part : helmet) head.put(part, helmetTex);
        Map<String, String> body = new LinkedHashMap<>();
        for (String part : chest) body.put(part, chestTex);
        body.put("LeftArm", armTex);
        body.put("RightArm", armTex);
        Map<String, String> legs = new LinkedHashMap<>();
        legs.put("LeftLeg", legTex);
        legs.put("RightLeg", legTex);
        Map<String, String> boots = new LinkedHashMap<>();
        boots.put(leftBoot, legTex);
        boots.put(rightBoot, legTex);

        piece(set + "_helmet", set, "head", objId, helmetTex, head, all, noCullAll);
        piece(set + "_plate", set, "chest", objId, chestTex, body, all, noCullAll);
        piece(set + "_legs", set, "legs", objId, legTex, legs, all, true);
        piece(set + "_boots", set, "feet", objId, legTex, boots, all, true);
    }

    private static void piece(String item, String set, String slot, String obj, String texture,
                              Map<String, String> parts, List<String> all, boolean noCull) {
        JsonObject model = new JsonObject();
        model.addProperty("type", "hbm:machine_obj");
        model.addProperty("obj", obj);
        model.addProperty("texture", tex(texture));
        model.addProperty("noCull", noCull);
        model.addProperty("animation", "armor_" + set + "_" + slot);
        JsonObject textures = new JsonObject();
        parts.forEach((part, tex) -> textures.addProperty(part, tex(tex)));
        model.add("partTextures", textures);
        JsonArray exclude = new JsonArray();
        for (String part : all) if (!parts.containsKey(part)) exclude.add(part);
        model.add("exclude", exclude);

        // The model is the definition's own model (not wrapped in minecraft:special): that is the
        // context-aware path, where the armor_ animation replays setupRenderInv / setupRenderNonInv.
        Dejson.item(NS, item).shares("hbm:item/machine_obj_base").register();
        Dejson.itemDefinition("hbm:item/" + item).add("model", model);
    }

    private static String tex(String name) {
        return NS + ":textures/armor/" + name + ".png";
    }

    /** 1.7 {@code ArmorRecipes.register}, this family's lines in source order. The alloy set has none. */
    private static void recipes() {
        // addHelmet/addChest/addLegs/addBoots: the vanilla armour patterns
        shaped(NS, "cmb_helmet", "hbm:cmb_helmet", rows("EEE", "E E"), "E", TagIds.ingots_combine_steel);
        shaped(NS, "cmb_plate", "hbm:cmb_plate", rows("E E", "EEE", "EEE"), "E", TagIds.ingots_combine_steel);
        shaped(NS, "cmb_legs", "hbm:cmb_legs", rows("EEE", "E E", "E E"), "E", TagIds.ingots_combine_steel);
        shaped(NS, "cmb_boots", "hbm:cmb_boots", rows("E E", "E E"), "E", TagIds.ingots_combine_steel);

        shaped(NS, "dnt_helmet", "hbm:dnt_helmet", rows("EEE", "EE "), "E", TagIds.ingots_dineutronium);
        shaped(NS, "dnt_plate", "hbm:dnt_plate", rows("EE ", "EEE", "EEE"), "E", TagIds.ingots_dineutronium);
        shaped(NS, "dnt_legs", "hbm:dnt_legs", rows("EE ", "EEE", "E E"), "E", TagIds.ingots_dineutronium);
        shaped(NS, "dnt_boots", "hbm:dnt_boots", rows("  E", "E  ", "E E"), "E", TagIds.ingots_dineutronium);
        shaped(NS, "zirconium_legs", "hbm:zirconium_legs", rows("EEE", "E E", "E E"), "E", TagIds.ingots_zirconium);

        shaped(NS, "ajr_helmet", "hbm:ajr_helmet", rows("PPC", "PBP", "IXI"),
                "P", ItemIds.plate_armor_ajr, "C", ItemIds.circuit_basic, "I", TagIds.ingots_any_plastic,
                "X", ItemIds.gas_mask_m65, "B", "hbm:titanium_helmet");
        shaped(NS, "ajr_plate", "hbm:ajr_plate", rows("MPM", "TBT", "PPP"),
                "M", ItemIds.motor_desh, "P", ItemIds.plate_armor_ajr, "T", ItemIds.gas_empty,
                "B", "hbm:titanium_plate");
        shaped(NS, "ajr_legs", "hbm:ajr_legs", rows("MPM", "PBP", "P P"),
                "M", ItemIds.motor_desh, "P", ItemIds.plate_armor_ajr, "B", "hbm:titanium_legs");
        shaped(NS, "ajr_boots", "hbm:ajr_boots", rows("P P", "PBP"),
                "P", ItemIds.plate_armor_ajr, "B", "hbm:titanium_boots");
        // KEY_RED / KEY_BLACK are the dye ore dictionary keys.
        shapeless(NS, "ajro_helmet", "hbm:ajro_helmet", "hbm:ajr_helmet", TagIds.dyes_red, TagIds.dyes_black);
        shapeless(NS, "ajro_plate", "hbm:ajro_plate", "hbm:ajr_plate", TagIds.dyes_red, TagIds.dyes_black);
        shapeless(NS, "ajro_legs", "hbm:ajro_legs", "hbm:ajr_legs", TagIds.dyes_red, TagIds.dyes_black);
        shapeless(NS, "ajro_boots", "hbm:ajro_boots", "hbm:ajr_boots", TagIds.dyes_red, TagIds.dyes_black);

        shaped(NS, "hev_helmet", "hbm:hev_helmet", rows("PPC", "PBP", "IFI"),
                "P", ItemIds.plate_armor_hev, "C", ItemIds.circuit_basic, "B", "hbm:titanium_helmet",
                "I", TagIds.ingots_any_plastic, "F", ItemIds.gas_mask_filter);
        shaped(NS, "hev_plate", "hbm:hev_plate", rows("MPM", "IBI", "PPP"),
                "P", ItemIds.plate_armor_hev, "B", "hbm:titanium_plate", "I", TagIds.ingots_any_plastic,
                "M", ItemIds.motor_desh);
        shaped(NS, "hev_legs", "hbm:hev_legs", rows("MPM", "IBI", "P P"),
                "P", ItemIds.plate_armor_hev, "B", "hbm:titanium_legs", "I", TagIds.ingots_any_plastic,
                "M", ItemIds.motor_desh);
        shaped(NS, "hev_boots", "hbm:hev_boots", rows("P P", "PBP"),
                "P", ItemIds.plate_armor_hev, "B", "hbm:titanium_boots");

        // new ItemStack(Blocks.wool, 1, 14) is red wool.
        shaped(NS, "fau_helmet", "hbm:fau_helmet", rows("PWP", "PBP", "FSF"),
                "P", ItemIds.plate_armor_fau, "W", VanillaIds.red_wool, "B", "hbm:starmetal_helmet",
                "F", ItemIds.gas_mask_filter, "S", TagIds.pipes_steel);
        shaped(NS, "fau_plate", "hbm:fau_plate", rows("MCM", "PBP", "PSP"),
                "M", ItemIds.motor_desh, "C", ItemIds.demon_core_closed, "P", ItemIds.plate_armor_fau,
                "B", "hbm:starmetal_plate", "S", ItemIds.ancient_scrap);
        shaped(NS, "fau_legs", "hbm:fau_legs", rows("MPM", "PBP", "PDP"),
                "M", ItemIds.motor_desh, "P", ItemIds.plate_armor_fau, "B", "hbm:starmetal_legs",
                "D", ItemIds.billet_polonium);
        shaped(NS, "fau_boots", "hbm:fau_boots", rows("PDP", "PBP"),
                "P", ItemIds.plate_armor_fau, "D", ItemIds.billet_polonium, "B", "hbm:starmetal_boots");

        // "Bismuth fursui- I mean armor"
        shaped(NS, "bismuth_helmet", "hbm:bismuth_helmet", rows("GPP", "P  ", "FPP"),
                "G", VanillaIds.gold_ingot, "P", ItemIds.plate_bismuth, "F", ItemIds.rag);
        shaped(NS, "bismuth_plate", "hbm:bismuth_plate", rows("RWR", "PCP", "SFS"),
                "R", ItemIds.crystal_rare, "W", TagIds.wires_gold, "P", ItemIds.plate_bismuth,
                "C", ItemIds.laser_crystal_bismuth, "S", ItemIds.ring_starmetal, "F", ItemIds.rag);
        shaped(NS, "bismuth_legs", "hbm:bismuth_legs", rows("FSF", "   ", "FSF"),
                "F", ItemIds.rag, "S", ItemIds.ring_starmetal);
        shaped(NS, "bismuth_boots", "hbm:bismuth_boots", rows("W W", "P P"),
                "W", TagIds.wires_gold, "P", ItemIds.plate_bismuth);

        shaped(NS, "euphemium_helmet", "hbm:euphemium_helmet", rows("EEE", "E E"), "E", ItemIds.plate_euphemium);
        shaped(NS, "euphemium_plate", "hbm:euphemium_plate", rows("EWE", "EEE", "EEE"),
                "E", ItemIds.plate_euphemium, "W", ItemIds.watch);
        shaped(NS, "euphemium_legs", "hbm:euphemium_legs", rows("EEE", "E E", "E E"), "E", ItemIds.plate_euphemium);
        shaped(NS, "euphemium_boots", "hbm:euphemium_boots", rows("E E", "E E"), "E", ItemIds.plate_euphemium);
    }
}
