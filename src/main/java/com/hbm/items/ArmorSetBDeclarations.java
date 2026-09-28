package com.hbm.items;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.lib.ItemIds;
import com.hbm.lib.TagIds;
import com.hbm.lib.VanillaIds;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Drops;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Tags;
import dev.golem.core.dejson.Variants;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.golem.core.dejson.Crafting.rows;
import static dev.golem.core.dejson.Crafting.shaped;
import static dev.golem.core.dejson.Crafting.shapeless;

/**
 * Dejson declarations for {@link ModArmorSetBItems} and {@code statue_elb_f}: item models, the statue's block,
 * repair tags and the 1.7 recipes ({@code ArmorRecipes}, {@code CraftingManager}, {@code ConsumableRecipes}).
 * Touches no Minecraft class. This wave's own ids are spelled out, since {@code ItemIds} is regenerated after it.
 *
 * <p>The three fuelled/powered suits drew their icon through {@code renderStandard} over the worn OBJ, like the
 * suits of {@link ArmorSetDeclarations}, and use the same {@code armor_<set>_<slot>} path of {@code MachineItemModel}.
 * The M1TTY helmet's own icon (Helmet textured, Lamps untextured in (1, 1, 0.8)) is the same head framing; its lamps
 * wear a plain (1, 1, 0.8) texture since an item model part carries no tint.
 */
public final class ArmorSetBDeclarations {

    private static final String NS = "hbm";
    private static final String DUMMY = "hbm:block/machine_dummy_empty";
    private static boolean declared;

    private ArmorSetBDeclarations() {}

    /** Plain 1.7 sprites: id -> texture name (the Fabulous Hat's texture is {@code hat}). */
    static final String[][] SPRITES = {
            {"goggles", "goggles"}, {"ashglasses", "ashglasses"}, {"mask_rag", "mask_rag"}, {"mask_piss", "mask_piss"},
            {"nossy_hat", "hat"}, {"no9", "no9"}, {"beta", "beta"}, {"jackt", "jackt"}, {"jackt2", "jackt2"},
            {"iv_blood", "iv_blood"}, {"iv_xp_empty", "iv_xp_empty"}, {"iv_xp", "iv_xp"},
    };

    public static void declare() {
        if (declared) return;
        declared = true;

        for (String[] sprite : SPRITES) {
            Dejson.item(NS, sprite[0]).models(Models.generated("hbm:items/" + sprite[1])).register();
        }

        modelled("steamsuit", "models/armor/steamsuit.obj", "steamsuit", List.of("Head"), List.of("Body"),
                "LeftBoot", "RightBoot", List.of("Head", "Body", "LeftArm", "RightArm", "LeftLeg", "RightLeg",
                        "LeftBoot", "RightBoot"), Map.of());
        modelled("dieselsuit", "models/armor/bnuuy.obj", "bnuuy", List.of("Head"), List.of("Body"),
                "LeftBoot", "RightBoot", List.of("Head", "Body", "LeftArm", "RightArm", "LeftLeg", "RightLeg",
                        "LeftBoot", "RightBoot"), Map.of());
        modelled("envsuit", "models/armor/envsuit.obj", "envsuit", List.of("Helmet"), List.of("Chest"),
                "LeftFoot", "RightFoot", List.of("Helmet", "Lamps", "Chest", "LeftArm", "RightArm", "LeftLeg",
                        "RightLeg", "LeftFoot", "RightFoot", "Tail"),
                Map.of("Lamps", "hbm:textures/models/misc/envsuit_lamp.png"));

        // statue_elb_f: renderer-drawn, facing states; 1.7's item was the flat blocks/code icon.
        Dejson.block(NS, "statue_elb_f").rendered(DUMMY)
                .states(Variants.uniform("facing", "north", "east", "south", "west")).register();
        Dejson.item(NS, "statue_elb_f").models(Models.generated("hbm:blocks/code")).register();
        // 1.7 func_147480_a(x, y, z, true): the red key pops it out as itself.
        Drops.self(NS, "statue_elb_f");

        // customCraftingMaterial of each 1.7 material (repairs/steel, /infamy, /hev exist already;
        // repairs/alloy stays empty as aMatAlloy had none).
        Tags.item(NS, "repairs/rags", ItemIds.rag);
        Tags.item(NS, "repairs/desh_suit", ItemIds.ingot_desh);
        Tags.item(NS, "repairs/dieselsuit", ItemIds.plate_copper);

        recipes();
    }

