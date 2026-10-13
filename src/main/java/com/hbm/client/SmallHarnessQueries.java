package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.handler.radiation.RadiationFog;
import com.hbm.main.ModEventHandlerImpact;
import com.hbm.world.feature.OreCaveFeature;
import com.hbm.world.gen.LegacyBiomes;
import dev.golem.core.Ids;
import dev.golem.core.harness.HarnessQueryRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.phys.AABB;

/**
 * The {@code hbm_small} harness query. Inert unless the harness listener runs.
 * <ul>
 *   <li>{@code boss}: the client's 1.7 {@link BossStatus} bar (showing, name).</li>
 *   <li>{@code fog}: radiation fog effects sent so far; {@code set_rad} sets the chunk radiation at the player first.</li>
 *   <li>{@code spawns}: spawns the impact hook refused so far, and the monsters within {@code radius} by height.</li>
 *   <li>{@code sulfur}: columns of this world's sulfur stratum near {@code x, z} (noise only, nothing generated).</li>
 * </ul>
 */
public final class SmallHarnessQueries {

    private SmallHarnessQueries() {}

    public static void register() {
        HarnessQueryRegistry.register("hbm_small", SmallHarnessQueries::query);
    }

    private static JsonObject query(Minecraft mc, JsonObject params) {
        String mode = params.has("mode") ? params.get("mode").getAsString() : "boss";
        if (mode.equals("boss")) {
            JsonObject json = new JsonObject();
            json.addProperty("showing", BossStatus.showing());
            json.addProperty("name", BossStatus.shownName() == null ? "" : BossStatus.shownName().getString());
            return json;
        }
        return ServerQueries.answer(mc, "hbm_small", params.deepCopy(), (level, p) -> {
            JsonObject json = new JsonObject();
            try {
                var player = level.getServer().getPlayerList().getPlayers().isEmpty() ? null
                        : level.getServer().getPlayerList().getPlayers().get(0);
                switch (mode) {
                    case "fog" -> {
                        if (player != null && p.has("set_rad")) {
                            BlockPos at = p.has("y") ? new BlockPos(p.get("x").getAsInt(), p.get("y").getAsInt(), p.get("z").getAsInt())
                                    : player.blockPosition();
                            ChunkRadiationManager.proxy.setRadiation(level, at, p.get("set_rad").getAsDouble());
                        }
                        json.addProperty("sent", RadiationFog.sent);
                        if (player != null) json.addProperty("chunk_rad",
                                ChunkRadiationManager.proxy.getRadiation(level, player.blockPosition()));
                    }
                    case "spawns" -> {
                        json.addProperty("denied", ModEventHandlerImpact.denied);
                        int radius = p.has("radius") ? p.get("radius").getAsInt() : 96;
                        int tall = 0, small = 0;
                        JsonObject types = new JsonObject();
                        if (player != null) {
                            for (Entity e : level.getEntities((Entity) null, AABB.ofSize(player.position(), radius * 2, radius * 2, radius * 2),
                                    e -> e instanceof LivingEntity && e.getType().getCategory() == MobCategory.MONSTER)) {
                                if (e.getBbHeight() >= 0.85F || e.getBbWidth() >= 0.85F) tall++;
                                else small++;
                                String id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();
                                types.addProperty(id, (types.has(id) ? types.get(id).getAsInt() : 0) + 1);
                            }
                        }
                        json.addProperty("tall_monsters", tall);
                        json.addProperty("small_monsters", small);
                        json.add("types", types);
                    }
                    case "sulfur" -> {
                        JsonArray cols = new JsonArray();
                        for (int[] c : OreCaveFeature.sulfurColumns(level.getSeed(),
                                p.get("x").getAsInt(), p.get("z").getAsInt(), p.get("radius").getAsInt(),
                                p.has("step") ? p.get("step").getAsInt() : 8, p.has("limit") ? p.get("limit").getAsInt() : 64)) {
                            JsonArray a = new JsonArray();
                            a.add(c[0]);
                            a.add(c[1]);
                            a.add(c[2]);
                            // the biome at the stratum's centre (noise only): oceans fill their caves with water
                            var generator = level.getChunkSource().getGenerator();
                            a.add(LegacyBiomes.biomeAt(generator.getBiomeSource(), level.getChunkSource().randomState(),
                                    c[0], 64, c[1]).unwrapKey().map(k -> Ids.keyId(k).toString()).orElse("?"));
                            cols.add(a);
                        }
                        json.add("columns", cols);
                    }
                    default -> json.addProperty("error", "unknown mode " + mode);
                }
            } catch (Throwable t) {
                json.addProperty("error", t.toString());
            }
            return json;
        });
    }
}
