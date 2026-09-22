package com.hbm.items;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.core.dejson.Dejson;

/** Original OBJ item payloads, emitted through Dejson without changing their rendering frames. */
final class ContentItemModels {
    private ContentItemModels() {}

    static void declare() {
        {
            JsonObject model = machine("hbm:models/machines/condenser.obj", "hbm:textures/models/machines/compressor_compact.png");
            model.addProperty("scale", 0.35);
            model.addProperty("yaw", 90);
            model.addProperty("ty", 0.0);
            JsonObject special = new JsonObject();
            special.addProperty("type", "minecraft:special");
            special.addProperty("base", "hbm:item/machine_obj_base");
            special.add("model", model);
            item("machine_compressor_compact", special);
        }
        {
            JsonObject model = machine("hbm:models/machines/tower_small.obj", "hbm:textures/models/machines/tower_small.png");
            model.addProperty("noCull", true);
            model.addProperty("scale", 0.25);
            model.addProperty("yaw", 0);
            model.addProperty("ty", 0.0);
            model.addProperty("rawLegacyTransform", true);
            model.addProperty("frame", "machine");
            model.add("guiTranslate", numbers(0.0, -4.0, 0.0));
            model.addProperty("guiScale", 3.0);
            item("machine_tower_small", model);
        }
        {
            JsonObject model = machine("hbm:models/machines/tower_large.obj", "hbm:textures/models/machines/tower_large.png");
            model.addProperty("noCull", true);
            model.addProperty("scale", 0.25);
            model.addProperty("yaw", 0);
            model.addProperty("ty", 0.0);
            model.addProperty("rawLegacyTransform", true);
            model.addProperty("frame", "machine");
            model.add("guiTranslate", numbers(0.0, -3.0, 0.0));
            model.addProperty("guiScale", 3.8);
            item("machine_tower_large", model);
        }
        {
            JsonObject model = machine("hbm:models/blocks/rail_standard_buffer.obj", "hbm:textures/blocks/rail_standard_buffer.png");
            model.addProperty("scale", 0.3);
            model.addProperty("yaw", 90.0);
            model.addProperty("pitch", 0.0);
            model.addProperty("rawLegacyTransform", true);
            model.add("itemTranslate", numbers(0.5, 0.4375, 0.5));
            model.add("itemPreTranslate", numbers(0.0, 0.0, 0.0));
            JsonObject special = new JsonObject();
            special.addProperty("type", "minecraft:special");
            special.addProperty("base", "hbm:item/machine_obj_base");
            special.add("model", model);
            item("rail_large_buffer", special);
        }
        {
            JsonObject model = machine("hbm:models/blocks/rail_standard_bend.obj", "hbm:textures/blocks/rail_standard_straight.png");
            model.addProperty("scale", 0.3);
            model.addProperty("yaw", 45.0);
            model.addProperty("pitch", 60.0);
            model.addProperty("rawLegacyTransform", true);
            model.add("itemTranslate", numbers(0.5, 0.5, 0.5));
            model.add("itemPreTranslate", numbers(0.6, -0.519615242, 0.3));
            JsonObject special = new JsonObject();
            special.addProperty("type", "minecraft:special");
            special.addProperty("base", "hbm:item/machine_obj_base");
            special.add("model", model);
            item("rail_large_curve", special);
        }
        {
            JsonObject model = machine("hbm:models/blocks/rail_standard_bend_wide.obj", "hbm:textures/blocks/rail_standard_straight.png");
            model.addProperty("scale", 0.225);
            model.addProperty("yaw", 45.0);
            model.addProperty("pitch", 60.0);
            model.addProperty("rawLegacyTransform", true);
            model.add("itemTranslate", numbers(0.5, 0.5, 0.5));
            model.add("itemPreTranslate", numbers(0.675, -0.584567148, 0.3375));
            JsonObject special = new JsonObject();
            special.addProperty("type", "minecraft:special");
            special.addProperty("base", "hbm:item/machine_obj_base");
            special.add("model", model);
            item("rail_large_curve_7", special);
        }
        {
            JsonObject model = machine("hbm:models/blocks/rail_standard_bend_wide9.obj", "hbm:textures/blocks/rail_standard_straight.png");
            model.addProperty("scale", 0.175);
            model.addProperty("yaw", 45.0);
            model.addProperty("pitch", 60.0);
            model.addProperty("rawLegacyTransform", true);
            model.add("itemTranslate", numbers(0.5, 0.5, 0.5));
            model.add("itemPreTranslate", numbers(0.7, -0.606217783, 0.35));
            JsonObject special = new JsonObject();
            special.addProperty("type", "minecraft:special");
            special.addProperty("base", "hbm:item/machine_obj_base");
            special.add("model", model);
            item("rail_large_curve_9", special);
        }
        {
            JsonObject model = machine("hbm:models/blocks/rail_standard_ramp.obj", "hbm:textures/blocks/rail_standard_straight.png");
            model.addProperty("scale", 0.3);
            model.addProperty("yaw", 90.0);
            model.addProperty("pitch", 0.0);
            model.addProperty("rawLegacyTransform", true);
            model.add("itemTranslate", numbers(0.5, 0.4375, 0.5));
            model.add("itemPreTranslate", numbers(0.0, 0.0, 0.0));
            JsonObject special = new JsonObject();
            special.addProperty("type", "minecraft:special");
            special.addProperty("base", "hbm:item/machine_obj_base");
            special.add("model", model);
            item("rail_large_ramp", special);
        }
        {
            JsonObject model = machine("hbm:models/blocks/rail_standard.obj", "hbm:textures/blocks/rail_standard_straight.png");
            model.addProperty("scale", 0.3);
            model.addProperty("yaw", 90.0);
            model.addProperty("pitch", 0.0);
            model.addProperty("rawLegacyTransform", true);
            model.add("itemTranslate", numbers(0.5, 0.4375, 0.5));
            model.add("itemPreTranslate", numbers(0.0, 0.0, 0.0));
            JsonObject special = new JsonObject();
            special.addProperty("type", "minecraft:special");
            special.addProperty("base", "hbm:item/machine_obj_base");
            special.add("model", model);
            item("rail_large_straight", special);
        }
        {
            JsonObject model = machine("hbm:models/blocks/rail_standard_short.obj", "hbm:textures/blocks/rail_standard_straight.png");
            model.addProperty("scale", 0.7);
            model.addProperty("yaw", 90.0);
            model.addProperty("pitch", 0.0);
            model.addProperty("rawLegacyTransform", true);
            model.add("itemTranslate", numbers(0.5, 0.4375, 0.5));
            model.add("itemPreTranslate", numbers(0.0, 0.0, 0.0));
            JsonObject special = new JsonObject();
            special.addProperty("type", "minecraft:special");
            special.addProperty("base", "hbm:item/machine_obj_base");
            special.add("model", model);
            item("rail_large_straight_short", special);
        }
        {
            JsonObject model = machine("hbm:models/blocks/rail_standard_switch.obj", "hbm:textures/blocks/rail_standard_straight.png");
            model.addProperty("scale", 0.1);
            model.addProperty("yaw", 90.0);
            model.addProperty("pitch", 0.0);
            model.addProperty("rawLegacyTransform", true);
            model.add("itemTranslate", numbers(0.5, 0.4375, 0.3125));
            model.add("itemPreTranslate", numbers(0.0, 0.0, 0.0));
            model.add("exclude", strings("SignTurn", "SignStraight"));
            JsonObject special = new JsonObject();
            special.addProperty("type", "minecraft:special");
            special.addProperty("base", "hbm:item/machine_obj_base");
            special.add("model", model);
            item("rail_large_switch", special);
        }
        {
            JsonObject model = machine("hbm:models/blocks/rail_standard_switch_flipped.obj", "hbm:textures/blocks/rail_standard_straight.png");
            model.addProperty("scale", 0.1);
            model.addProperty("yaw", 90.0);
            model.addProperty("pitch", 0.0);
            model.addProperty("rawLegacyTransform", true);
            model.add("itemTranslate", numbers(0.5, 0.4375, 0.5));
            model.add("itemPreTranslate", numbers(0.0, 0.0, 0.0));
            model.add("exclude", strings("SignTurn", "SignStraight"));
            JsonObject special = new JsonObject();
            special.addProperty("type", "minecraft:special");
            special.addProperty("base", "hbm:item/machine_obj_base");
            special.add("model", model);
            item("rail_large_switch_flipped", special);
        }
        {
            JsonObject model = machine("hbm:models/blocks/rail_narrow_bend.obj", "hbm:textures/blocks/rail_narrow_neo.png");
            model.addProperty("scale", 0.2);
            model.addProperty("yaw", 0.0);
            model.addProperty("pitch", 0.0);
            model.addProperty("rawLegacyTransform", true);
            model.add("itemTranslate", numbers(0.5, 0.5, 0.5));
            model.add("itemPreTranslate", numbers(0.5, -0.0125, -0.3));
            JsonObject special = new JsonObject();
            special.addProperty("type", "minecraft:special");
            special.addProperty("base", "hbm:item/machine_obj_base");
            special.add("model", model);
            item("rail_narrow_curve", special);
        }
        {
            JsonObject model = machine("hbm:models/blocks/rail_narrow.obj", "hbm:textures/blocks/rail_narrow_neo.png");
            model.addProperty("scale", 1.0);
            model.addProperty("yaw", 0.0);
            model.addProperty("pitch", 0.0);
            model.addProperty("rawLegacyTransform", true);
            model.add("itemTranslate", numbers(0.5, 0.4375, 0.5));
            model.add("itemPreTranslate", numbers(0.0, 0.0, 0.0));
            JsonObject special = new JsonObject();
            special.addProperty("type", "minecraft:special");
            special.addProperty("base", "hbm:item/machine_obj_base");
            special.add("model", model);
            item("rail_narrow_straight", special);
        }
    }

    private static JsonObject machine(String obj, String texture) {
        JsonObject model = new JsonObject();
        model.addProperty("type", "hbm:machine_obj");
        model.addProperty("obj", obj);
        model.addProperty("texture", texture);
        return model;
    }

    private static void item(String name, JsonObject model) {
        // shares emits only the definition; legacy-assets derives the correct legacy frame.
        Dejson.item("hbm", name).shares("hbm:item/machine_obj_base").register();
        Dejson.itemDefinition("hbm:item/" + name).add("model", model);
        if (name.startsWith("rail_")) {
            Dejson.itemDefinition("hbm:item/" + name).addProperty("oversized_in_gui", true);
        }
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
