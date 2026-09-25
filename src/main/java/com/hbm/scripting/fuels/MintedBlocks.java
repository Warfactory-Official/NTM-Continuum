package com.hbm.scripting.fuels;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.Constants;
import com.hbm.scripting.ScriptedContent;
import dev.golem.core.world.DataPaths;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Data-pack side of script-minted blocks: self drop, pickaxe tag, English name. */
final class MintedBlocks {

    private static final List<String> PICKAXE = new ArrayList<>();
    private static boolean hooked;

    private MintedBlocks() {}

    static synchronized void declare(String id, String name, boolean dropsWhenExploded) {
        String block = Constants.MOD_ID + ":" + id;
        ScriptedContent.lang("block." + Constants.MOD_ID + "." + id, name);
        ScriptedContent.data(out -> out.json(Constants.MOD_ID, DataPaths.blockLootTable(id), selfDrop(block, dropsWhenExploded)));
        PICKAXE.add(block);
        if (hooked) return;
        hooked = true;
        ScriptedContent.data(out -> {
            JsonObject tag = new JsonObject();
            tag.addProperty("replace", false);
            JsonArray values = new JsonArray();
            PICKAXE.forEach(values::add);
            tag.add("values", values);
            out.json("minecraft", DataPaths.blockTag("mineable/pickaxe"), tag);
        });
    }

    private static JsonObject selfDrop(String block, boolean dropsWhenExploded) {
        JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", block);
        JsonArray entries = new JsonArray();
        entries.add(entry);
        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1.0);
        pool.addProperty("bonus_rolls", 0.0);
        pool.add("entries", entries);
        if (!dropsWhenExploded) {
            JsonObject survives = new JsonObject();
            survives.addProperty("condition", "minecraft:survives_explosion");
            JsonArray conditions = new JsonArray();
            conditions.add(survives);
            pool.add("conditions", conditions);
        }
        JsonArray pools = new JsonArray();
        pools.add(pool);
        JsonObject table = new JsonObject();
        table.addProperty("type", "minecraft:block");
        table.add("pools", pools);
        return table;
    }

    static JsonObject blockState(Map<String, String> variants) {
        JsonObject all = new JsonObject();
        variants.forEach((state, model) -> {
            JsonObject variant = new JsonObject();
            variant.addProperty("model", model);
            all.add(state, variant);
        });
        JsonObject blockState = new JsonObject();
        blockState.add("variants", all);
        return blockState;
    }

    static JsonObject retextured(String parent, Map<String, String> textures) {
        JsonObject slots = new JsonObject();
        textures.forEach(slots::addProperty);
        JsonObject model = new JsonObject();
        model.addProperty("parent", parent);
        model.add("textures", slots);
        return model;
    }
}
