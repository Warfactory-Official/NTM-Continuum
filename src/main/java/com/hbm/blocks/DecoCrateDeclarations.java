package com.hbm.blocks;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.lib.ItemIds;
import com.hbm.lib.TagIds;
import com.hbm.lib.VanillaIds;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Drops;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Variants;

import static dev.golem.core.dejson.Crafting.rows;
import static dev.golem.core.dejson.Crafting.shaped;
import static dev.golem.core.dejson.Crafting.shapeless;
import static dev.golem.core.dejson.Crafting.stack;

/**
 * Dejson declarations for {@link ModDecoCrateBlocks}. Touches no Minecraft class: it runs in a plain JVM from
 * {@code dejson.gradle.kts}. The family's own ids are spelled out, since {@code ItemIds} is generated from the
 * registry after this wave.
 *
 * <p>The renderer-drawn blocks carry the empty placeholder model. Their items are 1.7's item renderers
 * ({@code ItemRenderLibrary} bobblehead, {@code RenderPlushie}/{@code RenderSnowglobe}/
 * {@code RenderVendingMachine}/{@code RenderFileCabinet} providers) as OBJ item models, one per subtype, with
 * the 1.7 {@code renderInventory} translate/scale as the GUI frame. The bobblehead and plushie items are
 * {@code hbm:custom_item} models that call the block renderers' own draw, as 1.7's did, so the engraved name, held
 * props, glow layers and Number Nine's helmet and cigarette are on the item too. The broadcaster, pest control box and FM radio had no item
 * renderer in 1.7 and showed their block icon flat, as they do here. The wooden structures keep hand-built
 * element models and a multipart blockstate (their joins), which this vocabulary does not express.
 *
 * <p>Not declared: the tungsten crate and safe are crafted from a steel crate through 1.7's
 * {@code ContainerUpgradeCraftingHandler}, which carried the steel crate's contents over; the port has no
 * shaped recipe that copies a container, so they are plain shaped recipes here and a filled steel crate used in
 * one loses its contents.
 */
public final class DecoCrateDeclarations {

    private static final String NS = "hbm";
    private static final String DUMMY = "hbm:block/machine_dummy_empty";
    private static boolean declared;

    private static final String[] CAPS = {"nuka", "quantum", "sparkle", "rad", "korl", "fritz"};
    private static final String[] COILS = {"alloy", "gold", "neodymium", "magtung", "schrabidium", "schrabidate",
            "starmetal", "chlorophyte", "mese"};
    private static final String[] ABSORBERS = {"rad_absorber", "rad_absorber_red", "rad_absorber_green", "rad_absorber_pink"};
    private static final String[] ABSORBER_TEX = {"absorber", "absorber_red", "absorber_green", "absorber_pink"};

