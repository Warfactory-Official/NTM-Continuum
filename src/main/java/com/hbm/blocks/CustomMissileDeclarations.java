package com.hbm.blocks;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.items.weapon.MissilePartTable;
import com.hbm.lib.ItemIds;
import com.hbm.lib.TagIds;
import com.hbm.lib.VanillaIds;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Drops;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Variants;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import static dev.golem.core.dejson.Crafting.rows;
import static dev.golem.core.dejson.Crafting.shaped;

/**
 * Dejson declarations for the custom missile system. Touches no Minecraft class: it runs in a plain JVM from
 * {@code dejson.gradle.kts}.
 *
 * <p>The launchers and pads are folded multiblocks drawn by their renderers, so their blockstates are the empty
 * placeholder. The two launchers and the assembly station had no item renderer in 1.7, so their items are the
 * flat block icon, exactly as 1.7 drew a render-type -1 block in a slot.
 *
 * <p>Every part with a body gets upstream's {@code ItemRenderMissilePart}, which is not a frame of its own: its
 * inventory pose is the bare 1.7 pixel slot — the same GUI frame the launchable missiles use — with the chain
 * {@code T(h/2*s, 0, 0) Rz(135) Rx(145) [T(0, h/8*s) warhead | T(0, h/4*s) fuselage] T(3.5, 14, 0) S(-s)} and a
 * {@code -t/25} degree spin, {@code s = 10 / guiHeight}. That chain is re-expressed as one rotation, translation
 * and scale, which is the shape a context transform takes. In hand and on the ground the part renderer used
 * {@code [T(0.5, 0, 0)] S(0.4)} where the missile frame ends on its own preamble, so each slot carries the
 * quotient of the two.
 */
public final class CustomMissileDeclarations {

    private static final String NS = "hbm";
    private static boolean declared;

