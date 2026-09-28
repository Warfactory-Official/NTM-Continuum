package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.config.WorldConfig;
import com.hbm.entity.projectile.EntityMeteor;
import com.hbm.handler.BossSpawnHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Harness query {@code hbm_worldgen} (dev clients with the harness on), run on the integrated server thread in the
 * player's dimension:
 * <ul>
 *   <li>{@code mode=count}, box {@code x0 z0 x1 z1 y0 y1}, optional {@code ids} (comma list, exact) or {@code contains} (substrings): how many of each block id
 *       stand in the box (chunks are loaded/generated as needed), plus the number of chunks;</li>
 *   <li>{@code mode=find}, box + {@code id}, {@code limit}: positions of that block in the box, top-down;</li>
 *   <li>{@code mode=biome}, {@code x y z}: the biome id there; {@code mode=top}, {@code x z}: the
 *       {@code MOTION_BLOCKING} surface y there, the block under it and the biome;</li>
 *   <li>{@code mode=meteor}: the meteor scheduler — optional {@code strikeChance} (sets
 *       {@code WorldConfig.meteorStrikeChance}), {@code shower} (sets the shower ticks), {@code strike=true} (one
 *       strike roll's charm check + meteor on the player); returns the settings and the falling meteors.</li>
 * </ul>
 */
public final class WorldFeatureHarnessQueries {

    private WorldFeatureHarnessQueries() {}

    public static void register() {
        dev.golem.core.harness.HarnessQueryRegistry.register("hbm_worldgen", WorldFeatureHarnessQueries::query);
    }

    private static JsonObject query(Minecraft mc, JsonObject params) {
        MinecraftServer server = mc.getSingleplayerServer();
        JsonObject json = new JsonObject();
        if (server == null || mc.player == null) {
            json.addProperty("error", "no integrated server");
            return json;
        }
        var dimension = mc.player.level().dimension();
        var playerId = mc.player.getUUID();
        try {
            return server.submit(() -> onServer(server, server.getLevel(dimension), playerId, params)).get(120, TimeUnit.SECONDS);
        } catch (Exception e) {
            json.addProperty("error", e.toString());
            return json;
        }
    }

    private static int i(JsonObject p, String key, int def) {
        return p.has(key) ? p.get(key).getAsInt() : def;
    }

    private static String id(BlockState state) {
        return String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    private static JsonObject onServer(MinecraftServer server, ServerLevel level, java.util.UUID playerId, JsonObject p) {
        JsonObject json = new JsonObject();
        String mode = p.has("mode") ? p.get("mode").getAsString() : "count";
        switch (mode) {
            case "count", "find" -> {
                int x0 = i(p, "x0", 0), z0 = i(p, "z0", 0), x1 = i(p, "x1", 15), z1 = i(p, "z1", 15);
                int y0 = Math.max(level.getMinY(), i(p, "y0", level.getMinY()));
                int y1 = Math.min(level.getMaxY() - 1, i(p, "y1", level.getMaxY() - 1));
                Set<String> ids = new HashSet<>();
                if (p.has("ids")) for (String s : p.get("ids").getAsString().split(",")) ids.add(s.trim());
                java.util.List<String> contains = new java.util.ArrayList<>();
                if (p.has("contains")) for (String s : p.get("contains").getAsString().split(",")) contains.add(s.trim());
                String find = p.has("id") ? p.get("id").getAsString() : null;
                int limit = i(p, "limit", 64);
                Map<String, Integer> counts = new TreeMap<>();
                JsonArray found = new JsonArray();
                int chunks = 0;
                for (int cx = x0 >> 4; cx <= x1 >> 4; cx++) {
                    for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) {
                        level.getChunk(cx, cz);
                        chunks++;
                    }
                }
                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
                for (int y = y1; y >= y0; y--) {
                    for (int x = x0; x <= x1; x++) {
                        for (int z = z0; z <= z1; z++) {
                            String block = id(level.getBlockState(pos.set(x, y, z)));
                            if (find != null) {
                                if (block.equals(find) && found.size() < limit) {
                                    JsonArray at = new JsonArray();
                                    at.add(x);
                                    at.add(y);
                                    at.add(z);
                                    found.add(at);
                                }
                            } else if ((ids.isEmpty() && contains.isEmpty()) || ids.contains(block)
                                    || contains.stream().anyMatch(block::contains)) {
                                counts.merge(block, 1, Integer::sum);
                            }
                        }
                    }
                }
                json.addProperty("chunks", chunks);
                if (find != null) json.add("found", found);
                else {
                    JsonObject c = new JsonObject();
                    counts.forEach(c::addProperty);
                    json.add("counts", c);
                }
            }
            case "top" -> {
                int x = i(p, "x", 0), z = i(p, "z", 0);
                level.getChunk(x >> 4, z >> 4);
                int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
                json.addProperty("y", y);
                json.addProperty("below", id(level.getBlockState(new BlockPos(x, y - 1, z))));
                json.addProperty("biome", level.getBiome(new BlockPos(x, y, z)).unwrapKey()
                        .map(k -> dev.golem.core.Ids.keyId(k).toString()).orElse("?"));
            }
            case "features" -> {
                // the biome's placed features per generation step, in order (all of them, vanilla included), and the
                // index each has in the level's global per-step feature order (which seeds its placement)
                var biome = level.getBiome(new BlockPos(i(p, "x", 0), i(p, "y", 64), i(p, "z", 0)));
                JsonArray steps = new JsonArray();
                for (var step : biome.value().getGenerationSettings().features()) {
                    JsonArray ids = new JsonArray();
                    for (var holder : step) ids.add(holder.unwrapKey().map(k -> dev.golem.core.Ids.keyId(k).toString()).orElse("?"));
                    steps.add(ids);
                }
                json.add("steps", steps);
            }
            case "biome" -> json.addProperty("biome", level.getBiome(new BlockPos(i(p, "x", 0), i(p, "y", 64), i(p, "z", 0)))
                    .unwrapKey().map(k -> dev.golem.core.Ids.keyId(k).toString()).orElse("?"));
            case "meteor" -> {
                if (p.has("strikeChance")) WorldConfig.meteorStrikeChance = p.get("strikeChance").getAsInt();
                if (p.has("shower")) BossSpawnHandler.meteorShower = p.get("shower").getAsInt();
                var player = server.getPlayerList().getPlayer(playerId);
                if (p.has("strike") && p.get("strike").getAsBoolean() && player != null) {
                    json.addProperty("struck", BossSpawnHandler.strike(level, player) != null);
                }
                json.addProperty("strikeChance", WorldConfig.meteorStrikeChance);
                json.addProperty("showerChance", WorldConfig.meteorShowerChance);
                json.addProperty("shower", BossSpawnHandler.meteorShower);
                JsonArray meteors = new JsonArray();
                if (player != null) {
                    for (EntityMeteor m : level.getEntitiesOfClass(EntityMeteor.class, new AABB(player.blockPosition()).inflate(256))) {
                        JsonObject o = new JsonObject();
                        o.addProperty("x", m.getX());
                        o.addProperty("y", m.getY());
                        o.addProperty("z", m.getZ());
                        o.addProperty("safe", m.safe);
                        meteors.add(o);
                    }
                }
                json.add("meteors", meteors);
            }
            default -> json.addProperty("error", "unknown mode " + mode);
        }
        return json;
    }
}
