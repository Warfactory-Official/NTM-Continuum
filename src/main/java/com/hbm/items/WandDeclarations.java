package com.hbm.items;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.core.dejson.Dejson;

/**
 * The typed OBJ item definition of {@code missile_test}; the wands' models are stated on their registrations
 * ({@link ModWandItems}). Touches no Minecraft class.
 *
 * <p>{@code missile_test} is drawn by 1.7 {@code ItemRenderMissileGeneric} as the micro body wearing
 * {@code missile_test.png} ({@code generateStandard}, tier-0 GUI table), i.e. exactly the {@code missile_micro} item
 * definition with the test skin. It has no recipe upstream either.
 */
public final class WandDeclarations {

    private static final String NS = "hbm";
    private static boolean declared;

    private WandDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        missileTest();
    }

    /** The {@code missile_micro} definition (tier-0 table: scale 3.75, offset 10.75) with the test skin. */
    private static void missileTest() {
        JsonObject model = new JsonObject();
        model.addProperty("type", "hbm:machine_obj");
        model.addProperty("frame", "missile");
        model.addProperty("obj", "hbm:models/missiles/missile_micro.obj");
        model.addProperty("texture", "hbm:textures/models/missiles/missile_test.png");
        model.addProperty("noCull", true);
        model.addProperty("scale", 1.0);
        model.addProperty("yaw", 0.0);
        model.addProperty("rawLegacyTransform", true);
        model.add("itemTranslate", numbers(0, 0, 0));
        model.add("itemPreTranslate", numbers(0, 0, 0));
        model.addProperty("ty", 0.0);
        // 1.7 spins these at currentTimeMillis() / 15 % 360
        model.addProperty("guiSpinSpeed", 1.0 / 15.0);

        JsonObject gui = new JsonObject();
        gui.add("rotation", numbers(0, 0, 135));
        gui.add("translation", numbers(0, (-16.0 + 10.75) * 3.75, 0));
        gui.addProperty("scale", 3.75);
        JsonObject transforms = new JsonObject();
        transforms.add("gui", gui);
        model.add("contextTransforms", transforms);

        Dejson.item(NS, "missile_test").shares("hbm:item/machine_obj_base").register();
        Dejson.itemDefinition("hbm:item/missile_test").add("model", model);
        Dejson.itemDefinition("hbm:item/missile_test").addProperty("oversized_in_gui", true);
    }

    private static JsonArray numbers(double... values) {
        JsonArray array = new JsonArray();
        for (double value : values) array.add(value);
        return array;
    }
}