    private CustomMissileDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        blocks();
        parts();
        recipes();
    }

    private static void blocks() {
        for (String name : new String[]{"compact_launcher", "launch_table", "launch_pad_large", "launch_pad_rusted"}) {
            Dejson.block(NS, name).rendered("hbm:block/machine_dummy_empty")
                    .states(Variants.product(Variants.uniform("facing", "north", "east", "south", "west"),
                            Variants.uniform("cell", "false", "true"))).register();
        }
        Dejson.block(NS, "machine_missile_assembly").rendered("hbm:block/machine_dummy_empty")
                .states(Variants.uniform("facing", "north", "east", "south", "west")).register();

        for (String core : new String[]{"struct_launcher_core", "struct_launcher_core_large"}) {
            Dejson.block(NS, core).models(Models.cubeAll("hbm:blocks/" + core)).states(Variants.simple()).register();
            Dejson.item(NS, core).shares("hbm:block/" + core).register();
            Drops.self(NS, core);
        }

        // 1.7: no IItemRenderer, render type -1 -> the block's own icon as a flat item
        for (String flat : new String[]{"compact_launcher", "launch_table", "machine_missile_assembly"}) {
            Dejson.item(NS, flat).models(Models.generated("hbm:blocks/" + flat)).register();
        }

        // 1.7 getItemDropped: the launchers give back their core component; the rusted pad gives nothing
        Drops.only(NS, "compact_launcher", "hbm:struct_launcher_core");
        Drops.only(NS, "launch_table", "hbm:struct_launcher_core_large");
        Drops.self(NS, "machine_missile_assembly");
        Drops.self(NS, "launch_pad_large");
        Drops.nothing(NS, "launch_pad_rusted");

        // RenderLaunchPadRusted's item: ItemRenderBase, renderInventory T(0,-1,0) S(3), the silo in the rusted skin
        {
            JsonObject model = machine("hbm:models/weapons/launch_pad_silo.obj", "hbm:textures/models/launchpad/silo_rusted.png");
            model.addProperty("scale", 1.0);
            model.addProperty("yaw", 0.0);
            model.addProperty("rawLegacyTransform", true);
            model.add("guiTranslate", numbers(0, -1, 0));
            model.addProperty("guiScale", 3.0);
            item("launch_pad_rusted", model);
        }
        // RenderLaunchPadLarge's item: renderInventory T(0,-3.75,0) S(1.625); common S(0.5) Ry(90), the pad then
        // the Atlas erector's pad, erector and pivot in the Atlas skin
        {
            JsonObject model = machine("hbm:models/weapons/launch_pad_erector.obj", "hbm:textures/models/launchpad/pad.png");
            model.addProperty("scale", 0.5);
            model.addProperty("yaw", 90.0);
            model.addProperty("rawLegacyTransform", true);
            model.add("guiTranslate", numbers(0, -3.75, 0));
            model.addProperty("guiScale", 1.625);
            model.add("parts", strings("Pad"));
            model.add("translate", numbers(0, 0, 0));
            JsonArray passes = new JsonArray();
            JsonObject atlas = new JsonObject();
            atlas.addProperty("texture", "hbm:textures/models/launchpad/erector_atlas.png");
            atlas.add("parts", strings("Atlas_Pad", "Atlas_Erector", "Atlas_Pivot"));
            atlas.add("translate", numbers(0, 0, 0));
            passes.add(atlas);
            model.add("passes", passes);
            item("launch_pad_large", model);
        }
    }

    private static void parts() {
        for (MissilePartTable.Row row : MissilePartTable.rows().values()) {
            item(row.id(), partModel(row));
        }
        for (int i = 1; i <= 5; i++) {
            Dejson.item(NS, "mp_c_" + i).models(Models.generated("hbm:items/mp_c_" + i)).register();
        }
        // 1.7 ItemRenderMissile draws the assembled missile from its parts in every slot
        com.hbm.items.ItemRendererDeclarations.model("missile_custom", "missile_custom", "weapon");
    }

    /** One part's {@code hbm:machine_obj} model on the missile frame; see the class comment for the algebra. */
    static JsonObject partModel(MissilePartTable.Row row) {
        String type = row.id().startsWith("mp_warhead") ? "warhead"
                : row.id().startsWith("mp_fuselage") ? "fuselage" : "other";
        float height = row.guiHeight() == 0F ? 4F : row.guiHeight();
        float s = 10F / height;

        Matrix3f rot = new Matrix3f().rotateZ((float) Math.toRadians(135)).rotateX((float) Math.toRadians(145));
        Vector3f euler = rot.getEulerAnglesXYZ(new Vector3f());
        // T(a,0,0) R = R T(R^-1 (a,0,0)); everything after R is already in R's frame
        Vector3f lead = new Matrix3f(rot).transpose().transform(new Vector3f(height / 2F * s, 0F, 0F));
        float lift = type.equals("warhead") ? height / 8F * s : type.equals("fuselage") ? height / 4F * s : 0F;
        Vector3f translation = lead.add(3.5F, 14F + lift, 0F);

        JsonObject model = machine("hbm:" + row.mesh(), "hbm:" + row.skin());
        model.addProperty("frame", "missile");
        model.addProperty("scale", 1.0);
        model.addProperty("yaw", 0.0);
        model.addProperty("rawLegacyTransform", true);
        model.add("itemTranslate", numbers(0, 0, 0));
        model.add("itemPreTranslate", numbers(0, 0, 0));
        model.addProperty("ty", 0.0);
        // glRotatef(System.currentTimeMillis() / 25 % 360, 0, -1, 0)
        model.addProperty("guiSpinSpeed", -1D / 25D);

        JsonObject transforms = new JsonObject();
        transforms.add("gui", transform((float) Math.toDegrees(euler.x), (float) Math.toDegrees(euler.y),
                (float) Math.toDegrees(euler.z), translation.x, translation.y, translation.z, -s));
        // missile frame preamble: first person T(0.5, 0.25, 0) S(0.1), third person T(0.5, -0.25, 0) S(0.15),
        // ground/frame S(0.15); the part renderer: equipped T(0.5, 0, 0) S(0.4), entity S(0.4)
        transforms.add("first_person", transform(0, 0, 0, 0, -0.25F / 0.1F, 0, 0.4F / 0.1F));
        transforms.add("third_person", transform(0, 0, 0, 0, 0.25F / 0.15F, 0, 0.4F / 0.15F));
        transforms.add("non_gui", transform(0, 0, 0, 0, 0.25F / 0.15F, 0, 0.4F / 0.15F));
        transforms.add("ground", transform(0, 0, 0, 0, 0, 0, 0.4F / 0.15F));
        transforms.add("fixed", transform(0, 0, 0, 0, 0, 0, 0.4F / 0.15F));
        model.add("contextTransforms", transforms);
        return model;
    }

    private static JsonObject transform(float rx, float ry, float rz, float tx, float ty, float tz, float scale) {
        JsonObject t = new JsonObject();
        t.add("rotation", numbers(rx, ry, rz));
        t.add("translation", numbers(tx, ty, tz));
        // Widened on purpose: a Float property is written with Float.toString's short digits, which read back
        // as a different double, so the shipped file would no longer equal its declaration (dejson_item_parity).
        t.addProperty("scale", (double) scale);
        return t;
    }

    private static void recipes() {
        // 1.7 WeaponRecipes: rubber, a circuit tier, a steel scaffold
        String[] circuits = {ItemIds.circuit_vacuum_tube, ItemIds.circuit_analog, ItemIds.circuit_basic,
                ItemIds.circuit_advanced, ItemIds.circuit_bismoid};
        for (int i = 0; i < 5; i++) {
            shaped(NS, "mp_c_" + (i + 1), "hbm:mp_c_" + (i + 1), rows("P", "C", "S"),
                    "P", TagIds.ingots_any_rubber, "C", circuits[i], "S", ItemIds.steel_scaffold);
        }
        // 1.7 CraftingManager
        shaped(NS, "struct_launcher_core", "hbm:struct_launcher_core", rows("SCS", "SIS", "BEB"),
                "S", ItemIds.steel_scaffold, "I", VanillaIds.iron_bars, "C", ItemIds.circuit_basic,
                "B", ItemIds.struct_launcher, "E", ItemIds.battery_lead);
        shaped(NS, "struct_launcher_core_large", "hbm:struct_launcher_core_large", rows("SIS", "ICI", "BEB"),
                "S", ItemIds.circuit_advanced, "I", VanillaIds.iron_bars, "C", ItemIds.circuit_advanced,
                "B", ItemIds.struct_launcher, "E", ItemIds.battery_lead);
        // machine_missile_assembly ("PWP", "SSS", "CCC") needs the wrench: declared with it in MeleeKitDeclarations.
    }

    private static JsonObject machine(String obj, String texture) {
        JsonObject model = new JsonObject();
        model.addProperty("type", "hbm:machine_obj");
        model.addProperty("obj", obj);
        model.addProperty("texture", texture);
        return model;
    }

    private static void item(String name, JsonObject model) {
        Dejson.item(NS, name).shares("hbm:item/machine_obj_base").register();
        Dejson.itemDefinition("hbm:item/" + name).add("model", model);
        Dejson.itemDefinition("hbm:item/" + name).addProperty("oversized_in_gui", true);
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
