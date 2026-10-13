package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import dev.golem.core.client.Fogs;
import dev.golem.core.data.CompatChicken;
import dev.golem.core.harness.HarnessQueryRegistry;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * The {@code hbm_hooks} harness query on the integrated server and the client:
 * <ul>
 *   <li>{@code set_soot} / {@code set_metal}: the soot / heavy metal of the player's pollution region;</li>
 *   <li>{@code find_biome}: the nearest position of that biome id (x, z within 6400 blocks), for the duck check;</li>
 *   <li>always: the player's effects, the server pollution there, the client's synced soot and {@code renderSoot},
 *       the smog's fog end and colour share, and for every {@code CompatChicken} within 32 blocks (256 vertically) whether it lays the
 *       plain egg.</li>
 * </ul>
 * Inert unless the harness listener is running.
 */
public final class HooksHarnessQueries {

    private HooksHarnessQueries() {}

    public static void register() {
        HarnessQueryRegistry.register("hbm_hooks", HooksHarnessQueries::hooks);
    }

    private static JsonObject hooks(Minecraft mc, JsonObject params) {
        JsonObject json = new JsonObject();
        var server = mc.getSingleplayerServer();
        if (mc.player == null || server == null || mc.level == null) return json;
        UUID id = mc.player.getUUID();
        CompletableFuture<JsonObject> result = new CompletableFuture<>();
        server.execute(() -> {
            try {
                JsonObject s = new JsonObject();
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (player != null && player.level() instanceof ServerLevel level) {
                    BlockPos pos = player.blockPosition();
                    if (params.has("set_soot")) PollutionHandler.setPollution(level, pos, PollutionType.SOOT, params.get("set_soot").getAsFloat());
                    if (params.has("set_metal")) PollutionHandler.setPollution(level, pos, PollutionType.HEAVYMETAL, params.get("set_metal").getAsFloat());
                    s.addProperty("soot", PollutionHandler.getPollution(level, pos, PollutionType.SOOT));
                    s.addProperty("metal", PollutionHandler.getPollution(level, pos, PollutionType.HEAVYMETAL));
                    JsonArray effects = new JsonArray();
                    for (var effect : player.getActiveEffects()) effects.add(effect.toString());
                    s.add("effects", effects);
                    s.addProperty("alive", player.isAlive());
                    if (params.has("find_biome")) {
                        String biome = params.get("find_biome").getAsString();
                        var found = level.findClosestBiome3d(h -> h.unwrapKey().map(k -> k.toString().endsWith(" / " + biome + "]")).orElse(false),
                                pos, 6400, 32, 64);
                        if (found != null) {
                            s.addProperty("biome_x", found.getFirst().getX());
                            s.addProperty("biome_y", found.getFirst().getY());
                            s.addProperty("biome_z", found.getFirst().getZ());
                        }
                    }
                    JsonArray chickens = new JsonArray();
                    for (var chicken : level.getEntitiesOfClass(CompatChicken.class, player.getBoundingBox().inflate(32, 256, 32))) {
                        JsonObject c = new JsonObject();
                        c.addProperty("type", chicken.getType().toString());
                        c.addProperty("plain_egg", chicken.compatLaysPlainEgg());
                        c.addProperty("biome", level.getBiome(chicken.blockPosition()).unwrapKey().map(k -> k.toString()).orElse("?"));
                        chickens.add(c);
                    }
                    s.add("chickens", chickens);
                }
                result.complete(s);
            } catch (Throwable t) {
                JsonObject error = new JsonObject();
                error.addProperty("error", t.toString());
                result.complete(error);
            }
        });
        try {
            json.add("server", result.get(10, TimeUnit.SECONDS));
        } catch (Exception e) {
            json.addProperty("error", e.toString());
        }
        JsonObject c = new JsonObject();
        c.addProperty("synced_soot", SmogFog.pollution[0]);
        c.addProperty("render_soot", SmogFog.renderSoot);
        float far = Fogs.far(mc.getCameraEntity(), Fogs.renderDistanceBlocks());
        c.addProperty("fog_far", Float.isNaN(far) ? -1F : far);
        c.addProperty("render_distance_blocks", Fogs.renderDistanceBlocks());
        c.addProperty("smog_tint", SmogFog.tint(SmogFog.renderSoot));
        c.addProperty("smog_active", Fogs.active(mc.getCameraEntity()));
        json.add("client", c);
        return json;
    }
}