    /** One suit's four icons over its worn OBJ; see ArmorSetDeclarations.modelled. */
    private static void modelled(String set, String obj, String tex, List<String> helmet, List<String> chest,
                                 String leftBoot, String rightBoot, List<String> all, Map<String, String> extraHead) {
        String objId = NS + ":" + obj;
        Map<String, String> head = new LinkedHashMap<>();
        for (String part : helmet) head.put(part, tex(tex + "_helmet"));
        head.putAll(extraHead);
        Map<String, String> body = new LinkedHashMap<>();
        for (String part : chest) body.put(part, tex(tex + "_chest"));
        body.put("LeftArm", tex(tex + "_arm"));
        body.put("RightArm", tex(tex + "_arm"));
        Map<String, String> legs = new LinkedHashMap<>();
        legs.put("LeftLeg", tex(tex + "_leg"));
        legs.put("RightLeg", tex(tex + "_leg"));
        Map<String, String> boots = new LinkedHashMap<>();
        boots.put(leftBoot, tex(tex + "_leg"));
        boots.put(rightBoot, tex(tex + "_leg"));

        piece(set + "_helmet", set, "head", objId, tex(tex + "_helmet"), head, all, false);
        piece(set + "_plate", set, "chest", objId, tex(tex + "_chest"), body, all, false);
        piece(set + "_legs", set, "legs", objId, tex(tex + "_leg"), legs, all, true);
        piece(set + "_boots", set, "feet", objId, tex(tex + "_leg"), boots, all, true);
    }

    private static void piece(String item, String set, String slot, String obj, String texture,
                              Map<String, String> parts, List<String> all, boolean noCull) {
        JsonObject model = new JsonObject();
        model.addProperty("type", "hbm:machine_obj");
        model.addProperty("obj", obj);
        model.addProperty("texture", texture);
        model.addProperty("noCull", noCull);
        model.addProperty("animation", "armor_" + set + "_" + slot);
        JsonObject textures = new JsonObject();
        parts.forEach(textures::addProperty);
        model.add("partTextures", textures);
        JsonArray exclude = new JsonArray();
        for (String part : all) if (!parts.containsKey(part)) exclude.add(part);
        model.add("exclude", exclude);

        Dejson.item(NS, item).shares("hbm:item/machine_obj_base").register();
        Dejson.itemDefinition("hbm:item/" + item).add("model", model);
    }

    private static String tex(String name) {
        return NS + ":textures/armor/" + name + ".png";
    }

