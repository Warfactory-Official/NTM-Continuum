package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.golem.core.render.ConnectedTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The {@code hbm_ct} harness query, read by {@code ConnectedTextures17}: the connected-texture counts, and for each
 * position in {@code {"at":[[x,y,z],...]}} the block, the 24 quarter types Core draws there (client level) and the
 * sheet pair of each face.
 */
public final class ConnectedTextureHarnessQueries {

    private ConnectedTextureHarnessQueries() {}

    public static void register() {
        dev.golem.core.harness.HarnessQueryRegistry.register("hbm_ct", ConnectedTextureHarnessQueries::query);
    }

    private static JsonObject query(Minecraft mc, JsonObject params) {
        JsonObject json = new JsonObject();
        json.addProperty("registered", ConnectedTextures.registered());
        json.addProperty("wrapped", ConnectedTextures.wrapped());
        if (mc.level == null || !params.has("at")) return json;
        JsonArray out = new JsonArray();
        for (JsonElement e : params.getAsJsonArray("at")) {
            JsonArray p = e.getAsJsonArray();
            BlockPos pos = new BlockPos(p.get(0).getAsInt(), p.get(1).getAsInt(), p.get(2).getAsInt());
            BlockState state = mc.level.getBlockState(pos);
            JsonObject at = new JsonObject();
            at.addProperty("block", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
            int[] types = ConnectedTextures.fragmentsAt(mc.level, pos);
            if (types != null) {
                JsonArray t = new JsonArray();
                for (int type : types) t.add(type);
                at.add("types", t);
                JsonObject sheets = new JsonObject();
                for (Direction face : Direction.values()) {
                    String[] pair = ConnectedTextures.sheetsOf(state, face);
                    JsonArray a = new JsonArray();
                    a.add(pair[0]);
                    a.add(pair[1]);
                    sheets.add(face.getName(), a);
                }
                at.add("sheets", sheets);
            }
            out.add(at);
        }
        json.add("at", out);
        return json;
    }
}
