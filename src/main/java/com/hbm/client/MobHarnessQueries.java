package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.core.Ids;
import dev.golem.core.harness.HarnessQueryRegistry;
import dev.golem.core.world.Biomes;
import dev.golem.core.world.Spawns;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;

/**
 * The {@code hbm_mob_spawns} harness query: the natural spawn entries the integrated
 * server's biome {@code biome} (default: the biome at the player) lists for every category, after biome modifiers.
 * Inert unless the harness listener is running.
 */
public final class MobHarnessQueries {

    private MobHarnessQueries() {}

    public static void register() {
        HarnessQueryRegistry.register("hbm_mob_spawns", MobHarnessQueries::spawns);
    }

    private static JsonObject spawns(Minecraft mc, JsonObject params) {
        JsonObject json = new JsonObject();
        var server = mc.getSingleplayerServer();
        if (mc.player == null || server == null) return json;
        UUID id = mc.player.getUUID();
        CompletableFuture<JsonObject> result = new CompletableFuture<>();
        server.execute(() -> {
            try {
                JsonObject s = new JsonObject();
                var player = server.getPlayerList().getPlayer(id);
                var level = player != null ? player.level() : server.overworld();
                Holder<Biome> biome;
                if (params.has("biome")) {
                    biome = Biomes.holder(server.registryAccess(), ResourceKey.create(Registries.BIOME,
                            Ids.parse(params.get("biome").getAsString())));
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
            json.add("server", result.get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
            json.addProperty("error", e.toString());
        }
        return json;
    }
}