    private static void recipes() {
        // ArmorRecipes: steam suit over steel armour
        shaped(NS, "steamsuit_helmet", "hbm:steamsuit_helmet", rows("DCD", "CXC", " F "),
                "D", TagIds.ingots_desh, "C", TagIds.plates_copper, "X", "hbm:steel_helmet", "F", ItemIds.gas_mask_filter);
        shaped(NS, "steamsuit_plate", "hbm:steamsuit_plate", rows("C C", "DXD", "CFC"),
                "D", TagIds.ingots_desh, "C", TagIds.plates_copper, "X", "hbm:steel_plate", "F", ItemIds.tank_steel);
        shaped(NS, "steamsuit_legs", "hbm:steamsuit_legs", rows("CCC", "DXD", "C C"),
                "D", TagIds.ingots_desh, "C", TagIds.plates_copper, "X", "hbm:steel_legs");
        shaped(NS, "steamsuit_boots", "hbm:steamsuit_boots", rows("C C", "DXD"),
                "D", TagIds.ingots_desh, "C", TagIds.plates_copper, "X", "hbm:steel_boots");
        // new ItemStack(Blocks.wool, 1, 14) is red wool; circuit ANALOG
        shaped(NS, "dieselsuit_helmet", "hbm:dieselsuit_helmet", rows("W W", "W W", "SCS"),
                "W", VanillaIds.red_wool, "S", TagIds.ingots_steel, "C", ItemIds.circuit_analog);
        shaped(NS, "dieselsuit_plate", "hbm:dieselsuit_plate", rows("W W", "CDC", "SWS"),
                "W", VanillaIds.red_wool, "S", TagIds.ingots_steel, "C", ItemIds.circuit_analog,
                "D", ItemIds.machine_diesel);
        shaped(NS, "dieselsuit_legs", "hbm:dieselsuit_legs", rows("M M", "S S", "W W"),
                "W", VanillaIds.red_wool, "S", TagIds.ingots_steel, "M", ItemIds.motor);
        shaped(NS, "dieselsuit_boots", "hbm:dieselsuit_boots", rows("W W", "S S"),
                "W", VanillaIds.red_wool, "S", TagIds.ingots_steel);
        // KEY_ANYPANE is any glass pane; circuit CHIP; RUBBER.ingot
        shaped(NS, "envsuit_helmet", "hbm:envsuit_helmet", rows("TCT", "TGT", "RRR"),
                "T", TagIds.plates_titanium, "C", ItemIds.circuit_chip, "G", TagIds.glass_panes,
                "R", TagIds.ingots_rubber);
        shaped(NS, "envsuit_plate", "hbm:envsuit_plate", rows("T T", "TCT", "RRR"),
                "T", TagIds.plates_titanium, "C", TagIds.plates_cast_titanium, "R", TagIds.ingots_rubber);
        shaped(NS, "envsuit_legs", "hbm:envsuit_legs", rows("TCT", "R R", "T T"),
                "T", TagIds.plates_titanium, "C", TagIds.plates_cast_titanium, "R", TagIds.ingots_rubber);
        shaped(NS, "envsuit_boots", "hbm:envsuit_boots", rows("R R", "T T"),
                "T", TagIds.plates_titanium, "R", TagIds.ingots_rubber);
        shaped(NS, "goggles", "hbm:goggles", rows("P P", "GPG"),
                "G", TagIds.glass_panes, "P", TagIds.plates_steel);
        shaped(NS, "ashglasses", "hbm:ashglasses", rows("I I", "GPG"),
                "I", TagIds.ingots_any_rubber, "G", ItemIds.glass_ash, "P", TagIds.ingots_any_plastic);
        shaped(NS, "mask_rag", "hbm:mask_rag", rows("RRR"), "R", ItemIds.rag_damp);
        shaped(NS, "mask_piss", "hbm:mask_piss", rows("RRR"), "R", ItemIds.rag_piss);
        // CraftingManager
        shaped(NS, "jackt", "hbm:jackt", rows("S S", "LIL", "LIL"),
                "S", TagIds.plates_steel, "L", VanillaIds.leather, "I", TagIds.ingots_any_rubber);
        shaped(NS, "jackt2", "hbm:jackt2", rows("S S", "LIL", "III"),
                "S", TagIds.plates_steel, "L", VanillaIds.leather, "I", TagIds.ingots_any_rubber);
        // ConsumableRecipes
        shapeless(NS, "iv_xp_empty", "hbm:iv_xp_empty", ItemIds.iv_empty, ItemIds.powder_magic);
        shapeless(NS, "radaway_iv_blood", ItemIds.radaway, "hbm:iv_blood", TagIds.dusts_coal, VanillaIds.pumpkin_seeds);
    }
}
