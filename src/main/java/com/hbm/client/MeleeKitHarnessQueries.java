package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/**
 * The {@code hbm_living} harness query: the integrated server's view of every non-player living entity near the
 * local player (health, motion, effects), so a scenario can check a melee weapon's effect by value. Inert unless
 * the harness listener is running.
 */
public final class MeleeKitHarnessQueries {

    private MeleeKitHarnessQueries() {}

    public static void register() {
        dev.golem.core.harness.HarnessQueryRegistry.register("hbm_living", (mc, params) -> query(mc));
        dev.golem.core.harness.HarnessQueryRegistry.register("hbm_props", MeleeKitHarnessQueries::props);
        ImpactHarnessQueries.register(); // fix wave phase 6
        MobHarnessQueries.register(); // fix wave phase 7
        HooksHarnessQueries.register(); // wave 2 HOOKS
        VisualHarnessQueries.register(); // wave 2 VISUAL
    }

    /**
     * The {@code hbm_props} query (EntityDataRoundTrip): the integrated server's HBM living props of the local
     * player and every living entity within 24 blocks. {@code set_rads}/{@code set_reputation} write the player's
     * (and, with {@code mobs=true}, the nearby entities') values first, on the server thread.
     */
    private static JsonObject props(Minecraft mc, JsonObject params) {
        JsonObject json = new JsonObject();
        var server = mc.getSingleplayerServer();
        if (mc.player == null || server == null) return json;
        java.util.UUID id = mc.player.getUUID();
        java.util.concurrent.CompletableFuture<JsonObject> result = new java.util.concurrent.CompletableFuture<>();
        server.execute(() -> {
            try {
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                result.complete(player == null ? new JsonObject() : propsOnServer(player, params));
            } catch (Throwable t) {
                JsonObject error = new JsonObject();
                error.addProperty("error", t.toString());
                result.complete(error);
            }
        });
        try {
            return result.get(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            json.addProperty("error", e.toString());
            return json;
        }
    }

    private static JsonObject propsOnServer(ServerPlayer player, JsonObject params) {
        boolean mobs = params.has("mobs") && params.get("mobs").getAsBoolean();
        java.util.List<LivingEntity> nearby = player.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(player.blockPosition()).inflate(24), e -> e != player);
        java.util.List<LivingEntity> targets = new java.util.ArrayList<>();
        targets.add(player);
        if (mobs) targets.addAll(nearby);
        for (LivingEntity target : targets) {
            var data = com.hbm.capability.HbmLivingProps.getData(target);
            if (params.has("set_rads")) data.setRads(params.get("set_rads").getAsDouble());
            if (params.has("set_reputation")) data.reputation = params.get("set_reputation").getAsInt();
        }
        JsonObject json = new JsonObject();
        json.add("player", propsJson(player));
        JsonArray list = new JsonArray();
        for (LivingEntity living : nearby) list.add(propsJson(living));
        json.add("living", list);
        return json;
    }

    private static JsonObject propsJson(LivingEntity living) {
        JsonObject e = new JsonObject();
        e.addProperty("type", BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()).toString());
        e.addProperty("name", living.getName().getString());
        e.addProperty("stored", com.hbm.capability.HbmLivingProps.DATA.has(living));
        var data = com.hbm.capability.HbmLivingProps.getData(living);
        e.addProperty("rads", data.getRads());
        e.addProperty("digamma", data.getDigamma());
        e.addProperty("reputation", data.reputation);
        e.addProperty("radMark", data.radMark);
        return e;
    }

    private static JsonObject query(Minecraft mc) {
        JsonObject json = new JsonObject();
        if (mc.player == null || mc.getSingleplayerServer() == null) return json;
        ServerPlayer player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
        if (player == null) return json;
        json.addProperty("player_x", player.getX());
        json.addProperty("player_y", player.getY());
        json.addProperty("player_z", player.getZ());

        JsonArray list = new JsonArray();
        for (LivingEntity living : player.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(player.blockPosition()).inflate(24), e -> e != player)) {
            JsonObject e = new JsonObject();
            e.addProperty("type", BuiltInRegistries.ENTITY_TYPE.getKey(living.getType()).toString());
            e.addProperty("health", living.getHealth());
            e.addProperty("max_health", living.getMaxHealth());
            e.addProperty("alive", living.isAlive());
            e.addProperty("x", living.getX());
            e.addProperty("y", living.getY());
            e.addProperty("z", living.getZ());
            e.addProperty("dx", living.getDeltaMovement().x);
            e.addProperty("dy", living.getDeltaMovement().y);
            e.addProperty("dz", living.getDeltaMovement().z);
            JsonArray effects = new JsonArray();
            for (MobEffectInstance effect : living.getActiveEffects()) {
                effects.add(effect.getDescriptionId() + "@" + effect.getAmplifier());
            }
            e.add("effects", effects);
            list.add(e);
        }
        json.add("living", list);
        return json;
    }
}
