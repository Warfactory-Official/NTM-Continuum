package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Harness queries of the wave-2 VISUAL phase (History §19ek-visual). Inert unless the harness listener is running.
 *
 * <ul>
 *   <li>{@code hbm_lang_missing}: every registered {@code hbm} item, block and entity type whose display name has no
 *       translation in the client's loaded language (the name renders as its raw key).</li>
 *   <li>{@code hbm_sound}: resolves a sound event id ({@code id}) through the client sound manager and reports its
 *       sound count and first sound location; {@code play: true} also plays it at the player.</li>
 * </ul>
 */
public final class VisualHarnessQueries {

    private VisualHarnessQueries() {}

    public static void register() {
        dev.golem.core.harness.HarnessQueryRegistry.register("hbm_lang_missing", VisualHarnessQueries::langMissing);
        dev.golem.core.harness.HarnessQueryRegistry.register("hbm_sound", VisualHarnessQueries::sound);
        dev.golem.core.harness.HarnessQueryRegistry.register("hbm_torex", VisualHarnessQueries::torex);
        dev.golem.core.harness.HarnessQueryRegistry.register("hbm_haze", VisualHarnessQueries::haze);
        dev.golem.core.harness.HarnessQueryRegistry.register("hbm_swing", VisualHarnessQueries::swing);
    }

    /** Client-side state of every Torex cloud: local tick, synced age, replay readiness and cloudlet count. */
    private static JsonObject torex(Minecraft mc, JsonObject params) {
        JsonObject json = new JsonObject();
        JsonArray clouds = new JsonArray();
        if (mc.level != null) {
            for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
                if (!(e instanceof com.hbm.entity.effect.EntityNukeTorex torex)) continue;
                JsonObject o = new JsonObject();
                o.addProperty("tick", torex.tickCount);
                o.addProperty("synced_age", torex.syncedAnimationAge());
                o.addProperty("max_age", torex.getMaxAge());
                o.addProperty("ready", torex.isClientAnimationReady());
                o.addProperty("cloudlets", torex.cloudlets.size());
                o.addProperty("distance", Math.sqrt(e.distanceToSqr(mc.player)));
                clouds.add(o);
            }
        }
        json.add("clouds", clouds);
        return json;
    }

    /** Spawns 1.7's white-phosphorus haze at x/y/z, already {@code age} ticks old (default 200, its peak). */
    private static JsonObject haze(Minecraft mc, JsonObject params) {
        JsonObject json = new JsonObject();
        if (mc.level == null) return json;
        int age = params.has("age") ? params.get("age").getAsInt() : 200;
        dev.golem.core.render.Particles.add(new com.hbm.client.particle.ParticleHaze(mc.level,
                params.get("x").getAsDouble(), params.get("y").getAsDouble(), params.get("z").getAsDouble()).aged(age));
        json.addProperty("spawned", true);
        return json;
    }

    /** The local player's arm swing: whether it is swinging and its swing tick (vanilla's own fields). */
    private static JsonObject swing(Minecraft mc, JsonObject params) {
        JsonObject json = new JsonObject();
        if (mc.player == null) return json;
        json.addProperty("swinging", mc.player.swinging);
        json.addProperty("swing_time", mc.player.swingTime);
        return json;
    }

    private static JsonObject langMissing(Minecraft mc, JsonObject params) {
        Language lang = Language.getInstance();
        JsonArray items = new JsonArray(), blocks = new JsonArray(), entities = new JsonArray();
        int itemCount = 0, blockCount = 0, entityCount = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier key = BuiltInRegistries.ITEM.getKey(item);
            if (!"hbm".equals(key.getNamespace())) continue;
            itemCount++;
            String key0 = item.getDescriptionId();
            String shown;
            try {
                shown = new ItemStack(item).getHoverName().getString();
            } catch (Throwable t) {
                shown = key0;
            }
            if (!lang.has(key0) && (shown.equals(key0) || shown.startsWith("item.") || shown.startsWith("block.")
                    || shown.endsWith(".name"))) {
                items.add(key.getPath() + " = " + shown);
            }
        }
        for (net.minecraft.world.level.block.Block block : BuiltInRegistries.BLOCK) {
            Identifier key = BuiltInRegistries.BLOCK.getKey(block);
            if (!"hbm".equals(key.getNamespace())) continue;
            blockCount++;
            if (!lang.has(block.getDescriptionId())) blocks.add(key.getPath() + " = " + block.getDescriptionId());
        }
        for (net.minecraft.world.entity.EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            Identifier key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (!"hbm".equals(key.getNamespace())) continue;
            entityCount++;
            if (!lang.has(type.getDescriptionId())) entities.add(key.getPath() + " = " + type.getDescriptionId());
        }
        JsonObject json = new JsonObject();
        json.addProperty("item_count", itemCount);
        json.addProperty("block_count", blockCount);
        json.addProperty("entity_count", entityCount);
        json.add("items", items);
        json.add("blocks", blocks);
        json.add("entities", entities);
        return json;
    }

    private static JsonObject sound(Minecraft mc, JsonObject params) {
        JsonObject json = new JsonObject();
        String id = params.has("id") ? params.get("id").getAsString() : "hbm:rbmk_shutdown";
        Identifier location = dev.golem.core.Ids.parse(id);
        var events = mc.getSoundManager().getSoundEvent(location);
        json.addProperty("id", id);
        json.addProperty("resolved", events != null);
        if (events != null) {
            json.addProperty("weight", events.getWeight());
            var sound = events.getSound(net.minecraft.util.RandomSource.create(0L));
            json.addProperty("sound", sound == null ? "null" : String.valueOf(sound.getLocation()));
            json.addProperty("stream", sound != null && sound.shouldStream());
        }
        if (params.has("play") && params.get("play").getAsBoolean() && mc.player != null) {
            mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                    net.minecraft.sounds.SoundEvent.createVariableRangeEvent(location), 1.0F, 1.0F));
            json.addProperty("played", true);
        }
        return json;
    }
}
