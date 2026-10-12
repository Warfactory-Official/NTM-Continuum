package com.hbm.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.TreeMap;

/**
 * JSON text that reads the same on every node, for the GameTests that pin a SHA-256 of a table's JSON. A codec's field
 * order is DataFixerUpper's, and 1.20.1's DFU writes a record's fields in a different order than 1.21.1's and 26.1's
 * (same fields, same values); object keys are therefore sorted, recursively. Array order is data and is kept.
 */
final class CanonicalJson {

    private CanonicalJson() {}

    static String text(JsonElement json) {
        return sorted(json).toString();
    }

    static JsonElement sorted(JsonElement json) {
        if (json instanceof JsonObject object) {
            TreeMap<String, JsonElement> keys = new TreeMap<>();
            for (var entry : object.entrySet()) keys.put(entry.getKey(), sorted(entry.getValue()));
            JsonObject out = new JsonObject();
            keys.forEach(out::add);
            return out;
        }
        if (json instanceof JsonArray array) {
            JsonArray out = new JsonArray();
            for (JsonElement element : array) out.add(sorted(element));
            return out;
        }
        return json;
    }
}
