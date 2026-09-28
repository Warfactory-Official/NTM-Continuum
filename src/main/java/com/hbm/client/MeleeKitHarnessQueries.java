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
