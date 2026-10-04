package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.core.world.Spawns;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * The {@code hbm_mob_spawns} harness query (fix wave phase 7, History §19ej-7): the natural spawn entries the integrated
 * server's biome {@code biome} (default: the biome at the player) lists for every category, after biome modifiers.
 * Inert unless the harness listener is running.
 */
public final class MobHarnessQueries {

    private MobHarnessQueries() {}

    public static void register() {
        dev.golem.core.harness.HarnessQueryRegistry.register("hbm_mob_spawns", MobHarnessQueries::spawns);
    }

    private static JsonObject spawns(Minecraft mc, JsonObject params) {
        JsonObject json = new JsonObject();
        var server = mc.getSingleplayerServer();
        if (mc.player == null || server == null) return json;
        java.util.UUID id = mc.player.getUUID();
        java.util.concurrent.CompletableFuture<JsonObject> result = new java.util.concurrent.CompletableFuture<>();
        server.execute(() -> {
            try {
                JsonObject s = new JsonObject();
                var player = server.getPlayerList().getPlayer(id);
                var level = player != null ? player.level() : server.overworld();
                net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome;
                if (params.has("biome")) {
                    biome = dev.golem.core.world.Biomes.holder(server.registryAccess(), ResourceKey.create(Registries.BIOME,
                            dev.golem.core.Ids.parse(params.get("biome").getAsString())));
                } else {
                    biome = level.getBiome(player.blockPosition());
                }
                s.addProperty("biome", biome.unwrapKey().map(k -> k.toString()).orElse("?"));
                for (MobCategory category : MobCategory.values()) {
                    JsonArray list = new JsonArray();
                    for (Spawns.SpawnEntry entry : Spawns.entries(biome.value(), category)) {
                        JsonObject e = new JsonObject();
                        e.addProperty("type", EntityType.getKey(entry.type()).toString());
                        e.addProperty("weight", entry.weight());
                        e.addProperty("min", entry.minCount());
                        e.addProperty("max", entry.maxCount());
                        list.add(e);
                    }
                    s.add(category.getName(), list);
                }
                result.complete(s);
            } catch (Throwable t) {
                JsonObject error = new JsonObject();
                error.addProperty("error", t.toString());
                result.complete(error);
            }
        });
        try {
            json.add("server", result.get(5, java.util.concurrent.TimeUnit.SECONDS));
        } catch (Exception e) {
            json.addProperty("error", e.toString());
        }
        return json;
    }
}
