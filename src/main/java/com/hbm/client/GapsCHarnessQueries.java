package com.hbm.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.client.anim.HbmAnimations;
import com.hbm.client.particle.ParticleSmokePlume;
import com.hbm.client.particle.SparkParticles;
import com.hbm.client.particle.TextParticles;
import com.hbm.client.render.RenderOverhead;
import com.hbm.client.render.entity.EntityItemRenderers;
import com.hbm.extprop.HbmPlayerProps;
import com.hbm.handler.ArmorModHandler;
import com.hbm.items.ModArmorItems;
import com.hbm.items.ModArmorModItems;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.SatelliteScanner;
import dev.golem.core.client.EquipProgress;
import dev.golem.core.client.VanillaHud;
import dev.golem.core.render.ConnectedTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * The {@code hbm_gapsc} harness query of gap batch C (§19ed-gaps-c), read by {@code FinalGapsC}. Client: the held
 * slot's animation buses ({@code SWING_ROT}, {@code SWING_TRANS}, {@code GUARD_ROT}, {@code RECOIL}), the vanilla swing
 * flag, the hand's equip progress, live sparks/texts/markers, launch-smoke count, connected-texture registrations,
 * framed-item routing, the client's synced shield/magnet, and the HUD suppression flags. Server (params):
 * {@code {"shield":[cur,max]}} sets the server props; {@code {"lens":freq}} puts a depth scanner on {@code freq} and
 * equips a T-51 helmet carrying a neutrino lens tuned to it; {@code {"framed":[ids]}} asks the routing for each id.
 * Inert unless the harness runs.
 */
public final class GapsCHarnessQueries {

    private GapsCHarnessQueries() {}

    public static void register() {
        dev.golem.core.harness.HarnessQueryRegistry.register("hbm_gapsc", GapsCHarnessQueries::query);
    }

    private static JsonObject query(Minecraft mc, JsonObject params) {
        JsonObject json = new JsonObject();
        MinecraftServer server = mc.getSingleplayerServer();
        if (mc.player == null || server == null) return json;

        json.add("swing_rot", array(HbmAnimations.getRelevantTransformation("SWING_ROT")));
        json.add("swing_trans", array(HbmAnimations.getRelevantTransformation("SWING_TRANS")));
        json.add("guard_rot", array(HbmAnimations.getRelevantTransformation("GUARD_ROT")));
        json.add("recoil", array(HbmAnimations.getRelevantTransformation("RECOIL")));
        json.addProperty("anim_running", HbmAnimations.getRelevantAnim(0) != null);
        json.addProperty("swinging", mc.player.swinging);
        json.addProperty("equip", EquipProgress.mainHand());
        json.addProperty("equip_prev", EquipProgress.previousMainHand());
        json.addProperty("sparks", SparkParticles.active());
        json.addProperty("sparks_spawned", SparkParticles.spawned);
        JsonArray texts = new JsonArray();
        for (String text : TextParticles.active()) texts.add(text);
        json.add("texts", texts);
        json.addProperty("markers", RenderOverhead.count());
        JsonArray labels = new JsonArray();
        for (String label : RenderOverhead.labels()) labels.add(label);
        json.add("marker_labels", labels);
        json.addProperty("launch_smoke", ParticleSmokePlume.spawned);
        json.addProperty("launch_smoke_ticks", ParticleSmokePlume.ticked);
        json.addProperty("launch_smoke_drift", ParticleSmokePlume.maxDrift);
        json.addProperty("ct_registered", ConnectedTextures.registered());
        json.addProperty("ct_wrapped", ConnectedTextures.wrapped());
        json.addProperty("framed_routes", EntityItemRenderers.count());
        if (params.has("framed")) {
            JsonObject framed = new JsonObject();
            for (var id : params.getAsJsonArray("framed")) {
                var item = BuiltInRegistries.ITEM.getOptional(dev.golem.core.Ids.parse(id.getAsString()));
                framed.addProperty(id.getAsString(), item.isPresent() && EntityItemRenderers.handles(new ItemStack(item.get())));
            }
            json.add("framed", framed);
        }
        var props = HbmPlayerProps.getData(mc.player);
        json.addProperty("client_shield", props.shield);
        json.addProperty("client_max_shield", props.maxShield);
        json.addProperty("client_magnet", props.enableMagnet);
        json.addProperty("hide_health", VanillaHud.hideHealth());
        json.addProperty("hide_armor", VanillaHud.hideArmor());
        json.addProperty("hud_suppressible", VanillaHud.supported());
        json.addProperty("rpa_renderer", com.hbm.client.render.armor.ArmorRenderers.get("rpa") != null);
        json.addProperty("ncrpa_renderer", com.hbm.client.render.armor.ArmorRenderers.get("ncrpa") != null);

        CompletableFuture<JsonObject> result = new CompletableFuture<>();
        server.execute(() -> {
            try {
                ServerPlayer player = server.getPlayerList().getPlayer(mc.player.getUUID());
                result.complete(player == null ? new JsonObject() : onServer(player, params));
            } catch (Throwable t) {
                JsonObject error = new JsonObject();
                error.addProperty("error", t.toString());
                result.complete(error);
            }
        });
        try {
            JsonObject fromServer = result.get(5, TimeUnit.SECONDS);
            for (var e : fromServer.entrySet()) json.add(e.getKey(), e.getValue());
        } catch (Exception e) {
            json.addProperty("error", e.toString());
        }
        return json;
    }

    private static JsonObject onServer(ServerPlayer player, JsonObject params) {
        JsonObject json = new JsonObject();
        var props = HbmPlayerProps.getData(player);
        if (params.has("shield")) {
            props.shield = params.getAsJsonArray("shield").get(0).getAsFloat();
            props.maxShield = params.getAsJsonArray("shield").get(1).getAsFloat();
        }
        if (params.has("lens")) {
            int freq = params.get("lens").getAsInt();
            SatelliteSavedData.get((net.minecraft.server.level.ServerLevel) player.level()).put(freq, new SatelliteScanner());
            ItemStack lens = new ItemStack(ModArmorModItems.neutrino_lens.get());
            ModArmorModItems.neutrino_lens.get().setFreq(lens, freq);
            ItemStack helmet = new ItemStack(ModArmorItems.t51_helmet.get());
            ArmorModHandler.applyMod(helmet, lens);
            player.setItemSlot(EquipmentSlot.HEAD, helmet);
            json.addProperty("lens_applied", ArmorModHandler.pryMods(helmet)[ArmorModHandler.extra] != null);
        }
        json.addProperty("server_shield", props.shield);
        json.addProperty("server_max_shield", props.maxShield);
        json.addProperty("server_magnet", props.enableMagnet);
        json.addProperty("held", player.getMainHandItem().isEmpty() ? ""
                : BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString());
        return json;
    }

    private static JsonArray array(double[] values) {
        JsonArray out = new JsonArray();
        for (int i = 0; i < 6; i++) out.add(values[i]);
        return out;
    }
}
