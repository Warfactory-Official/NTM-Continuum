package com.hbm.blocks;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.blocks.generic.BlockBobble;
import com.hbm.blocks.generic.BlockPlushie;
import com.hbm.blocks.generic.BlockSnowglobe;
import com.hbm.items.ItemRendererDeclarations;
import com.hbm.lib.ItemIds;
import com.hbm.lib.TagIds;
import com.hbm.lib.VanillaIds;
import dev.golem.core.dejson.Dejson;
import java.util.Locale;

import static dev.golem.core.dejson.Crafting.rows;
import static dev.golem.core.dejson.Crafting.shaped;
import static dev.golem.core.dejson.Crafting.shapeless;
import static dev.golem.core.dejson.Crafting.stack;

/**
 * The typed OBJ item data and the recipes of {@link ModDecoCrateBlocks}; every block's model, blockstate, plain
 * item model, loot and tags are stated on its registration there (Reg). Touches no Minecraft class: it runs in a
 * plain JVM from {@code dejson.gradle.kts}. The family's own ids are spelled out, since {@code ItemIds} is
 * generated from the registry.
 *
 * <p>The renderer-drawn blocks carry the empty placeholder model. Their items are 1.7's item renderers
 * ({@code ItemRenderLibrary} bobblehead, {@code RenderPlushie}/{@code RenderSnowglobe}/
 * {@code RenderVendingMachine}/{@code RenderFileCabinet}) as OBJ item models, one per subtype, with the 1.7
 * {@code renderInventory} translate/scale as the GUI frame. The bobblehead and plushie items are
 * {@code hbm:custom_item} models that call the block renderers' own draw, as 1.7's did, so the engraved name,
 * props, glow layers and Number Nine's helmet and cigarette show on the item. The broadcaster, pest control box
 * and FM radio had no item renderer in 1.7 and show their block icon flat. The wooden structures keep hand-built
 * element models and a multipart blockstate, which this vocabulary does not express.
 *
 * <p>Not declared: the tungsten crate and safe. 1.7 crafted them from a steel crate through
 * {@code ContainerUpgradeCraftingHandler}, which carried the contents over; the port has no shaped recipe
 * that copies a container, so a filled steel crate used in one loses its contents.
 */
public final class DecoCrateDeclarations {

    private static final String NS = "hbm";
    private static boolean declared;

    private DecoCrateDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;
        items();
        recipes();
    }

    private static void items() {
        // RenderConserve inventory: the crate OBJ in its block texture (as the supply crate that shares it)
        {
            JsonObject model = machine("hbm:models/blocks/conservecrate.obj", "hbm:textures/block/crate_can.png");
            // RenderConserve.renderInventoryBlock: translate(0, -0.5, 0), renderWithIcon(conservecrate), in 1.7's
            // 3D block-item frame (ModItemModels "block" frame)
            model.addProperty("frame", "block");
            model.addProperty("scale", 1.0);
            model.addProperty("yaw", 0.0);
            model.addProperty("ty", 0.0);
            model.addProperty("rawLegacyTransform", true);
            model.add("itemTranslate", numbers(0, -0.5, 0));
            item("crate_can", model);
        }
        // ItemRenderLibrary bobblehead / RenderPlushie item: ItemRenderBase around the block renderers' own
        // renderBobble / renderPlushie, so the item has every prop, glow skin and the engraved name
        // one item per type; the renderer reads the member's type
        for (String id : trinketIds("bobblehead", BlockBobble.BobbleType.VALUES)) {
            ItemRendererDeclarations.model(id, "bobblehead", "machine");
        }
        for (String id : trinketIds("plushie", BlockPlushie.PlushieType.VALUES)) {
            ItemRendererDeclarations.model(id, "plushie", "machine");
        }
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
            variantItems(new String[] {"vending_machine", "vending_machine_snacks"}, cases);
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
            variantItems(new String[] {"filing_cabinet_green", "filing_cabinet_steel"}, cases);
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
        variantItems(trinketIds("snowglobe", BlockSnowglobe.SnowglobeType.VALUES),
                cases);
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
                "I", TagIds.ingots_any_plastic, "C", ItemIds.powder_desh_mix, "P", "hbm:rad_absorber_red");
        shaped(NS, "rad_absorber_pink", "hbm:rad_absorber_pink", rows("ICI", "CPC", "ICI"),
                "I", TagIds.ingots_saturnite, "C", ItemIds.powder_nitan_mix, "P", "hbm:rad_absorber_green");
        shaped(NS, "decon", ItemIds.decon, rows("BGB", "SAS", "BSB"),
                "B", "#c:ingots/beryllium", "G", VanillaIds.iron_bars, "S", "#c:ingots/steel", "A", "hbm:rad_absorber");
        shaped(NS, "deco_emitter", "hbm:deco_emitter", rows("IDI", "DRD", "IDI"),
                "I", TagIds.ingots_iron, "D", TagIds.gems_diamond, "R", TagIds.storage_blocks_redstone);
        shaped(NS, "field_disturber", "hbm:field_disturber", rows("ICI", "CAC", "ICI"),
                "I", TagIds.ingots_starmetal, "C", ItemIds.circuit_bismoid, "A", ItemIds.gem_alexandrite);
        shaped(NS, "filing_cabinet_steel", "hbm:filing_cabinet_steel",
                rows(" P ", "PIP", " P "), "P", TagIds.plates_steel, "I", ItemIds.plate_polymer);
        shaped(NS, "plushie_yomi", "hbm:plushie_yomi", rows("LCR"),
                "L", TagIds.crops_carrot, "C", "hbm:rag", "R", ItemIds.circuit_vacuum_tube);
        shaped(NS, "plushie_numbernine", "hbm:plushie_numbernine", rows(" C ", "LCR", " C "),
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

    // Item-model helpers

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

    /** {@code <base>} for the NONE type, {@code <base>_<type>} for the rest (ntm-next's trinket ids). */
    public static String[] trinketIds(String base, Enum<?>[] types) {
        String[] ids = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            ids[i] = i == 0 ? base : base + "_" + types[i].name().toLowerCase(Locale.ROOT);
        }
        return ids;
    }

    /** One item per subtype (1.7 metadata = index), each drawn with its case's OBJ model. */
    private static void variantItems(String[] ids, JsonArray cases) {
        for (int i = 0; i < ids.length; i++) item(ids[i], cases.get(i).getAsJsonObject().get("model").getAsJsonObject());
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