    private DecoCrateDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;
        blocks();
        items();
        drops();
        recipes();
    }

    private static void blocks() {
        // --- crates ---
        Dejson.block(NS, "crate").models(Models.cubeAll("hbm:blocks/crate")).states(Variants.simple())
                .blockItem().register();
        Dejson.block(NS, "crate_can").rendered(DUMMY).states(Variants.simple()).register();
        // 1.7 BlockStorageCrate.getIcon: the top icon on top and bottom, the side icon all round
        Dejson.block(NS, "crate_tungsten")
                .models(Models.cubeBottomTop("hbm:blocks/crate_tungsten_top", "hbm:blocks/crate_tungsten_side",
                        "hbm:blocks/crate_tungsten_top"))
                .states(Variants.product(Variants.uniform("facing", "north", "east", "south", "west"),
                        Variants.uniform("hot", "false", "true")))
                .blockItem().register();
        // the safe's door is on the face it was placed towards
        Dejson.block(NS, "safe")
                .models(Models.orientable("hbm:blocks/safe_front", "hbm:blocks/safe_side", "hbm:blocks/safe_side"))
                .states(Variants.product(Variants.facing(), Variants.uniform("hot", "false", "true")))
                .blockItem().register();
        Dejson.block(NS, "filing_cabinet").rendered(DUMMY)
                .states(Variants.product(Variants.uniform("facing", "north", "east", "south", "west"),
                        Variants.uniform("type", "green", "steel"))).register();

        // --- deco props ---
        for (String prop : new String[]{"bobblehead", "plushie", "snowglobe"}) {
            Dejson.block(NS, prop).rendered(DUMMY).states(Variants.uniformRange("rotation", 0, 15)).register();
        }
        Dejson.block(NS, "vending_machine").rendered(DUMMY)
                .states(Variants.product(Variants.uniform("facing", "north", "east", "south", "west"),
                        Variants.uniform("cell", "false", "true"), Variants.uniform("snacks", "false", "true"))).register();
        for (String box : new String[]{"broadcaster_pc", "radiorec"}) {
            Dejson.block(NS, box).rendered(DUMMY)
                    .states(Variants.uniform("facing", "north", "east", "south", "west")).register();
        }
        Dejson.block(NS, "radiobox").rendered(DUMMY)
                .states(Variants.product(Variants.uniform("facing", "north", "east", "south", "west"),
                        Variants.uniform("enabled", "false", "true"))).register();
        Dejson.block(NS, "deco_emitter").models(Models.cubeAll("hbm:blocks/emitter"))
                .states(Variants.uniform("facing", "down", "up", "north", "south", "west", "east"))
                .register();
        Dejson.block(NS, "part_emitter").models(Models.cubeAll("hbm:blocks/part_top")).states(Variants.simple())
                .register();

        // --- simple blocks ---
        for (String coil : COILS) {
            Dejson.block(NS, "hadron_coil_" + coil).models(Models.cubeAll("hbm:blocks/hadron_coil_" + coil))
                    .states(Variants.simple()).register();
        }
        for (String cap : CAPS) {
            Dejson.block(NS, "block_cap_" + cap)
                    .models(Models.cubeBottomTop("hbm:blocks/block_cap_" + cap + "_top", "hbm:blocks/block_cap_" + cap,
                            "hbm:blocks/block_cap_" + cap + "_top"))
                    .states(Variants.simple()).blockItem().register();
        }
        Dejson.block(NS, "pink_log").models(Models.column("hbm:blocks/pink_log", "minecraft:block/oak_log"))
                .states(Variants.axis()).blockItem().register();
        for (int i = 0; i < ABSORBERS.length; i++) {
            Dejson.block(NS, ABSORBERS[i]).models(Models.cubeAll("hbm:blocks/" + ABSORBER_TEX[i]))
                    .states(Variants.simple()).blockItem().register();
        }
        // 1.7 BlockGlyphid: two textures per subtype, chosen per position
        Dejson.block(NS, "glyphid_base")
                .models(Models.group()
                        .and("_normal_0", Models.cubeAll("hbm:blocks/glyphid_base"))
                        .and("_normal_1", Models.cubeAll("hbm:blocks/glyphid_base_alt"))
                        .and("_infested_0", Models.cubeAll("hbm:blocks/glyphid_base_infested"))
                        .and("_infested_1", Models.cubeAll("hbm:blocks/glyphid_base_infested_alt"))
                        .and("_rad_0", Models.cubeAll("hbm:blocks/glyphid_base_rad"))
                        .and("_rad_1", Models.cubeAll("hbm:blocks/glyphid_base_rad_alt")))
                .states(Variants.product(Variants.select("type", "normal", "_normal", "infested", "_infested",
                        "rad", "_rad"), Variants.pool(2)))
                .register();
        Dejson.block(NS, "glyphid_spawner")
                .models(Models.group()
                        .and("_normal", Models.cubeAll("hbm:blocks/glyphid_eggs_alt"))
                        .and("_infested", Models.cubeAll("hbm:blocks/glyphid_eggs_infested"))
                        .and("_rad", Models.cubeAll("hbm:blocks/glyphid_eggs_rad")))
                .states(Variants.select("type", "normal", "_normal", "infested", "_infested", "rad", "_rad"))
                .register();
        Dejson.block(NS, "field_disturber").models(Models.cubeAll("hbm:blocks/field_disturber"))
                .states(Variants.simple()).blockItem().register();
    }

    private static void items() {
        for (String block : new String[]{"deco_emitter", "part_emitter"}) {
            Dejson.item(NS, block).shares("hbm:block/" + block).register();
        }
        for (String coil : COILS) Dejson.item(NS, "hadron_coil_" + coil).shares("hbm:block/hadron_coil_" + coil).register();
        // no item renderer in 1.7: the block texture, flat
        Dejson.item(NS, "broadcaster_pc").models(Models.generated("hbm:blocks/broadcaster_pc")).register();
        Dejson.item(NS, "radiobox").models(Models.generated("hbm:blocks/radiobox")).register();
        Dejson.item(NS, "radiorec").models(Models.generated("hbm:blocks/radiorec")).register();
        for (String wood : new String[]{"roof", "scaffold", "ceiling"}) {
            Dejson.item(NS, "wood_structure_" + wood).shares("hbm:block/wood_structure_" + wood).register();
        }
        Dejson.item(NS, "glyphid_base").models(Models.group()
                .and("", Models.blockItem("hbm:block/glyphid_base_normal_0"))
                .and("_infested", Models.blockItem("hbm:block/glyphid_base_infested_0"))
                .and("_rad", Models.blockItem("hbm:block/glyphid_base_rad_0")))
                .selects(NS + ":item_variant").register();
        Dejson.item(NS, "glyphid_spawner").models(Models.group()
                .and("", Models.blockItem("hbm:block/glyphid_spawner_normal"))
                .and("_infested", Models.blockItem("hbm:block/glyphid_spawner_infested"))
                .and("_rad", Models.blockItem("hbm:block/glyphid_spawner_rad")))
                .selects(NS + ":item_variant").register();

        // RenderConserve inventory: the crate OBJ in its block texture (as the supply crate that shares it)
        {
            JsonObject model = machine("hbm:models/blocks/conservecrate.obj", "hbm:textures/block/crate_can.png");
            model.addProperty("scale", 1.0);
            model.addProperty("yaw", 0);
            model.addProperty("ty", 0.0);
            // the block-framed form (as crate_supply): 1.7 drew an ISBRH item like a block in the slot
            JsonObject special = new JsonObject();
            special.addProperty("type", "minecraft:special");
            special.addProperty("base", "hbm:item/machine_obj_base");
            special.add("model", model);
            item("crate_can", special);
        }
        // ItemRenderLibrary bobblehead / RenderPlushie item: ItemRenderBase around the block renderers' own
        // renderBobble / renderPlushie, so the item has every prop, glow skin and the engraved name
        com.hbm.items.ItemRendererDeclarations.model("bobblehead", "bobblehead", "machine");
        com.hbm.items.ItemRendererDeclarations.model("plushie", "plushie", "machine");
        snowglobes();
        // RenderVendingMachine item: inventory T(0,-4,0) S(6.25); the "Soda" or "Obamna" part
        {
            JsonArray cases = new JsonArray();
            String[] parts = {"Soda", "Obamna"};
            for (int i = 0; i < 2; i++) {
                JsonObject model = machine("hbm:models/machines/vending_machine.obj",
                        "hbm:textures/models/machines/vending_machine.png");
                common(model, 1.0, 0, 0, 0, 0, -4, 6.25);
                model.add("parts", strings(parts[i]));
                cases.add(when(i, model));
            }
            variantItem("vending_machine", cases);
        }
        // RenderFileCabinet item: inventory T(-1,0.5,-1) Ry(180) S(4); common T(0,-1.25,0) S(2.75), renderAll
        {
            JsonArray cases = new JsonArray();
            String[] tex = {"file_cabinet", "file_cabinet_steel"};
            for (int i = 0; i < 2; i++) {
                JsonObject model = machine("hbm:models/file_cabinet.obj", "hbm:textures/models/" + tex[i] + ".png");
                common(model, 2.75, 0, 0, -1.25, 0, 0, 4);
                model.add("guiTranslate", numbers(-1, 0.5, -1));
                model.addProperty("guiYaw", 180.0);
                cases.add(when(i, model));
            }
            variantItem("filing_cabinet", cases);
        }
    }

    /** RenderSnowglobe item: inventory T(0,-2,0) S(6); common T(0,0.25,0) S(3), then S(1/16): socket, glass, scene. */
    private static void snowglobes() {
        String[] scenes = {null, "RivetCity", "TenpennyTower", "Lucky38", "SierraMadre", "Prydwen"};
        JsonArray cases = new JsonArray();
        for (int type = 0; type < scenes.length; type++) {
            JsonObject model = machine("hbm:models/trinkets/snowglobe.obj", "hbm:textures/models/trinkets/snowglobe.png");
            common(model, 3 * 0.0625, 0, 0, 0.25, 0, -2, 6);
            model.addProperty("noCull", true);
            model.add("parts", strings("Socket"));
            model.add("translate", numbers(0, 0, 0));
            JsonArray passes = new JsonArray();
            passes.add(pass("hbm:textures/models/trinkets/snowglobe_glass.png", null, "Glass"));
            if (scenes[type] != null) passes.add(pass("hbm:textures/models/trinkets/snowglobe_features.png", null, scenes[type]));
            model.add("passes", passes);
            cases.add(when(type, model));
        }
        variantItem("snowglobe", cases);
    }

    private static void drops() {
        Drops.self(NS, "crate");
        Drops.self(NS, "crate_can");
        for (String crate : new String[]{"crate_tungsten", "safe"}) {
            Drops.container(NS, crate, "minecraft:container", "minecraft:custom_data", "minecraft:custom_name");
        }
        // drop themselves through their own getDrops (subtype from the block entity or state, or nothing)
        for (String block : new String[]{"filing_cabinet", "bobblehead", "plushie", "snowglobe", "vending_machine",
                "glyphid_base", "glyphid_spawner"}) {
            Drops.nothing(NS, block);
        }
        for (String block : new String[]{"broadcaster_pc", "radiobox", "radiorec", "deco_emitter", "part_emitter",
                "pink_log", "field_disturber", "wood_structure_roof", "wood_structure_scaffold", "wood_structure_ceiling"}) {
            Drops.self(NS, block);
        }
        for (String coil : COILS) Drops.self(NS, "hadron_coil_" + coil);
        for (String absorber : ABSORBERS) Drops.self(NS, absorber);
        // 1.7 BlockCap: 128 of its caps, no fortune; silk touch keeps the block
        for (String cap : CAPS) Drops.oreNoFortune(NS, "block_cap_" + cap, "hbm:cap_" + cap, 128, 128);
    }

    private static void recipes() {
        // 1.7 CraftingManager. crate_tungsten and safe are ContainerUpgradeCraftingHandler recipes that keep the
        // steel crate's contents: declared with the other content-copy upgrades in ModContainerRecipes.
        shaped(NS, "radiobox", "hbm:radiobox", rows("PLP", "PSP", "PLP"),
                "P", TagIds.plates_steel, "S", ItemIds.ring_starmetal, "L", TagIds.plates_dura_steel);
        shaped(NS, "radiorec", "hbm:radiorec", rows("  W", "PCP", "PIP"),
                "W", TagIds.wires_copper, "P", TagIds.plates_steel, "C", ItemIds.circuit_vacuum_tube,
                "I", TagIds.ingots_any_plastic);
        shaped(NS, "rad_absorber", "hbm:rad_absorber", rows("ICI", "CPC", "ICI"),
                "I", TagIds.ingots_copper, "C", TagIds.dusts_coal, "P", TagIds.dusts_lead);
        shaped(NS, "rad_absorber_red", "hbm:rad_absorber_red", rows("ICI", "CPC", "ICI"),
                "I", TagIds.ingots_titanium, "C", TagIds.dusts_coal, "P", "hbm:rad_absorber");
        shaped(NS, "rad_absorber_green", "hbm:rad_absorber_green", rows("ICI", "CPC", "ICI"),
                "I", TagIds.ingots_any_plastic, "C", TagIds.dusts_desh_mix, "P", "hbm:rad_absorber_red");
        shaped(NS, "rad_absorber_pink", "hbm:rad_absorber_pink", rows("ICI", "CPC", "ICI"),
                "I", TagIds.ingots_saturnite, "C", ItemIds.powder_nitan_mix, "P", "hbm:rad_absorber_green");
        shaped(NS, "decon", ItemIds.decon, rows("BGB", "SAS", "BSB"),
                "B", "#c:ingots/beryllium", "G", VanillaIds.iron_bars, "S", "#c:ingots/steel", "A", "hbm:rad_absorber");
        shaped(NS, "deco_emitter", "hbm:deco_emitter", rows("IDI", "DRD", "IDI"),
                "I", TagIds.ingots_iron, "D", TagIds.gems_diamond, "R", TagIds.storage_blocks_redstone);
        shaped(NS, "field_disturber", "hbm:field_disturber", rows("ICI", "CAC", "ICI"),
                "I", TagIds.ingots_starmetal, "C", ItemIds.circuit_bismoid, "A", ItemIds.gem_alexandrite);
        shaped(NS, "filing_cabinet_steel", stack("hbm:filing_cabinet").with("hbm:item_variant", 1),
                rows(" P ", "PIP", " P "), "P", TagIds.plates_steel, "I", ItemIds.plate_polymer);
        shaped(NS, "plushie_yomi", stack("hbm:plushie").with("hbm:item_variant", 1), rows("LCR"),
                "L", TagIds.crops_carrot, "C", "hbm:rag", "R", ItemIds.circuit_vacuum_tube);
        shaped(NS, "plushie_numbernine", stack("hbm:plushie").with("hbm:item_variant", 2), rows(" C ", "LCR", " C "),
                "L", "hbm:cigarette", "C", "hbm:rag", "R", TagIds.gems_coal);
        shaped(NS, "wood_structure_roof", "hbm:wood_structure_roof", 16, rows("SSS", "F F"),
                "S", TagIds.minecraft_wooden_slabs, "F", VanillaIds.oak_fence);
        shaped(NS, "wood_structure_ceiling", "hbm:wood_structure_ceiling", 16, rows("F F", "SSS"),
                "S", TagIds.minecraft_wooden_slabs, "F", VanillaIds.oak_fence);
        shaped(NS, "wood_structure_scaffold", "hbm:wood_structure_scaffold", 4, rows("SSS", "F F", "F F"),
                "S", TagIds.minecraft_wooden_slabs, "F", VanillaIds.oak_fence);
        shaped(NS, "pink_planks_from_log", ItemIds.pink_planks, 4, rows("W"), "W", "hbm:pink_log");
        // the dense coils break back into their wire
        shapeless(NS, "hadron_coil_gold_wire", ItemIds.wire_dense_gold, 4, "hbm:hadron_coil_gold");
        shapeless(NS, "hadron_coil_neodymium_wire", ItemIds.wire_dense_neodymium, 4, "hbm:hadron_coil_neodymium");
        shapeless(NS, "hadron_coil_magtung_wire", ItemIds.wire_dense_magnetized_tungsten, 4, "hbm:hadron_coil_magtung");
        shapeless(NS, "hadron_coil_schrabidium_wire", ItemIds.wire_dense_schrabidium, 2, "hbm:hadron_coil_schrabidium");
        shapeless(NS, "hadron_coil_schrabidate_wire", ItemIds.wire_dense_schrabidate, 2, "hbm:hadron_coil_schrabidate");
        shapeless(NS, "hadron_coil_starmetal_wire", ItemIds.wire_dense_starmetal, 2, "hbm:hadron_coil_starmetal");
        shapeless(NS, "hadron_coil_chlorophyte_powder", ItemIds.powder_chlorophyte, 2, "hbm:hadron_coil_chlorophyte");
        shapeless(NS, "hadron_coil_mese_wire", ItemIds.wire_dense_dineutronium, 1, "hbm:hadron_coil_mese");
    }

    // --- item-model helpers ---

    /** {@code renderCommon}'s T(x,y,z) then S(scale) at yaw, and {@code renderInventory}'s T(0,gy,0) S(gs). */
    private static void common(JsonObject model, double scale, double yaw, double x, double y, double z,
                               double guiY, double guiScale) {
        model.addProperty("scale", scale);
        model.addProperty("yaw", yaw);
        model.addProperty("rawLegacyTransform", true);
        model.add("itemTranslate", numbers(x, y, z));
        model.add("guiTranslate", numbers(0, guiY, 0));
        model.addProperty("guiScale", guiScale);
    }

    private static JsonObject machine(String obj, String texture) {
        JsonObject model = new JsonObject();
        model.addProperty("type", "hbm:machine_obj");
        model.addProperty("obj", obj);
        model.addProperty("texture", texture);
        return model;
    }

    private static JsonObject pass(String texture, String obj, String... parts) {
        JsonObject pass = new JsonObject();
        pass.addProperty("texture", texture);
        if (obj != null) pass.addProperty("obj", obj);
        pass.add("parts", strings(parts));
        return pass;
    }

    private static JsonObject when(int variant, JsonObject model) {
        JsonObject entry = new JsonObject();
        entry.addProperty("when", String.valueOf(variant));
        entry.add("model", model);
        return entry;
    }

    private static void item(String name, JsonObject model) {
        Dejson.item(NS, name).shares("hbm:item/machine_obj_base").register();
        Dejson.itemDefinition("hbm:item/" + name).add("model", model);
        Dejson.itemDefinition("hbm:item/" + name).addProperty("oversized_in_gui", true);
    }

    /** A {@code minecraft:select} on {@code hbm:item_variant} over one OBJ model per subtype; case 0 is the fallback. */
    private static void variantItem(String name, JsonArray cases) {
        JsonObject select = new JsonObject();
        select.addProperty("type", "minecraft:select");
        select.addProperty("property", NS + ":item_variant");
        select.add("cases", cases);
        select.add("fallback", cases.get(0).getAsJsonObject().get("model").deepCopy());
        item(name, select);
    }

    private static JsonArray numbers(double... values) {
        JsonArray array = new JsonArray();
        for (double value : values) array.add(value);
        return array;
    }

    private static JsonArray strings(String... values) {
        JsonArray array = new JsonArray();
        for (String value : values) array.add(value);
        return array;
    }
}
